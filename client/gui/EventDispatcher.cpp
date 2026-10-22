/*
 * EventDispatcher.cpp, part of VCMI engine
 *
 * Authors: listed in file AUTHORS in main folder
 *
 * License: GNU General Public License v2.0 or later
 * Full text of license available in license.txt file, in main folder
 *
 */
#include "StdInc.h"
#include "EventDispatcher.h"

#include "EventsReceiver.h"
#include "CIntObject.h"
#include "FramerateManager.h"
#include "GameEngine.h"
#include "MouseButton.h"
#include "WindowHandler.h"
#include "gui/Shortcut.h"
#include "ShortcutHandler.h"

#include "../../lib/CConfigHandler.h"
#include "../../lib/Rect.h"
#include "events/InputHandler.h"

template<typename Functor>
void EventDispatcher::processLists(ui16 activityFlag, const Functor & cb)
{
	auto processList = [&](ui16 mask, EventReceiversList & lst)
	{
		if(mask & activityFlag)
			cb(lst);
	};

	processList(AEventsReceiver::LCLICK, lclickable);
	processList(AEventsReceiver::SHOW_POPUP, rclickable);
	processList(AEventsReceiver::HOVER, hoverable);
	processList(AEventsReceiver::MOVE, motioninterested);
	processList(AEventsReceiver::DRAG, draginterested);
	processList(AEventsReceiver::DRAG_POPUP, dragPopupInterested);
	processList(AEventsReceiver::KEYBOARD, keyinterested);
	processList(AEventsReceiver::TIME, timeinterested);
	processList(AEventsReceiver::WHEEL, wheelInterested);
	processList(AEventsReceiver::DOUBLECLICK, doubleClickInterested);
	processList(AEventsReceiver::TEXTINPUT, textInterested);
	processList(AEventsReceiver::GESTURE, panningInterested);
	processList(AEventsReceiver::INPUT_MODE_CHANGE, inputModeChangeInterested);
	processList(AEventsReceiver::KEY_NAME, keyNameInterested);
	processList(AEventsReceiver::CONTROLLER_AXIS, controllerAxisInterested);
}

void EventDispatcher::activateElement(AEventsReceiver * elem, ui16 activityFlag)
{
	processLists(activityFlag,[&](EventReceiversList & lst){
		lst.push_front(elem);
	});
	elem->activeState |= activityFlag;
}

void EventDispatcher::deactivateElement(AEventsReceiver * elem, ui16 activityFlag)
{
	// element that is deactivated while being touched will never receive finger-up event
	if((activityFlag & AEventsReceiver::LCLICK) && vstd::erase_if_present(touchPressedElements, elem))
		elem->onTouchPress(false);

	processLists(activityFlag,[&](EventReceiversList & lst){
		auto hlp = std::find(lst.begin(),lst.end(),elem);
		assert(hlp != lst.end());
		lst.erase(hlp);
	});
	elem->activeState &= ~activityFlag;
}

void EventDispatcher::dispatchTimer(uint32_t msPassed)
{
	EventReceiversList hlp = timeinterested;
	for (auto & elem : hlp)
	{
		if(!vstd::contains(timeinterested,elem))
			continue;

		elem->tick(msPassed);
	}
}

void EventDispatcher::blockControllerButton(int instance, const std::string & control)
{
	controllerPresses.suppress({instance, control});
}

void EventDispatcher::dispatchControllerButtonPressed(int instance, const std::string & control)
{
	const auto identity = std::make_pair(instance, control);
	const auto shortcuts = translateControllerShortcuts(ENGINE->shortcuts().translateJoystickButton(control));
	const auto window = ENGINE->windows().topWindow<IShowActivatable>();
	if(!controllerPresses.press(identity, shortcuts, window))
		return;
	dispatchKeyPressed(control);
	if(!controllerPresses.active(identity, window))
		return;
	dispatchShortcutPressed(shortcuts);

	// A held right-click owns only the informational popup opened by this press.
	const auto current = ENGINE->windows().topWindow<IShowActivatable>();
	if(vstd::contains(shortcuts, EShortcut::MOUSE_RIGHT) && current != window && ENGINE->windows().isTopWindowPopup())
		controllerPresses.transferPopup(identity, current);
}

