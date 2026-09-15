package eu.vcmi.vcmi;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ThorContextIdsTest
{
    @Test
    public void identifiersMatchNativeContract()
    {
        assertEquals("UNKNOWN", ThorContextIds.UNKNOWN);
        assertEquals("MAIN_MENU", ThorContextIds.MAIN_MENU);
        assertEquals("MAIN_MENU_NEW_GAME", ThorContextIds.MAIN_MENU_NEW_GAME);
        assertEquals("MAIN_MENU_LOAD_GAME", ThorContextIds.MAIN_MENU_LOAD_GAME);
        assertEquals("MAIN_MENU_CAMPAIGN", ThorContextIds.MAIN_MENU_CAMPAIGN);
        assertEquals("MAIN_MENU_CREDITS", ThorContextIds.MAIN_MENU_CREDITS);
    }
}
