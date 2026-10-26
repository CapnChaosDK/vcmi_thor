package eu.vcmi.vcmi;

/** View controls have disjoint resource, quantity and footer ranges. */
final class ThorMarketControls
{
    static final int SELL_FIRST = 1, BUY_FIRST = 10, LESS_TEN = 19, LESS = 20,
            MORE = 21, MORE_TEN = 22, MAX = 23, TRADE = 24, RESET = 25, CLOSE = 26, PREVIOUS = 27, NEXT = 28, PAGE_SIZE = 4;
    static int command(final int control)
    {
        if (control >= SELL_FIRST && control < BUY_FIRST) return ThorExtendedCommands.MARKET_SELL;
        if (control >= BUY_FIRST && control < LESS_TEN) return ThorExtendedCommands.MARKET_BUY;
        if (control >= LESS_TEN && control <= MAX) return ThorExtendedCommands.MARKET_AMOUNT;
        if (control == TRADE) return ThorExtendedCommands.MARKET_TRADE;
        if (control == RESET) return ThorExtendedCommands.MARKET_RESET;
        if (control == CLOSE) return ThorExtendedCommands.MARKET_CLOSE;
        return 0;
    }
    static int target(final int control, final ThorMarketState state)
    {
        if (control >= SELL_FIRST && control < LESS_TEN)
        {
            final int row = control < BUY_FIRST ? control - SELL_FIRST : control - BUY_FIRST;
            return row < state.count() ? state.id(row) : -1;
        }
        if (control >= LESS_TEN && control <= MAX)
        {
            final int delta = control == LESS_TEN ? -10 : control == LESS ? -1 : control == MORE ? 1 : 10;
            return control == MAX ? state.maximum()
                    : (int) Math.max(0L, Math.min(state.maximum(), (long) state.amount() + delta));
        }
        return -1;
    }
    static boolean enabled(final int control, final ThorMarketState state)
    {
        final int target = target(control, state);
        return state.allows(command(control), target)
                && (command(control) != ThorExtendedCommands.MARKET_AMOUNT || target != state.amount());
    }
    static boolean enabled(final int control, final ThorMarketState state, final int page)
    {
        if (control == PREVIOUS) return page > 0;
        if (control == NEXT) return (page + 1) * PAGE_SIZE < state.count();
        return enabled(control, state);
    }
    static float[] bounds(final int control, final ThorPresentationLayout layout, final int count, final int page)
    {
        if (!layout.hasContent() || control < SELL_FIRST || control > NEXT) return null;
        final float left = layout.margin + layout.bevel * 3, width = layout.right - layout.bevel * 3 - left;
        final float top = layout.margin, height = layout.contentHeight, gap = width * .025f;
        if (control == PREVIOUS || control == NEXT)
        {
            final float x = control == PREVIOUS ? left : left + width * .82f;
            return new float[]{x, top + height * .64f, x + width * .18f, top + height * .75f};
        }
        if (control < LESS_TEN)
        {
            final int row = control < BUY_FIRST ? control - SELL_FIRST : control - BUY_FIRST;
            if (row >= count || row / PAGE_SIZE != page) return null;
            final int column = control < BUY_FIRST ? 0 : 1;
            final float w = (width - gap) / 2, h = height * .44f / PAGE_SIZE;
            final float x = left + column * (w + gap), y = top + height * .18f + (row % PAGE_SIZE) * h;
            return new float[]{x, y, x + w, y + h * .88f};
        }
        final boolean footer = control >= TRADE;
        final int columns = footer ? 3 : 5, index = control - (footer ? TRADE : LESS_TEN);
        final float w = (width - gap * (columns - 1)) / columns;
        final float x = left + index * (w + gap), y = top + height * (footer ? .885f : .775f);
        return new float[]{x, y, x + w, y + height * .10f};
    }
    static int at(final float x, final float y, final ThorPresentationLayout layout, final int count, final int page)
    {
        for (int control = SELL_FIRST; control <= NEXT; ++control)
        {
            final float[] b = bounds(control, layout, count, page);
            if (b != null && x >= b[0] && x < b[2] && y >= b[1] && y < b[3]) return control;
        }
        return 0;
    }
}
