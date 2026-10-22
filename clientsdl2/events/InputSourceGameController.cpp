/*
* InputSourceGameController.cpp, part of VCMI engine
*
* Authors: listed in file AUTHORS in main folder
*
* License: GNU General Public License v2.0 or later
* Full text of license available in license.txt file, in main folder
*
*/

#include "StdInc.h"
#include "InputSourceGameController.h"

#include "InputHandler.h"

#include "GameEngine.h"
#include "gui/CursorHandler.h"
#include "gui/EventDispatcher.h"
#include "gui/ShortcutHandler.h"
#include "../render/IScreenHandler.h"

#include "lib/CConfigHandler.h"

#include <SDL_version.h>

namespace
{

ControllerPrompt::Family controllerPromptFamily(SDL_GameController * controller)
{
#if SDL_VERSION_ATLEAST(2,0,12)
	switch(SDL_GameControllerGetType(controller))
	{
	case SDL_CONTROLLER_TYPE_PS3:
	case SDL_CONTROLLER_TYPE_PS4:
#if SDL_VERSION_ATLEAST(2,0,14)
	case SDL_CONTROLLER_TYPE_PS5:
#endif
		return ControllerPrompt::Family::PLAYSTATION;
	case SDL_CONTROLLER_TYPE_XBOX360:
	case SDL_CONTROLLER_TYPE_XBOXONE:
		return ControllerPrompt::Family::XBOX;
	default:
		return ControllerPrompt::Family::UNKNOWN;
	}
#else
	static_cast<void>(controller);
	return ControllerPrompt::Family::UNKNOWN;
#endif
}

}

void InputSourceGameController::gameControllerDeleter(SDL_GameController * gameController)
{
	if(gameController)
		SDL_GameControllerClose(gameController);
}

InputSourceGameController::InputSourceGameController():
	cursorAxisValueX(0),
	cursorAxisValueY(0),
	cursorPlanDisX(0.0),
	cursorPlanDisY(0.0),
	scrollAxisMoved(false),
	scrollStart(Point(0,0)),
	scrollCurrent(Point(0,0)),
	scrollAxisValueX(0),
	scrollAxisValueY(0),
	scrollPlanDisX(0.0),
	scrollPlanDisY(0.0),
	configTriggerThreshold(settings["input"]["controllerTriggerThreshold"].Float()),
	configAxisDeadZone(settings["input"]["controllerAxisDeadZone"].Float()),
	configAxisFullZone(settings["input"]["controllerAxisFullZone"].Float()),
	configAxisSpeed(settings["input"]["controllerAxisSpeed"].Float()),
	configAxisScale(settings["input"]["controllerAxisScale"].Float())
{
	tryOpenAllGameControllers();
}

void InputSourceGameController::tryOpenAllGameControllers()
{
	for(int i = 0; i < SDL_NumJoysticks(); ++i)
		if(SDL_IsGameController(i))
			openGameController(i);
		else
			logGlobal->warn("Joystick %d is an unsupported game controller!", i);
}

void InputSourceGameController::openGameController(int index)
{
	SDL_GameController * controller = SDL_GameControllerOpen(index);
	if(!controller)
	{
		logGlobal->error("Fail to open game controller %d!", index);
		return;
	}
	GameControllerPtr controllerPtr(controller, &gameControllerDeleter);

	// Need to save joystick index for event. Joystick index may not be equal to index sometimes.
	int joystickIndex = getJoystickIndex(controllerPtr.get());
	if(joystickIndex < 0)
	{
		logGlobal->error("Fail to get joystick index of game controller %d!", index);
		return;
	}

	if(gameControllerMap.find(joystickIndex) != gameControllerMap.end())
	{
		logGlobal->warn("Game controller with joystick index %d is already opened.", joystickIndex);
		return;
	}

	gameControllerMap.try_emplace(joystickIndex, std::move(controllerPtr));
	latchHeldControls(joystickIndex);
}