void EventDispatcher::dispatchControllerButtonReleased(int instance, const std::string & control)
{
	const auto window = ENGINE->windows().topWindow<IShowActivatable>();
	const auto press = controllerPresses.release({instance, control}, window);
	if(!press)
		return;
	dispatchKeyReleased(control);
	if(window == ENGINE->windows().topWindow<IShowActivatable>())
		dispatchShortcutReleased(press->shortcuts);
}

void EventDispatcher::cancelControllerInput(bool dismissPopup)
{
	++controllerEpoch;
	std::shared_ptr<IShowActivatable> popup;
	controllerPresses.cancel(ENGINE->windows().topWindow<IShowActivatable>(), [&](const ControllerPressState::Press & press)
	{
		if(press.popup)
			popup = press.window.lock();
		if(vstd::contains(press.shortcuts, EShortcut::MOUSE_LEFT))
		{
			const auto receivers = lclickable;
			for(auto receiver : receivers)
				if(vstd::contains(lclickable, receiver) && receiver->mouseClickedState)
				{
					receiver->mouseClickedState = false;
					receiver->clickCancel(ENGINE->getCursorPosition());
				}
		}
		const auto receivers = keyinterested;
		for(auto receiver : receivers)
			for(auto shortcut : press.shortcuts)
				if(vstd::contains(keyinterested, receiver))
					receiver->keyCanceled(shortcut);
	});
	const auto receivers = controllerAxisInterested;
	for(auto receiver : receivers)
		if(vstd::contains(controllerAxisInterested, receiver))
			receiver->controllerInputCanceled();
	if(dismissPopup && popup && ENGINE->windows().isTopWindow(popup))
		ENGINE->windows().popWindow(popup);
}

void EventDispatcher::forgetController(int instance)
{
	controllerPresses.forget(instance);
}

void EventDispatcher::dispatchGesturePanningCanceled(bool closeWindow)
{
	const auto receivers = panningInterested;
	for(auto receiver : receivers)
		if(vstd::contains(panningInterested, receiver) && receiver->isGesturing())
		{
			receiver->panningState = false;
			receiver->gestureCanceled(closeWindow);
		}
}

bool EventDispatcher::dispatchControllerAxis(const std::vector<EShortcut> & axes, double value)
{
	bool consumed = false;
	for(auto receiver : controllerAxisInterested)
		for(auto axis : axes)
			consumed = receiver->controllerAxisMoved(axis, value) || consumed;
	return consumed;
}

std::vector<EShortcut> EventDispatcher::translateControllerShortcuts(std::vector<EShortcut> shortcuts)
{
	for(auto receiver : keyinterested)
		if(receiver->translateControllerShortcuts(shortcuts))
			break;
	return shortcuts;
}

void EventDispatcher::dispatchShortcutPressed(const std::vector<EShortcut> & shortcutsVector)
{
	const auto window = ENGINE->windows().topWindow<IShowActivatable>();
	const auto dispatchEpoch = controllerEpoch;
	bool keysCaptured = false;

	if (vstd::contains(shortcutsVector, EShortcut::MOUSE_LEFT))
		dispatchMouseLeftButtonPressed(ENGINE->getCursorPosition(), settings["input"]["shortcutToleranceDistance"].Integer());

	if(dispatchEpoch != controllerEpoch || window != ENGINE->windows().topWindow<IShowActivatable>())
		return;

	if (vstd::contains(shortcutsVector, EShortcut::MOUSE_RIGHT))
		dispatchShowPopup(ENGINE->getCursorPosition(), settings["input"]["shortcutToleranceDistance"].Integer());

	if(dispatchEpoch != controllerEpoch || window != ENGINE->windows().topWindow<IShowActivatable>())
		return;

	for(auto & i : keyinterested)
		for(EShortcut shortcut : shortcutsVector)
			if(i->captureThisKey(shortcut))
				keysCaptured = true;

	EventReceiversList miCopy = keyinterested;

	for(auto & i : miCopy)
	{
		for(EShortcut shortcut : shortcutsVector)
			if(vstd::contains(keyinterested, i) && (!keysCaptured || i->captureThisKey(shortcut)))
			{
				i->keyPressed(shortcut);
				if(dispatchEpoch != controllerEpoch || window != ENGINE->windows().topWindow<IShowActivatable>())
					return;
				if (keysCaptured)
					return;
			}
	}
}

