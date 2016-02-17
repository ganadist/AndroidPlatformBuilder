package dbgsprw.core;

import java.io.File;

public class Utils {
    private static final String EMPTY = "";

    public static String join(final char seperator, final Object[] array) {
        if (array == null) {
            return null;
        }
        if (array.length == 0) {
            return EMPTY;
        }

        final StringBuilder sb = new StringBuilder();
        final int startIndex = 0;
        for (int i = startIndex; i < array.length; i++) {
            if (i > startIndex) {
                sb.append(seperator);
            }
            if (array[i] != null) {
                sb.append(array[i]);
            }
        }
        return sb.toString();
    }

    public static String join(final char seperator, String ... array) {
        return join(seperator, (Object[])array);
    }

    public static String pathJoin(String ... paths) {
        return join(File.separatorChar, paths);
    }
}
