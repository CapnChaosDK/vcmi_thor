package eu.vcmi.vcmi;

/** Thor-local preference contract, backed by app-private SharedPreferences on Android. */
final class ThorHapticPreference
{
    static final String FILE_NAME = "vcmi_thor_presentation";
    static final String KEY_ENABLED = "lower_deck_haptics_enabled";

    interface Store
    {
        boolean getBoolean(String key, boolean defaultValue);
        void putBoolean(String key, boolean value);
    }

    private ThorHapticPreference()
    {
    }

    static boolean load(final Store store)
    {
        return store.getBoolean(KEY_ENABLED, true);
    }

    static void save(final Store store, final boolean enabled)
    {
        store.putBoolean(KEY_ENABLED, enabled);
    }
}
