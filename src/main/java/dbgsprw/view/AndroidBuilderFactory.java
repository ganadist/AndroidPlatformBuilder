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

package dbgsprw.view;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.roots.impl.libraries.LibraryEx;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.roots.ui.configuration.ProjectStructureConfigurable;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ex.ToolWindowManagerEx;
import com.intellij.util.Consumer;
import dbgsprw.core.DeviceManager;
import dbgsprw.core.Utils;
import org.jetbrains.annotations.NotNull;

import javax.swing.event.HyperlinkEvent;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AndroidBuilderFactory implements ToolWindowFactory, ProjectManagerListener {
    private static final Map<String, AndroidBuilderView> sProjectMap = new HashMap<String, AndroidBuilderView>();
    private DeviceManager mDeviceManager = new DeviceManager();

    @Override
    public void createToolWindowContent(@NotNull final Project project, @NotNull ToolWindow toolWindow) {
        final String projectPath = project.getProjectFilePath();

        synchronized (sProjectMap) {
            if (sProjectMap.containsKey(projectPath)) {
                return;
            }

            if (getAndroidModule(project) == null) {
                ToolWindowManagerEx toolWindowManagerEx = ToolWindowManagerEx.getInstanceEx(project);
                toolWindowManagerEx.unregisterToolWindow("Android Builder");
                Notifications.Bus.notify(new Notification("Android Builder", "Android Builder",
                        "It seems project is not AOSP.\nAndroid Builder is disabled.", NotificationType.WARNING));
                return;
            }

            notifySetSdk(project);

            // FIXME
            // change Android Builder to be canWorkInDumbMode
            ToolWindowManagerEx toolWindowManagerEx = ToolWindowManagerEx.getInstanceEx(project);
            toolWindowManagerEx.unregisterToolWindow("Android Builder");
            toolWindow = toolWindowManagerEx.registerToolWindow("Android Builder", false, ToolWindowAnchor.RIGHT,
                    project, true);

            // FIXME
            // for refresh gui
            toolWindow.hide(null);
            toolWindow.show(null);


            AndroidBuilderView view = new AndroidBuilderView(project, toolWindow);
            mDeviceManager.adbInit(); // FIXME
            mDeviceManager.addDeviceStateListener(view);

            sProjectMap.put(projectPath, view);

            ProjectManager.getInstance().addProjectManagerListener(project, this);
        }
    }

    static Module getAndroidModule(Project project) {
        return ModuleManager.getInstance(project).findModuleByName("android");
    }

    public static AndroidBuilderView getInstance(final Project project) {
        synchronized (sProjectMap) {
            return sProjectMap.get(project.getProjectFilePath());
        }
    }

    public static void notifySetSdk(final Project project) {
        if (ProjectRootManager.getInstance(project).getProjectSdk() == null) {
            Notifications.Bus.notify(new Notification("Android Builder", "Android Builder",
                    "<a href=''>Project SDK is not selected. Set project SDK</a>",
                    NotificationType.WARNING,
                    new NotificationListener() {
                        @Override
                        public void hyperlinkUpdate(@NotNull Notification notification, @NotNull HyperlinkEvent event) {
                            ProjectStructureConfigurable configurable = ProjectStructureConfigurable.getInstance(project);
                            configurable.selectProjectGeneralSettings(true);
                            ShowSettingsUtil.getInstance().editConfigurable(project,
                                    configurable);
                        }
                    }));
        }
    }

    public static void showNotification(String message, NotificationType type) {
        Notifications.Bus.notify(new Notification("Android Builder", "Android Builder", message, type));
    }

    @Override
    public void projectOpened(Project project) {

    }

    @Override
    public boolean canCloseProject(Project project) {
        return true;
    }

    @Override
    public void projectClosed(Project project) {
        AndroidBuilderView view = sProjectMap.remove(project.getProjectFilePath());
        view.prepareClose();
        mDeviceManager.removeDeviceStateListener(view);
        ProjectManager.getInstance().removeProjectManagerListener(project, this);
    }

    @Override
    public void projectClosing(Project project) {

    }
}
