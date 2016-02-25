package dbgsprw.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Properties;
import java.util.Set;

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

public class ArgumentProperties extends Properties {
    public ArrayList<String> getPropertyNames() {
        ArrayList<String> propertiesNameList = new ArrayList();
        Set<String> nameSet = stringPropertyNames();

        for (String name : nameSet) {
            propertiesNameList.add(name);
        }
        Collections.sort(propertiesNameList);
        return propertiesNameList;
    }

    public String[] getArguments(String name, String delimiter) {
        String valueLine = getProperty(name);
        String[] strings;
        if (valueLine.contains(delimiter)) {
            strings = valueLine.split(delimiter);
        } else {
            strings = new String[]{valueLine};
        }
        return strings;
    }
}
