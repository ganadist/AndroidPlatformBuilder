/*
 * Copyright 2016 Young Ho Cha / ganadist@gmail.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package dbgsprw.core.test

import dbgsprw.core.AndroidJdk
import dbgsprw.core.AndroidVersion
import dbgsprw.core.parseMakefileLine
import junit.framework.TestCase

/**
 * Created by ganadist on 16. 3. 19.
 */
class AndroidVersionTest : TestCase() {

    fun parseAndCheckLine(line: String, key: String, value: String) {
        val p = parseMakefileLine(line)
        assertEquals(key, p.first)
        assertEquals(value, p.second)
    }

    fun testParseMakeLine() {
        parseAndCheckLine("Hello = World", "Hello", "World")
        parseAndCheckLine("Hello=World", "Hello", "World")
        parseAndCheckLine(" Hello=World ", "Hello", "World")
        parseAndCheckLine(" Hello = World ", "Hello", "World")
        parseAndCheckLine("Hello := World", "Hello", "World")
        parseAndCheckLine("export Hello := World", "Hello", "World")
        parseAndCheckLine("Hello =", "Hello", "")
        parseAndCheckLine("Hello =  ", "Hello", "")
        parseAndCheckLine("\tHello =  ", "Hello", "")
    }

    fun performTestParseMakefile(root: String, expectedApiLevel: Int, expectedJdkVersion: AndroidJdk) {
        val version = AndroidVersion(root)
        assertEquals(expectedApiLevel, version.mPlatformApiLevel)
        assertEquals(expectedJdkVersion, version.mJdk)
    }

    fun testParseMakefile() {
        performTestParseMakefile("src/test/build/api-4", 4, AndroidJdk.JDK_1_5)
        performTestParseMakefile("src/test/build/api-6", 6, AndroidJdk.JDK_1_5)
        performTestParseMakefile("src/test/build/api-7", 7, AndroidJdk.JDK_1_5)
        performTestParseMakefile("src/test/build/api-8", 8, AndroidJdk.JDK_1_5)
        performTestParseMakefile("src/test/build/api-9", 9, AndroidJdk.JDK_1_6)
        performTestParseMakefile("src/test/build/api-10", 10, AndroidJdk.JDK_1_6)
        performTestParseMakefile("src/test/build/api-14", 14, AndroidJdk.JDK_1_6)
        performTestParseMakefile("src/test/build/api-15", 15, AndroidJdk.JDK_1_6)
        performTestParseMakefile("src/test/build/api-16", 16, AndroidJdk.JDK_1_6)
        performTestParseMakefile("src/test/build/api-17", 17, AndroidJdk.JDK_1_6)
        performTestParseMakefile("src/test/build/api-18", 18, AndroidJdk.JDK_1_6)
        performTestParseMakefile("src/test/build/api-19", 19, AndroidJdk.JDK_1_6)
        performTestParseMakefile("src/test/build/api-21", 21, AndroidJdk.JDK_1_7)
        performTestParseMakefile("src/test/build/api-22", 22, AndroidJdk.JDK_1_7)
        performTestParseMakefile("src/test/build/api-23", 23, AndroidJdk.JDK_1_7)
        performTestParseMakefile("src/test/build/api-N", 23, AndroidJdk.JDK_1_8)

    }
}