void InputSourceGameController::latchHeldControls(int instance)
{
	const auto controller = gameControllerMap.at(instance).get();
	// Connecting/remapping a held control is not a new intent in the active window.
	for(int button = 0; button < SDL_CONTROLLER_BUTTON_MAX; ++button)
		if(SDL_GameControllerGetButton(controller, static_cast<SDL_GameControllerButton>(button)))
			ENGINE->events().blockControllerButton(instance, SDL_GameControllerGetStringForButton(static_cast<SDL_GameControllerButton>(button)));
	for(int axis = 0; axis < SDL_CONTROLLER_AXIS_MAX; ++axis)
	{
		const bool stick = axis < SDL_CONTROLLER_AXIS_TRIGGERLEFT;
		auto & latch = axisLatches[{instance, stick ? axis / 2 : axis}];
		latch.update(getRealAxisValue(SDL_GameControllerGetAxis(controller, static_cast<SDL_GameControllerAxis>(axis))), stick ? axis % 2 : 0);
		latch.cancel();
	}
}

ControllerPrompt::Family InputSourceGameController::getActiveControllerPromptFamily() const
{
	const auto active = gameControllerMap.find(activeController);
	if(active == gameControllerMap.end())
		return ControllerPrompt::Family::UNKNOWN;

	return controllerPromptFamily(active->second.get());
}

int InputSourceGameController::getJoystickIndex(SDL_GameController * controller)
{
	SDL_Joystick * joystick = SDL_GameControllerGetJoystick(controller);
	if(!joystick)
		return -1;

	SDL_JoystickID instanceID = SDL_JoystickInstanceID(joystick);
	if(instanceID < 0)
		return -1;
	return instanceID;
}

void InputSourceGameController::handleEventDeviceAdded(const SDL_ControllerDeviceEvent & device)
{
	// SDL reports a device index for ADDED events, while gameControllerMap is
	// keyed by the stable joystick instance ID resolved by openGameController.
	openGameController(device.which);
}

bool InputSourceGameController::handleEventDeviceRemoved(const SDL_ControllerDeviceEvent & device)
{
	if(gameControllerMap.find(device.which) == gameControllerMap.end())
	{
		logGlobal->warn("Game controller %d is not opened before.", device.which);
		return false;
	}
	const bool removedActiveController = activeController == device.which;
	if(removedActiveController)
		setActiveController(-1);
	ENGINE->events().forgetController(device.which);
	gameControllerMap.erase(device.which);
	std::erase_if(axisLatches, [&](const auto & entry) { return entry.first.first == device.which; });
	return removedActiveController;
}

void InputSourceGameController::handleEventDeviceRemapped(const SDL_ControllerDeviceEvent & device)
{
	if(gameControllerMap.find(device.which) == gameControllerMap.end())
	{
		logGlobal->warn("Game controller %d is not opened.", device.which);
		return;
	}

	// SDL updates the existing controller handle before reporting the remap.
	if(activeController == device.which)
	{
		resetInput();
		ENGINE->events().dispatchInputModeChanged(ENGINE->input().getCurrentInputMode());
	}
}

void InputSourceGameController::resetInput(bool dismissPopup)
{
	for(auto & [identity, latch] : axisLatches)
		latch.cancel();
	cursorAxisValueX = 0;
	cursorAxisValueY = 0;
	scrollAxisValueX = 0;
	scrollAxisValueY = 0;
	cursorPlanDisX = 0;
	cursorPlanDisY = 0;
	scrollPlanDisX = 0;
	scrollPlanDisY = 0;
	const bool cancelGesture = std::exchange(scrollAxisMoved, false);
	if(cancelGesture)
		ENGINE->events().dispatchGesturePanningCanceled(dismissPopup);
	ENGINE->events().cancelControllerInput(dismissPopup);
	if(gameControllerMap.count(activeController))
		latchHeldControls(activeController);
}

void InputSourceGameController::setActiveController(int instanceID)
{
	if(instanceID != -1 && !gameControllerMap.count(instanceID))
		return;
	if(activeController == instanceID)
		return;
	resetInput();
	activeController = instanceID;
	ENGINE->events().dispatchInputModeChanged(ENGINE->input().getCurrentInputMode());
}

bool InputSourceGameController::isAxisMotionActive(const SDL_ControllerAxisEvent & axis) const
{
	return !vstd::isAlmostZero(getRealAxisValue(axis.value));
}

