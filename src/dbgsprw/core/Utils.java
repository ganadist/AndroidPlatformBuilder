package dbgsprw.core;

import javax.swing.*;
import java.io.File;
import java.lang.reflect.InvocationTargetException;

public class Utils {
    private static final String EMPTY = "";
    public static final String ANDROID_MK = "Android.mk";

    public static String join(final char separator, final Object[] array) {
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
                sb.append(separator);
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

    public static String findAndroidMkOnParent(String root, String filename) {
        File file = new File(filename);
        if (!file.getAbsolutePath().startsWith(root)) {
            return null; // filename is out of project root directory.
        }
        while (true) {
            File parent = file.getParentFile();
            File AndroidMk = new File(parent, ANDROID_MK);
            if (AndroidMk.exists()) {
                String path = AndroidMk.getPath();
                return path.substring(root.length() + 1, path.length() - ANDROID_MK.length() - 1);
            }
            if (root.equals(parent.getAbsolutePath())) {
                return null; // cannot find Android.mk
            }
            file = parent;
        }
    }

    public static void runOnUi(Runnable runnable) {
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
        } else {
            SwingUtilities.invokeLater(runnable);
        }
    }
}
