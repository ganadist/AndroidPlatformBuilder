/*
 * Copyright 2016 Young Ho Cha / ganadist@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dbgsprw.core.test;

import dbgsprw.core.Utils;
import junit.framework.TestCase;

import java.io.File;

/**
 * Created by ganadist on 16. 2. 17.
 */
public class UtilsTest extends TestCase {

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
