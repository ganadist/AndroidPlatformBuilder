package org.dbgsprw.core

/**
 * Copyright 2016 dbgsprw / dbgsprw@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at

 * http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

class BuilderTest extends GroovyTestCase {
    Builder mBuilder;

    @Override
    void setUp() {
        mBuilder = new Builder();
    }

    void testLoadLunchMenus() {
        for (String lunch : mBuilder.mLunchMenuList) {
            System.out.println(lunch);
        }
        assertTrue(mBuilder.mLunchMenuList.size() > 10);
    }

    void testExecuteMake() {
        mBuilder.setMakeOptions("7",
                "outTest",
                "aosp_hammerhead", "userdebug");
        mBuilder.executeMake();
    }

    void _testExecuteShellCommandAndPrintResult() {
        ArrayList<String> input = new ArrayList<>();
        ArrayList<String> outList = mBuilder.executeShellCommand(input).getOutList();

        for (String line : outList) {
            System.out.println(line);
        }
    }
}
