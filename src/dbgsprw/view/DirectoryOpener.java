package dbgsprw.view;

import dbgsprw.core.ShellCommandExecutor;
import dbgsprw.exception.FileManagerNotFoundException;

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

public class DirectoryOpener {
    private static final String OS_FILE_SYSTEM_PROPERTIES_PATH = "properties/os_file_system_command.properties";
    private static ShellCommandExecutor sShellCommandExecutor;
    private static String sFileManagerCommand = null;

    static {
        sShellCommandExecutor = new ShellCommandExecutor();
        ArgumentPropertiesManager argumentPropertiesManager = new ArgumentPropertiesManager();
        ArgumentProperties osFileSystemProperties =
                argumentPropertiesManager.loadProperties(OS_FILE_SYSTEM_PROPERTIES_PATH);
        String osName = System.getProperty("os.name");
        for (String propertyName : osFileSystemProperties.getPropertyNames()) {
            if (osName.contains(propertyName)) {
                sFileManagerCommand = osFileSystemProperties.getProperty(propertyName);
                break;
            }
        }
    }

    public static void openDirectory(String path)
            throws FileManagerNotFoundException {
        if (sFileManagerCommand == null) {
            throw new FileManagerNotFoundException();
        }
        ArrayList<String> command = new ArrayList<String>();
        command.add(sFileManagerCommand);
        command.add(path);
        sShellCommandExecutor.executeShellCommand(command);
    }
}
