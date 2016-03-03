/*
 * Copyright 2016 dbgsprw / dbgsprw@gmail.com
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
 *
 */

package dbgsprw.core;

import com.intellij.openapi.diagnostic.Logger;

import java.io.IOException;
import java.util.Properties;

/**
 * Created by ganadist on 16. 3. 3.
 */
public class PropertiesLoader {
    private static final Logger LOG = Logger.getInstance(PropertiesLoader.class);
    private ClassLoader mClassLoader;

    public PropertiesLoader() {
        init(this.getClass().getClassLoader());
    }

    public PropertiesLoader(ClassLoader cl) {
        init(cl);
    }

    private void init(ClassLoader cl) {
        mClassLoader = cl;
    }

    public Properties getProperties(String path) {
        Properties properties = new Properties();
        try {
            properties.load(mClassLoader.getResourceAsStream(path));
        } catch (IOException e) {
            LOG.warn("load failed properties file: " + path, e);
        }
        return properties;
    }
}
