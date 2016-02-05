package org.dbgsprw;

import com.android.ddmlib.IShellOutputReceiver;
import com.android.ddmlib.MultiLineReceiver;
import org.dbgsprw.core.ShellCommandExecutor;

import java.io.File;
import java.util.ArrayList;

/**
 * Copyright 2016 dbgsprw / dbgsprw@gmail.com
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

public class Test {
    public static void main(String[] args) {
        ShellCommandExecutor shellCommandExecutor = new ShellCommandExecutor();
        ArrayList<String> command = new ArrayList<>();
        shellCommandExecutor.directory(new File("/"));
        command.add("ls");
        command.add("fdsafsf");
        /*
        shellCommandExecutor.executeShellCommand(command, new MultiLineReceiver() {
            @Override
            public void processNewLines(String[] lines) {
                for (String s : lines) {
                    System.out.println(s);
                }
            }

            @Override
            public boolean isCancelled() {
                return false;
            }
        });*/
    }
}
