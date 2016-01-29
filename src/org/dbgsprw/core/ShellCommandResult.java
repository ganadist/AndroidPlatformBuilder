package org.dbgsprw.core;

import java.util.ArrayList;

/**
 * Copyright 2016 dbgsprw / dbgsprw@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

public class ShellCommandResult {
    private ArrayList<String> mOutList;
    private ArrayList<String> mErrList;

    public ShellCommandResult(ArrayList<String> outList, ArrayList<String> errList) {
        mOutList = outList;
        mErrList = errList;
    }

    public ArrayList<String> getOutList() {
        return mOutList;
    }

    public ArrayList<String> getErrList() {
        return mErrList;
    }


}