double InputSourceGameController::getRealAxisValue(int value) const
{
	return ControllerInput::axisValue(value, configAxisDeadZone, configAxisFullZone);
}

void InputSourceGameController::dispatchAxisShortcuts(int instance, int axisValue, const std::string & axisName)
{
	if(getRealAxisValue(axisValue) > configTriggerThreshold)
		ENGINE->events().dispatchControllerButtonPressed(instance, axisName);
	else
		ENGINE->events().dispatchControllerButtonReleased(instance, axisName);
}

void InputSourceGameController::handleEventAxisMotion(const SDL_ControllerAxisEvent & axis)
{
	if(axis.which != activeController || !gameControllerMap.count(axis.which))
		return;
	const bool stick = axis.axis < SDL_CONTROLLER_AXIS_TRIGGERLEFT;
	if(!axisLatches[{axis.which, stick ? axis.axis / 2 : axis.axis}].update(getRealAxisValue(axis.value), stick ? axis.axis % 2 : 0))
		return;

	if(isAxisMotionActive(axis))
		tryToConvertCursor();

	SDL_GameControllerAxis axisID = static_cast<SDL_GameControllerAxis>(axis.axis);
	std::string axisName = SDL_GameControllerGetStringForAxis(axisID);

	auto axisActions = ENGINE->shortcuts().translateJoystickAxis(axisName);

	if(ENGINE->events().dispatchControllerAxis(axisActions, getRealAxisValue(axis.value)))
	{
		cursorAxisValueX = 0;
		cursorAxisValueY = 0;
		scrollAxisValueX = 0;
		scrollAxisValueY = 0;
		scrollAxisMoved = false;
		return;
	}

	for(const auto & action : axisActions)
	{
		switch(action)
		{
			case EShortcut::MOUSE_CURSOR_X:
				cursorAxisValueX = getRealAxisValue(axis.value);
				break;
			case EShortcut::MOUSE_CURSOR_Y:
				cursorAxisValueY = getRealAxisValue(axis.value);
				break;
			case EShortcut::MOUSE_SWIPE_X:
				scrollAxisValueX = getRealAxisValue(axis.value);
				break;
			case EShortcut::MOUSE_SWIPE_Y:
				scrollAxisValueY = getRealAxisValue(axis.value);
				break;
		}
	}

	dispatchAxisShortcuts(axis.which, axis.value, axisName);
}

void InputSourceGameController::tryToConvertCursor()
{
	if(ENGINE->cursor().getShowType() == Cursor::ShowType::HARDWARE)
	{
		int scalingFactor = ENGINE->screenHandler().getScalingFactor();
		const Point & cursorPosition = ENGINE->getCursorPosition();
		ENGINE->cursor().changeCursor(Cursor::ShowType::SOFTWARE);
		ENGINE->cursor().cursorMove(cursorPosition.x * scalingFactor, cursorPosition.y * scalingFactor);
		ENGINE->input().setCursorPosition(cursorPosition);
	}
}

void InputSourceGameController::handleEventButtonDown(const SDL_ControllerButtonEvent & button)
{
	if(!gameControllerMap.count(button.which))
		return;
	std::string buttonName = SDL_GameControllerGetStringForButton(static_cast<SDL_GameControllerButton>(button.button));
	ENGINE->events().dispatchControllerButtonPressed(button.which, buttonName);
}

void InputSourceGameController::handleEventButtonUp(const SDL_ControllerButtonEvent & button)
{
	if(!gameControllerMap.count(button.which))
		return;
	std::string buttonName = SDL_GameControllerGetStringForButton(static_cast<SDL_GameControllerButton>(button.button));
	ENGINE->events().dispatchControllerButtonReleased(button.which, buttonName);
}

