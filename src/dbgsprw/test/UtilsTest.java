package dbgsprw.test;

import junit.framework.TestCase;

import dbgsprw.core.Utils;

import java.io.File;

/**
 * Created by ganadist on 16. 2. 17.
 */
public class UtilsTest extends TestCase{

    public void testJoin() {
        final char sep = ' ';
        assertEquals(Utils.join(sep, null), null);

        final String[] values1 = {};
        assertEquals(Utils.join(sep, values1), "");

        final String[] values2 = {"hello", "world"};
        final String expected2 = "hello" + sep + "world";
        // test for variable argument
        assertEquals(Utils.join(sep, values2[0], values2[1]), expected2);

        // test for array argument
        assertEquals(Utils.join(sep, values2), expected2);

        final Object[] values3 = {values2[0], values2[1]};
        assertEquals(Utils.join(sep, values3), expected2);

    }

    public void testPathJoin() {
        final String path1 = "android";
        final String path2 = "build";
        final String path3 = "main.mk";
        final String expected1 = new File(path1).getPath();
        final String expected2 = new File(expected1, path2).getPath();
        final String expected3 = new File(expected2, path3).getPath();
        assertEquals(Utils.pathJoin(path1), expected1);
        assertEquals(Utils.pathJoin(path1, path2), expected2);
        assertEquals(Utils.pathJoin(path1, path2, path3), expected3);
    }
}
