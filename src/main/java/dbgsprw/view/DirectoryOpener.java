/*
 * Copyright 2016 dbgsprw / dbgsprw@gmail.com
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
 *
 */

package dbgsprw.view;

import dbgsprw.core.PropertiesLoader;

import java.util.ArrayList;
import java.util.Properties;

public class DirectoryOpener {
    private static final String OS_FILE_SYSTEM_PROPERTIES_PATH = "properties/os_file_system_command.properties";
    private static final String sFileManagerCommand;

    static {
        PropertiesLoader pl = new PropertiesLoader();
        Properties properties = pl.getProperties(OS_FILE_SYSTEM_PROPERTIES_PATH);
        final String osName = System.getProperty("os.name");
        sFileManagerCommand = properties.getProperty(osName);
    }

    public static void openDirectory(String path) {
        final ArrayList<String> command = new ArrayList<String>();
        command.add(sFileManagerCommand);
        command.add(path);

        // FIXME
        new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    Process process = new ProcessBuilder().command(command).start();
                    process.waitFor();
                } catch (Exception ex) {
                }
            }
        }).start();
    }
}
