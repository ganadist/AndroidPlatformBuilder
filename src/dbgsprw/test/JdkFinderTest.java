package dbgsprw.test;

import com.intellij.notification.EventLog;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkTypeId;
import com.intellij.openapi.projectRoots.impl.ProjectJdkImpl;
import com.intellij.openapi.projectRoots.impl.ProjectJdkTableImpl;
import com.intellij.openapi.projectRoots.ui.ProjectJdksEditor;
import com.intellij.openapi.roots.ProjectRootManager;
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

public class JdkFinderTest {
    public void testFindAndroidHome(final Project project) {
        boolean getSdk = false;
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    for (Sdk sdk : ProjectJdkTable.getInstance().getAllJdks()) {
                        SdkTypeId sdkTypeId = sdk.getSdkType();
                        System.out.println(sdkTypeId.getName());
                    }
                    System.out.println("default = " + ProjectRootManager.getInstance(project).getProjectSdkName());
                    System.out.println("\n\n\n\n");
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
            }
        }).start();
        for (Sdk sdk : ProjectJdkTable.getInstance().getAllJdks()) {
            SdkTypeId sdkTypeId = sdk.getSdkType();
            if ("Android SDK".equals(sdkTypeId.getName())) {
                getSdk=true;
            }
        }
        System.out.println("findAndroidHome = " + getSdk);
    }
}
