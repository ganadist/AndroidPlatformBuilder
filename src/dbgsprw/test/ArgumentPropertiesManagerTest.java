package dbgsprw.test;

import dbgsprw.view.ArgumentProperties;
import dbgsprw.view.ArgumentPropertiesManager;
import junit.framework.TestCase;

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

public class ArgumentPropertiesManagerTest extends TestCase{
    @Override
    public void setUp() throws Exception {
        super.setUp();

    }

    public void testProperties() throws Exception {
        ArgumentPropertiesManager argumentPropertiesManager = new ArgumentPropertiesManager();


        argumentPropertiesManager.putProperties("test", argumentPropertiesManager.loadProperties("target_argument.properties"));

        ArgumentProperties properties = argumentPropertiesManager.getProperties("test");
        for (String name : properties.getPropertyNames()) {
            System.out.print(name + " : ");
            for (String propertyValue : properties.getArguments(name, "/")) {
                System.out.print(propertyValue + " ");
            }
            System.out.println();
        }

    }


}