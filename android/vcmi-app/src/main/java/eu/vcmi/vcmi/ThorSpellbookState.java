package eu.vcmi.vcmi;

/** Defensive, bounded copy of one native spellbook. School IDs include mod schools. */
final class ThorSpellbookState
{
    static final int MAX_ROWS = 256, MAX_SCHOOLS = 32;
    static final ThorSpellbookState EMPTY = new ThorSpellbookState(0, -1, 0, false, "", new int[0], new String[0], new Row[0]);
    static final class Row
    {
        final int id, level, cost;
        final boolean combat, castable;
        final String name, details;
        private final int[] schools;
        Row(int id, int level, int cost, boolean combat, boolean castable, String name, String details, int[] schools)
        { this.id = id; this.level = level; this.cost = cost; this.combat = combat; this.castable = castable; this.name = name; this.details = details; this.schools = schools; }
        boolean hasSchool(int school) { for (int value : schools) if (value == school) return true; return false; }
    }
    final long revision;
    final int hero, mana;
    final boolean battle;
    final String heroName;
    private final int[] schools;
    private final String[] schoolNames;
    private final Row[] rows;
    private ThorSpellbookState(long revision, int hero, int mana, boolean battle, String heroName, int[] schools, String[] names, Row[] rows)
    { this.revision = revision; this.hero = hero; this.mana = mana; this.battle = battle; this.heroName = heroName; this.schools = schools; this.schoolNames = names; this.rows = rows; }
    private static boolean bounded(String value, int max)
    { return value != null && value.getBytes(java.nio.charset.StandardCharsets.UTF_8).length <= max; }
    static ThorSpellbookState copyOf(long revision, int[] data, String[] text)
    {
        if (revision <= 0 || data == null || data.length < 6 || data.length > 6 + MAX_SCHOOLS + MAX_ROWS * (6 + MAX_SCHOOLS)
                || data[0] != 1 || data[1] < 0 || data[2] < 0 || data[3] < 0 || data[3] > 1
                || data[4] < 0 || data[4] > MAX_SCHOOLS || data[5] < 0 || data[5] > MAX_ROWS
                || text == null || text.length != 1 + data[4] + data[5] * 2 || !bounded(text[0], 256) || text[0].isEmpty()
                || data.length < 6 + data[4]) return EMPTY;
        int[] schools = java.util.Arrays.copyOfRange(data, 6, 6 + data[4]);
        String[] names = java.util.Arrays.copyOfRange(text, 1, 1 + schools.length);
        for (int i = 0; i < schools.length; ++i)
        {
            if (schools[i] < 0 || !bounded(names[i], 256) || names[i].isEmpty()) return EMPTY;
            for (int j = 0; j < i; ++j) if (schools[j] == schools[i]) return EMPTY;
        }
        Row[] rows = new Row[data[5]];
        int offset = 6 + schools.length;
        for (int i = 0; i < rows.length; ++i)
        {
            if (offset + 6 > data.length) return EMPTY;
            int id = data[offset++], level = data[offset++], cost = data[offset++], combat = data[offset++], cast = data[offset++], count = data[offset++];
            String name = text[1 + schools.length + i * 2], details = text[2 + schools.length + i * 2];
            if (id < 0 || level < 0 || level > 5 || cost < 0 || combat < 0 || combat > 1 || cast < 0 || cast > 1
                    || count < 0 || count > MAX_SCHOOLS || offset + count > data.length
                    || cast == 1 && (cost > data[2] || combat != data[3]) || !bounded(name, 256) || name.isEmpty() || !bounded(details, 4096)) return EMPTY;
            for (int j = 0; j < i; ++j) if (rows[j].id == id) return EMPTY;
            int[] rowSchools = java.util.Arrays.copyOfRange(data, offset, offset + count); offset += count;
            for (int j = 0; j < count; ++j)
            {
                boolean found = false;
                for (int school : schools) if (school == rowSchools[j]) found = true;
                if (!found) return EMPTY;
                for (int k = 0; k < j; ++k) if (rowSchools[k] == rowSchools[j]) return EMPTY;
            }
            rows[i] = new Row(id, level, cost, combat == 1, cast == 1, name, details, rowSchools);
        }
        if (offset != data.length) return EMPTY;
        return new ThorSpellbookState(revision, data[1], data[2], data[3] == 1, text[0], schools, names, rows);
    }
    int count() { return rows.length; }
    Row row(int i) { return rows[i]; }
    int schoolCount() { return schools.length; }
    int school(int i) { return schools[i]; }
    String schoolName(int i) { return schoolNames[i]; }
    boolean allows(int command, int target)
    {
        if (this == EMPTY || !ThorExtendedCommands.validTarget(command, target)) return false;
        if (command == ThorExtendedCommands.SPELLBOOK_CLOSE) return true;
        if (command == ThorExtendedCommands.SPELLBOOK_CAST)
            for (Row row : rows) if (row.id == target) return row.castable;
        return false;
    }
}
