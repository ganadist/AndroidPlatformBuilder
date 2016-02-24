package dbgsprw.view;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ex.ToolWindowManagerEx;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

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

public class AndroidBuilderFactory implements ToolWindowFactory, ProjectManagerListener {
    private static final Map<String, AndroidBuilderView> sProjectMap = new HashMap<String, AndroidBuilderView>();

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
                        "This project is not AOSP.", NotificationType.ERROR));
                return;
            }

            // change Android Builder to be canWorkInDumbMode
            ToolWindowManagerEx toolWindowManagerEx = ToolWindowManagerEx.getInstanceEx(project);
            toolWindowManagerEx.unregisterToolWindow("Android Builder");
            toolWindow = toolWindowManagerEx.registerToolWindow("Android Builder", false, ToolWindowAnchor.RIGHT,
                    project, true);

            // for refresh gui
            toolWindow.hide(null);
            toolWindow.show(null);

            AndroidBuilderView view = new AndroidBuilderView(project, toolWindow);

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

    @Override
    public void projectOpened(Project project) {

    }

    @Override
    public boolean canCloseProject(Project project) {
        return true;
    }

    @Override
    public void projectClosed(Project project) {

    }

    @Override
    public void projectClosing(Project project) {

    }
}