void EventDispatcher::dispatchShortcutReleased(const std::vector<EShortcut> & shortcutsVector)
{
	const auto window = ENGINE->windows().topWindow<IShowActivatable>();
	const auto dispatchEpoch = controllerEpoch;
	bool keysCaptured = false;

	if (vstd::contains(shortcutsVector, EShortcut::MOUSE_LEFT))
		dispatchMouseLeftButtonReleased(ENGINE->getCursorPosition(), settings["input"]["shortcutToleranceDistance"].Integer());

	if(dispatchEpoch != controllerEpoch || window != ENGINE->windows().topWindow<IShowActivatable>())
		return;

	if (vstd::contains(shortcutsVector, EShortcut::MOUSE_RIGHT))
		dispatchClosePopup(ENGINE->getCursorPosition());

	if(dispatchEpoch != controllerEpoch || window != ENGINE->windows().topWindow<IShowActivatable>())
		return;

	for(auto & i : keyinterested)
		for(EShortcut shortcut : shortcutsVector)
			if(i->captureThisKey(shortcut))
				keysCaptured = true;

	EventReceiversList miCopy = keyinterested;

	for(auto & i : miCopy)
	{
		for(EShortcut shortcut : shortcutsVector)
			if(vstd::contains(keyinterested, i) && (!keysCaptured || i->captureThisKey(shortcut)))
			{
				i->keyReleased(shortcut);
				if(dispatchEpoch != controllerEpoch || window != ENGINE->windows().topWindow<IShowActivatable>())
					return;
				if (keysCaptured)
					return;
			}
	}
}

void EventDispatcher::dispatchKeyPressed(const std::string & keyName)
{
	EventReceiversList miCopy = keyNameInterested;

	for(auto & i : miCopy)
		i->keyPressed(keyName);
}

void EventDispatcher::dispatchKeyReleased(const std::string & keyName)
{
	EventReceiversList miCopy = keyNameInterested;

	for(auto & i : miCopy)
		i->keyReleased(keyName);
}

void EventDispatcher::dispatchMouseDoubleClick(const Point & position, int tolerance)
{
	handleDoubleButtonClick(position, tolerance);
}

void EventDispatcher::dispatchMouseLeftButtonPressed(const Point & position, int tolerance)
{
	handleLeftButtonClick(position, tolerance, true);
}

void EventDispatcher::dispatchMouseLeftButtonReleased(const Point & position, int tolerance)
{
	handleLeftButtonClick(position, tolerance, false);
}

AEventsReceiver * EventDispatcher::findElementInToleranceRange(const EventReceiversList & list, const Point & position, int eventToTest, int tolerance)
{
	AEventsReceiver * bestElement = nullptr;
	int bestDistance = std::numeric_limits<int>::max();

	for(auto & i : list)
	{
		// if there is element that can actually receive event then tolerance clicking is disabled
		if( i->receiveEvent(position, eventToTest))
			return nullptr;

		if (i->getPosition().distanceTo(position) > bestDistance)
			continue;

		Point center = i->getPosition().center();
		Point distance = center - position;

		if (distance.lengthSquared() == 0)
			continue;

		Point moveDelta = distance * std::min(1.0, static_cast<double>(tolerance) / distance.length());
		Point testPosition = position + moveDelta;

		if( !i->receiveEvent(testPosition, eventToTest))
			continue;

		bestElement = i;
		bestDistance = i->getPosition().distanceTo(position);
	}

	return bestElement;
}

void EventDispatcher::dispatchShowPopup(const Point & position, int tolerance)
{
	AEventsReceiver * nearestElement = findElementInToleranceRange(rclickable, position, AEventsReceiver::SHOW_POPUP, tolerance);

	auto hlp = rclickable;

	for(auto & i : hlp)
	{
		if(!vstd::contains(rclickable, i))
			continue;

		if( !i->receiveEvent(position, AEventsReceiver::SHOW_POPUP) && i != nearestElement)
			continue;

		i->showPopupWindow(position);
	}
}

