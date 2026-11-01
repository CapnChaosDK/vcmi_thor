package eu.vcmi.vcmi;

/** Revision-bound native journal page or final covered puzzle snapshot. */
final class ThorReferenceState
{
    static final ThorReferenceState EMPTY = new ThorReferenceState(0, new int[12], new String[]{""}, new byte[0]);
    final long revision;
    private final int[] data;
    private final String[] text;
    private final byte[] rgb;
    private ThorReferenceState(long revision, int[] data, String[] text, byte[] rgb)
    { this.revision = revision; this.data = data; this.text = text; this.rgb = rgb; }
    static boolean context(String id)
    { return ThorContextIds.QUEST_LOG.equals(id) || ThorContextIds.SCENARIO_EVENT_JOURNAL.equals(id) || ThorContextIds.PUZZLE_MAP.equals(id); }
    String context()
    { return kind() == 1 ? ThorContextIds.QUEST_LOG : kind() == 2 ? ThorContextIds.SCENARIO_EVENT_JOURNAL : ThorContextIds.PUZZLE_MAP; }
    private static boolean bounded(String s, int limit)
    {
        if (s == null || s.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > limit) return false;
        for (int i = 0; i < s.length(); ++i)
        {
            char c = s.charAt(i);
            if (c == 0 || Character.isLowSurrogate(c)) return false;
            if (Character.isHighSurrogate(c) && (++i >= s.length() || !Character.isLowSurrogate(s.charAt(i)))) return false;
        }
        return true;
    }
    static ThorReferenceState copyOf(long revision, int[] d, String[] text, byte[] rgb)
    {
        if (revision <= 0 || d == null || d.length < 12 || d[0] != 1 || d[1] < 1 || d[1] > 3
                || d[2] < 0 || d[3] < 0 || d[3] > 2000 || (d[3] == 0 ? d[2] != 0 : d[2] >= d[3])
                || d[4] < -1 || d[4] >= 10000 || d[5] < 0 || d[6] < 0 || d[6] > 512
                || (d[6] == 0 ? d[5] != 0 : d[5] >= d[6]) || d[7] < 0 || d[7] > 15 || (d[7] & 8) == 0
                || d[10] < 0 || d[10] > 5 || d.length != 12 + d[10] || text == null || text.length != 1 + d[10]
                || !bounded(text[0], 384) || d[6] == 0 && !text[0].isEmpty() || (d[7] & 4) != 0 && d[4] < 0 || rgb == null || d[11] < 0 || d[11] > 100) return EMPTY;
        if (d[1] == 3)
        {
            if (d[3] != 0 || d[4] != -1 || d[7] != 8 || d[10] != 0 || d[6] != 0
                    || !(d[8] == 0 && d[9] == 0 && rgb.length == 0
                    || d[8] == 592 && d[9] == 544 && rgb.length == 592 * 544 * 3)) return EMPTY;
        }
        else if (d[8] != 0 || d[9] != 0 || rgb.length != 0 || d[11] != 0
                || (d[3] == 0 ? d[10] != 0 || d[4] != -1 : d[4] >= d[3] * 5)) return EMPTY;
        for (int i = 0; i < d[10]; ++i)
            if (d[12 + i] != d[2] * 5 + i || d[12 + i] >= 10000 || !bounded(text[1 + i], 128) || text[1 + i].isEmpty()) return EMPTY;
        return new ThorReferenceState(revision, d.clone(), text.clone(), rgb.clone());
    }
    int kind() { return data[1]; }
    int page() { return data[2]; }
    int pages() { return data[3]; }
    int selected() { return data[4]; }
    int textPage() { return data[5]; }
    int textPages() { return data[6]; }
    int count() { return data[10]; }
    int progress() { return data[11]; }
    int width() { return data[8]; }
    int height() { return data[9]; }
    int target(int row) { return data[12 + row]; }
    String label(int row) { return text[1 + row]; }
    String body() { return text[0]; }
    int[] pixels()
    {
        int[] pixels = new int[rgb.length / 3];
        for (int i = 0; i < pixels.length; ++i)
            pixels[i] = 0xff000000 | (rgb[i * 3] & 255) << 16 | (rgb[i * 3 + 1] & 255) << 8 | rgb[i * 3 + 2] & 255;
        return pixels;
    }
    boolean allows(int command, int target)
    {
        if (this == EMPTY || !ThorExtendedCommands.validTarget(command, target)) return false;
        switch (command)
        {
            case ThorExtendedCommands.REFERENCE_SELECT:
                for (int i = 0; i < count(); ++i) if (target(i) == target) return true;
                return false;
            case ThorExtendedCommands.REFERENCE_PREVIOUS: return page() > 0;
            case ThorExtendedCommands.REFERENCE_NEXT: return page() + 1 < pages();
            case ThorExtendedCommands.REFERENCE_TEXT_PREVIOUS: return textPage() > 0;
            case ThorExtendedCommands.REFERENCE_TEXT_NEXT: return textPage() + 1 < textPages();
            case ThorExtendedCommands.REFERENCE_QUESTS: return kind() == 2 && (data[7] & 1) != 0;
            case ThorExtendedCommands.REFERENCE_EVENTS: return kind() == 1 && (data[7] & 2) != 0;
            case ThorExtendedCommands.REFERENCE_CENTER: return (data[7] & 4) != 0;
            case ThorExtendedCommands.REFERENCE_CLOSE: return true;
            default: return false;
        }
    }
}
