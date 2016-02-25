package dbgsprw.view;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ModuleRootModificationUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ex.ToolWindowManagerEx;
import dbgsprw.core.Utils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
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
                        "This project is not AOSP.", NotificationType.WARNING));
                return;
            }

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

            updateExcludeFoldersFirst(project);
            AndroidBuilderView view = new AndroidBuilderView(project, toolWindow);

            sProjectMap.put(projectPath, view);
            ProjectManager.getInstance().addProjectManagerListener(project, this);
        }
    }

    static Module getAndroidModule(Project project) {
        return ModuleManager.getInstance(project).findModuleByName("android");
    }

    private static final String[] EXCLUDE_FOLDER_INITIAL = {
            "abi",
            "bootable",
            "build",
            "developers",
            "development",
            "device",
            "docs",
            "kernel",
            "ndk",
            "pdk",
            "prebuilts",
            "sdk",
            "tools",
    };

    private static final String[] EXCLUDE_FOLDER_TEMPLATES = {
            "eclipse",
            "host",
            "target/common/docs",
            "target/common/obj/JAVA_LIBRARIES/android_stubs_current_intermediates",
            "target/common/R",
            "target/product",
    };

    private static class OutDirFileFilter implements FilenameFilter {
        private final String mOutDir;
        OutDirFileFilter(String outdir) {
            mOutDir = outdir;
        }
        @Override
        public boolean accept(File dir, String name) {
            if (mOutDir.equals(name)) {
                return false;
            }
            return name.startsWith("out");
        }
    }

    private static void updateExcludeFoldersFirst(Project project) {
        final Module module = getAndroidModule(project);
        final VirtualFile root = module.getModuleFile().getParent();
        final String rootUrl = root.getUrl();
        final ArrayList<String> excludedDirs = new ArrayList<String>();
        final ArrayList<String> unExcludedDirs = new ArrayList<String>();
        for (String dirname: EXCLUDE_FOLDER_INITIAL) {
            excludedDirs.add(Utils.join('/', rootUrl, dirname));
        }
        ModuleRootModificationUtil.updateExcludedFolders(module, root, unExcludedDirs, excludedDirs);
    }

    public static void updateExcludeFolders(Project project, final String outDir) {
        final Module module = getAndroidModule(project);
        final VirtualFile root = module.getModuleFile().getParent();
        final String rootUrl = root.getUrl();
        final ArrayList<String> excludedDirs = new ArrayList<String>();
        final ArrayList<String> unExcludedDirs = new ArrayList<String>();
        for (String dirname:
                new File(root.getPath()).list(new OutDirFileFilter(outDir))) {
            excludedDirs.add(Utils.join('/', rootUrl, dirname));
        }
        for (String dirname: EXCLUDE_FOLDER_TEMPLATES) {
            excludedDirs.add(Utils.join('/', rootUrl, outDir, dirname));
        }
        unExcludedDirs.add(Utils.join('/', rootUrl, outDir));

        ModuleRootModificationUtil.updateExcludedFolders(module, root, unExcludedDirs, excludedDirs);
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