void EventDispatcher::dispatchClosePopup(const Point & position)
{
	bool popupOpen = ENGINE->windows().isTopWindowPopup(); // popup can already be closed for mouse dragging with RMB

	if(popupOpen)
	{
		ENGINE->windows().popWindows(1);
	}

	auto hlp = rclickable;

	for(auto & i : hlp)
	{
		if(!vstd::contains(rclickable, i))
			continue;

		i->closePopupWindow(!popupOpen);
	}
}

void EventDispatcher::handleLeftButtonClick(const Point & position, int tolerance, bool isPressed)
{
	// WARNING: this approach is NOT SAFE
	// 1) We allow (un)registering elements when list itself is being processed/iterated
	// 2) To avoid iterator invalidation we create a copy of this list for processing
	// HOWEVER it is completely possible (as in, actually happen, no just theory) to:
	// 1) element gets unregistered and deleted from lclickable
	// 2) element is completely deleted, as in - destructor called, memory freed
	// 3) new element is created *with exactly same address(!)
	// 4) new element is registered and code will incorrectly assume that this element is still registered
	// POSSIBLE SOLUTION: make EventReceivers inherit from create_shared_from this and store weak_ptr's in lists
	AEventsReceiver * nearestElement = findElementInToleranceRange(lclickable, position, AEventsReceiver::LCLICK, tolerance);
	auto hlp = lclickable;
	bool lastActivated = true;

	for(auto & i : hlp)
	{
		if(!vstd::contains(lclickable, i))
			continue;

		if( i->receiveEvent(position, AEventsReceiver::LCLICK) || i == nearestElement)
		{
			if(isPressed)
			{
				i->mouseClickedState = isPressed;
				i->clickPressed(position, lastActivated);
			}
			else
			{
				if (i->mouseClickedState)
				{
					i->mouseClickedState = isPressed;
					i->clickReleased(position, lastActivated);
				}
				else
					i->mouseClickedState = isPressed;
			}

			lastActivated = false;
		}
		else
		{
			if(i->mouseClickedState && !isPressed)
			{
				i->mouseClickedState = isPressed;
				i->clickCancel(position);
			}
			else if(isPressed)
			{
				i->notFocusedClick();
			}
		}
	}
}

void EventDispatcher::handleDoubleButtonClick(const Point & position, int tolerance)
{
	// WARNING: this approach is NOT SAFE
	// 1) We allow (un)registering elements when list itself is being processed/iterated
	// 2) To avoid iterator invalidation we create a copy of this list for processing
	// HOWEVER it is completely possible (as in, actually happen, no just theory) to:
	// 1) element gets unregistered and deleted from lclickable
	// 2) element is completely deleted, as in - destructor called, memory freed
	// 3) new element is created *with exactly same address(!)
	// 4) new element is registered and code will incorrectly assume that this element is still registered
	// POSSIBLE SOLUTION: make EventReceivers inherit from create_shared_from this and store weak_ptr's in lists

	AEventsReceiver * nearestElement = findElementInToleranceRange(doubleClickInterested, position, AEventsReceiver::DOUBLECLICK, tolerance);
	bool doubleClicked = false;
	auto hlp = doubleClickInterested;

	for(auto & i : hlp)
	{
		if(!vstd::contains(doubleClickInterested, i))
			continue;

		if(i->receiveEvent(position, AEventsReceiver::DOUBLECLICK) || i == nearestElement)
		{
			i->clickDouble(position);
			doubleClicked = true;
		}
	}

	if(!doubleClicked)
		handleLeftButtonClick(position, tolerance, true);
}

void EventDispatcher::dispatchMouseScrolled(const Point & distance, const Point & position)
{
	EventReceiversList hlp = wheelInterested;
	for(auto & i : hlp)
	{
		if(!vstd::contains(wheelInterested,i))
			continue;

		if (i->receiveEvent(position, AEventsReceiver::WHEEL) && distance.y != 0)
			i->wheelScrolled(distance.y);
	}
}

void EventDispatcher::dispatchTextInput(const std::string & text)
{
	for(auto it : textInterested)
	{
		it->textInputted(text);
	}
}

void EventDispatcher::dispatchTextEditing(const std::string & text)
{
	for(auto it : textInterested)
	{
		it->textEdited(text);
	}
}

