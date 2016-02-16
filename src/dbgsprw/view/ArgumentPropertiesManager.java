package dbgsprw.view;

import java.io.IOException;
import java.util.*;

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

public class ArgumentPropertiesManager {

    private HashMap<String, ArgumentProperties> mPropertiesHashMap;
    private ArgumentProperties mFastBootArgumentProps;
    private ArgumentProperties mAdbSyncArgumentProps;
    private ArgumentProperties mTargetArgumentProps;
    private ClassLoader mClassLoader;

    public ArgumentPropertiesManager() {
        mClassLoader = this.getClass().getClassLoader();
        mPropertiesHashMap = new HashMap<>();
    }

    public ArgumentPropertiesManager(ClassLoader classLoader) {
        mClassLoader = classLoader;
        mPropertiesHashMap = new HashMap<>();
    }

    public ArgumentProperties loadProperties(String path) {
        /*
        mFastBootArgumentProps = new Properties();
        mAdbSyncArgumentProps = new Properties();
        mTargetArgumentProps = new Properties();
        try {
            mFastBootArgumentProps.load(classLoader.getResourceAsStream("fastboot_argument.properties"));
            mAdbSyncArgumentProps.load(classLoader.getResourceAsStream("adb_sync_argument.properties"));
            mTargetArgumentProps.load(classLoader.getResourceAsStream("target_argument.properties"));

        } catch (IOException e) {
            e.printStackTrace();
        }*/
        ArgumentProperties properties = new ArgumentProperties();
        try {
            properties.load(mClassLoader.getResourceAsStream(path));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return properties;
    }

    public void putProperties(String propertiesName, ArgumentProperties properties) {
        mPropertiesHashMap.put(propertiesName, properties);
    }

    public ArgumentProperties getProperties(String propertiesName) {
        return mPropertiesHashMap.get(propertiesName);
    }

}