void InputSourceGameController::doCursorMove(int deltaX, int deltaY)
{
	if(deltaX == 0 && deltaY == 0)
		return;
	const Point & screenSize = ENGINE->screenDimensions();
	const Point & cursorPosition = ENGINE->getCursorPosition();
	int scalingFactor = ENGINE->screenHandler().getScalingFactor();
	int newX = std::min(std::max(cursorPosition.x + deltaX, 0), screenSize.x);
	int newY = std::min(std::max(cursorPosition.y + deltaY, 0), screenSize.y);
	Point targetPosition{newX, newY};
	ENGINE->input().setCursorPosition(targetPosition);
	ENGINE->cursor().cursorMove(ENGINE->getCursorPosition().x * scalingFactor, ENGINE->getCursorPosition().y * scalingFactor);
}

int InputSourceGameController::getMoveDis(float planDis)
{
	if(planDis >= 0)
		return std::floor(planDis);
	else
		return std::ceil(planDis);
}

void InputSourceGameController::handleUpdate()
{
	std::chrono::steady_clock::time_point nowMs = std::chrono::steady_clock::now();

	if(lastCheckTime == std::chrono::steady_clock::time_point())
	{
		lastCheckTime = nowMs;
		return;
	}

	int32_t deltaTime = std::chrono::duration_cast<std::chrono::milliseconds>(nowMs - lastCheckTime).count();
	handleCursorUpdate(deltaTime);
	handleScrollUpdate(deltaTime);
	lastCheckTime = nowMs;
}

static double scaleAxis(double value, double power)
{
	if (value > 0)
		return std::pow(value, power);
	else
		return -std::pow(-value, power);
}

void InputSourceGameController::handleCursorUpdate(int32_t deltaTimeMs)
{
	float deltaTimeSeconds = static_cast<float>(deltaTimeMs) / 1000;

	if(vstd::isAlmostZero(cursorAxisValueX))
		cursorPlanDisX = 0;
	else
		cursorPlanDisX += deltaTimeSeconds * configAxisSpeed * scaleAxis(cursorAxisValueX, configAxisScale);

	if (vstd::isAlmostZero(cursorAxisValueY))
		cursorPlanDisY = 0;
	else
		cursorPlanDisY += deltaTimeSeconds * configAxisSpeed * scaleAxis(cursorAxisValueY, configAxisScale);

	int moveDisX = getMoveDis(cursorPlanDisX);
	int moveDisY = getMoveDis(cursorPlanDisY);
	cursorPlanDisX -= moveDisX;
	cursorPlanDisY -= moveDisY;
	doCursorMove(moveDisX, moveDisY);
}

void InputSourceGameController::handleScrollUpdate(int32_t deltaTimeMs)
{
	if(!scrollAxisMoved && isScrollAxisReleased())
	{
		return;
	}
	else if(!scrollAxisMoved && !isScrollAxisReleased())
	{
		scrollAxisMoved = true;
		scrollCurrent = scrollStart = ENGINE->input().getCursorPosition();
		ENGINE->events().dispatchGesturePanningStarted(scrollStart);
	}
	else if(scrollAxisMoved && isScrollAxisReleased())
	{
		ENGINE->events().dispatchGesturePanningEnded(scrollStart, scrollCurrent);
		scrollAxisMoved = false;
		scrollPlanDisX = scrollPlanDisY = 0;
		return;
	}
	float deltaTimeSeconds = static_cast<float>(deltaTimeMs) / 1000;
	scrollPlanDisX += deltaTimeSeconds * configAxisSpeed * scaleAxis(scrollAxisValueX, configAxisScale);
	scrollPlanDisY += deltaTimeSeconds * configAxisSpeed * scaleAxis(scrollAxisValueY, configAxisScale);
	int moveDisX = getMoveDis(scrollPlanDisX);
	int moveDisY = getMoveDis(scrollPlanDisY);
	if(moveDisX != 0 || moveDisY != 0)
	{
		scrollPlanDisX -= moveDisX;
		scrollPlanDisY -= moveDisY;
		scrollCurrent.x += moveDisX;
		scrollCurrent.y += moveDisY;
		Point distance(moveDisX, moveDisY);
		ENGINE->events().dispatchGesturePanning(scrollStart, scrollCurrent, distance);
	}
}

bool InputSourceGameController::isScrollAxisReleased() const
{
	return vstd::isAlmostZero(scrollAxisValueX) && vstd::isAlmostZero(scrollAxisValueY);
}