void EventDispatcher::dispatchInputModeChanged(const InputMode & modi)
{
	for(auto it : inputModeChangeInterested)
	{
		it->inputModeChanged(modi);
	}
}

void EventDispatcher::dispatchGesturePanningStarted(const Point & initialPosition)
{
	auto copied = panningInterested;

	for(auto it : copied)
	{
		if (!vstd::contains(panningInterested, it))
			continue;

		if (!it->isGesturing() && it->receiveEvent(initialPosition, AEventsReceiver::GESTURE))
		{
			it->panningState = true;
			it->gesture(true, initialPosition, initialPosition);
		}
	}
}

void EventDispatcher::dispatchGesturePanningEnded(const Point & initialPosition, const Point & finalPosition)
{
	dispatchGesturePanningStarted(initialPosition);
	auto copied = panningInterested;

	for(auto it : copied)
	{
		if (it->isGesturing())
		{
			it->panningState = false;
			it->gesture(false, initialPosition, finalPosition);
		}
	}
}

void EventDispatcher::dispatchGesturePanning(const Point & initialPosition, const Point & currentPosition, const Point & lastUpdateDistance)
{
	dispatchGesturePanningStarted(initialPosition);
	auto copied = panningInterested;

	for(auto it : copied)
	{
		if (!vstd::contains(panningInterested, it))
			continue;

		if (it->isGesturing())
			it->gesturePanning(initialPosition, currentPosition, lastUpdateDistance);
	}
}

void EventDispatcher::dispatchGesturePinch(const Point & initialPosition, double distance)
{
	for(auto it : panningInterested)
	{
		if (it->isGesturing())
			it->gesturePinch(initialPosition, distance);
	}
}

void EventDispatcher::dispatchMouseMoved(const Point & distance, const Point & position)
{
	EventReceiversList newlyHovered;

	auto hoverableCopy = hoverable;
	for(auto & elem : hoverableCopy)
	{
		if(elem->receiveEvent(position, AEventsReceiver::HOVER))
		{
			if (!elem->isHovered())
			{
				newlyHovered.push_back((elem));
			}
		}
		else
		{
			if (elem->isHovered())
			{
				elem->hoveredState = false;
				elem->hover(false);
			}
		}
	}

	for(auto & elem : newlyHovered)
	{
		elem->hoveredState = true;
		elem->hover(true);
	}

	//sending active, MotionInterested objects mouseMoved() call
	EventReceiversList miCopy = motioninterested;
	for(auto & elem : miCopy)
	{
		if (!vstd::contains(motioninterested, elem))
			continue;

		if(elem->receiveEvent(position, AEventsReceiver::HOVER))
			elem->mouseMoved(position, distance);
	}
}

void EventDispatcher::dispatchTouchPress(const Point & position, bool down, int tolerance)
{
	if (down)
	{
		touchPressedElements.clear();
		AEventsReceiver * nearestElement = findElementInToleranceRange(lclickable, position, AEventsReceiver::LCLICK, tolerance);
		auto hlp = lclickable;
		for(auto & elem : hlp)
		{
			if(!vstd::contains(lclickable, elem))
				continue;

			if(elem->receiveEvent(position, AEventsReceiver::LCLICK) || elem == nearestElement)
			{
				elem->onTouchPress(true);
				touchPressedElements.push_back(elem);
			}
		}
	}
	else
	{
		// reset all because we don't neccessary get the same element (finger can moved after touching, before releasing)
		auto hlp = std::move(touchPressedElements);
		touchPressedElements.clear();

		for(auto & elem : hlp)
			elem->onTouchPress(false);
	}
}

void EventDispatcher::dispatchMouseDragged(const Point & currentPosition, const Point & lastUpdateDistance)
{
	EventReceiversList diCopy = draginterested;
	for(auto & elem : diCopy)
	{
		if (elem->mouseClickedState)
			elem->mouseDragged(currentPosition, lastUpdateDistance);
	}
}

void EventDispatcher::dispatchMouseDraggedPopup(const Point & currentPosition, const Point & lastUpdateDistance)
{
	EventReceiversList diCopy = dragPopupInterested;
	for(auto & elem : diCopy)
		elem->mouseDraggedPopup(currentPosition, lastUpdateDistance);
}
