package eu.vcmi.vcmi;

final class ThorContextDetails
{
    static final int COUNT = 4;

    private ThorContextDetails()
    {
    }

    static String orEmpty(final String value)
    {
        return value == null ? "" : value;
    }
}
