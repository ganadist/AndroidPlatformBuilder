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

            updateExcludeFoldersFirst(project);
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

    private static final String[] EXCLUDE_FOLDER_INITIAL = {
            "abi",
            "bootable",
            "build",
            "dalvik/dx/src/junit",
            "dalvik/libcore/luni/src/test/java/junit",
            "dalvik/test",
            "developers",
            "development",
            "device",
            "docs",
            "frameworks/base/docs",
            "kernel",
            "ndk",
            "pdk",
            "prebuilts",
            "sdk",
            "tools",
            "vendor",
    };

    private static final String[] EXCLUDE_FOLDER_TEMPLATES = {
            "eclipse",
            "host",
            "target/common/docs",
            "target/common/obj/JAVA_LIBRARIES/android_stubs_current_intermediates",
            "target/common/R",
            "target/product",
    };

    private static final String[] FRAMEWORK_LIBRARY_SOURCES = {
            "frameworks/base/core/java",
            "frameworks/base/drm/java",
            "frameworks/base/graphics/java",
            "frameworks/base/keystore/java",
            "frameworks/base/location/java",
            "frameworks/base/location/lib/java",
            "frameworks/base/media/java",
            "frameworks/base/nfc-extras/java",
            "frameworks/base/opengl/java",
            "frameworks/base/rs/java",
            "frameworks/base/sax/java",
            "frameworks/base/services/accessibility/java",
            "frameworks/base/services/appwidget/java",
            "frameworks/base/services/backup/java",
            "frameworks/base/services/core/java",
            "frameworks/base/services/devicepolicy/java",
            "frameworks/base/services/java",
            "frameworks/base/services/midi/java",
            "frameworks/base/services/net/java",
            "frameworks/base/services/print/java",
            "frameworks/base/services/restrictions/java",
            "frameworks/base/services/usage/java",
            "frameworks/base/services/usb/java",
            "frameworks/base/services/voiceinteraction/java",
            "frameworks/base/telecomm/java",
            "frameworks/base/telephony/java",
            "frameworks/base/voip/java",
            "frameworks/base/wifi/java",
            "frameworks/opt/net/ethernet/src/java",
            "frameworks/opt/net/ims/src/java",
            "frameworks/opt/net/voip/src/java",
            "frameworks/opt/net/wifi/src/java",
            "frameworks/opt/telephony/src/java",
    };

    private static final String[] FRAMEWORK_LIBRARY_NAMES = {
            "framework",
            "services.core",
            "services",
            "telephony-common",
            "voip-common",
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
        for (String dirname : EXCLUDE_FOLDER_INITIAL) {
            excludedDirs.add(Utils.join('/', rootUrl, dirname));
        }
        ModuleRootModificationUtil.updateExcludedFolders(module, root, unExcludedDirs, excludedDirs);
    }

    private static String getProjectDirectory(String rootUrl, String directory) {
        StringBuilder sb = new StringBuilder(rootUrl);
        sb.append('/').append(directory);
        return sb.toString();
    }

    private static String getFrameworkJar(String rootPath, String outDir, String libName) {
        StringBuilder sb = new StringBuilder();
        String rootUrl = "jar://" + rootPath;
        sb.append(getProjectDirectory(rootUrl, outDir));
        sb.append("/target/common/obj/JAVA_LIBRARIES/");
        sb.append(libName);
        sb.append("_intermediates/classes-full-debug.jar!/");
        return sb.toString();
    }

    private static String getFrameworkSource(String rootUrl, String outDir, String libName) {
        StringBuilder sb = new StringBuilder();
        sb.append(getProjectDirectory(rootUrl, outDir));
        sb.append("/target/common/obj/JAVA_LIBRARIES/");
        sb.append(libName);
        sb.append("_intermediates/src/");
        return sb.toString();
    }

    private static void updateModel(@NotNull final Module module, @NotNull Consumer<ModifiableRootModel> task) {
        final ModifiableRootModel model = ApplicationManager.getApplication().runReadAction(new Computable<ModifiableRootModel>() {
            @Override
            public ModifiableRootModel compute() {
                return ModuleRootManager.getInstance(module).getModifiableModel();
            }
        });
        try {
            task.consume(model);
            doWriteAction(new Runnable() {

                @Override
                public void run() {
                    model.commit();
                }
            });
        } catch (RuntimeException ex) {
            model.dispose();
            throw ex;
        } catch (Error ex) {
            model.dispose();
            throw ex;
        }
    }

    private static void doWriteAction(final Runnable action) {
        final Application app = ApplicationManager.getApplication();
        app.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                app.runWriteAction(action);
            }
        }, app.getDefaultModalityState());
    }

    private static final String FRAMEWORK_LIBNAME = "framework";

    private static void updateModuleLibrary(Project project, final List<String> classesRoots,
                                            final List<String> sourcesRoots) {

        final Module module = getAndroidModule(project);
        updateModel(module, new Consumer<ModifiableRootModel>() {
            @Override
            public void consume(ModifiableRootModel model) {
                LibraryTable table = model.getModuleLibraryTable();

                for (Library library : table.getLibraries()) {
                    table.removeLibrary(library);
                }

                final LibraryEx library = (LibraryEx) table.createLibrary(FRAMEWORK_LIBNAME);
                final LibraryEx.ModifiableModelEx libraryModel = library.getModifiableModel();
                for (String root : classesRoots) {
                    libraryModel.addRoot(root, OrderRootType.CLASSES);
                }
                for (String root : sourcesRoots) {
                    libraryModel.addRoot(root, OrderRootType.SOURCES);
                }
                LibraryOrderEntry entry = model.findLibraryOrderEntry(library);
                assert entry != null : library;
                entry.setScope(DependencyScope.COMPILE);
                entry.setExported(false);

                doWriteAction(new Runnable() {
                    @Override
                    public void run() {
                        libraryModel.commit();
                    }
                });
            }
        });
    }

    public static void updateOutDir(Project project, final String outDir) {
        final Module module = getAndroidModule(project);
        final VirtualFile root = module.getModuleFile().getParent();
        final String rootUrl = root.getUrl();
        final ArrayList<String> excludedDirs = new ArrayList<String>();
        final ArrayList<String> unExcludedDirs = new ArrayList<String>();
        for (String dirname :
                new File(root.getPath()).list(new OutDirFileFilter(outDir))) {
            excludedDirs.add(Utils.join('/', rootUrl, dirname));
        }
        for (String dirname : EXCLUDE_FOLDER_TEMPLATES) {
            excludedDirs.add(Utils.join('/', rootUrl, outDir, dirname));
        }
        unExcludedDirs.add(Utils.join('/', rootUrl, outDir));

        ModuleRootModificationUtil.updateExcludedFolders(module, root, unExcludedDirs, excludedDirs);

        final ArrayList<String> classesRoots = new ArrayList<String>();
        final ArrayList<String> sourcesRoots = new ArrayList<String>();
        for (String source : FRAMEWORK_LIBRARY_SOURCES) {
            sourcesRoots.add(getProjectDirectory(rootUrl, source));
        }
        for (String libName : FRAMEWORK_LIBRARY_NAMES) {
            classesRoots.add(getFrameworkJar(root.getPath(), outDir, libName));
            sourcesRoots.add(getFrameworkSource(rootUrl, outDir, libName));
        }
        updateModuleLibrary(project, classesRoots, sourcesRoots);
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