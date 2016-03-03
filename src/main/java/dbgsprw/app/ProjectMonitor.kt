/*
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

package dbgsprw.app

import com.intellij.ProjectTopics
import com.intellij.facet.FacetManager
import com.intellij.facet.FacetTypeRegistry
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.ModuleListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.*
import com.intellij.openapi.roots.impl.libraries.LibraryEx
import com.intellij.openapi.roots.ui.configuration.ProjectStructureConfigurable
import com.intellij.openapi.util.Computable
import com.intellij.util.Consumer
import com.intellij.util.Function
import dbgsprw.core.Utils
import dbgsprw.view.Notify
import java.io.File


/**
 * Created by ganadist on 16. 2. 29.
 */
class ProjectMonitor(val mProject: Project) : ProjectComponent, ModuleListener {
    private val LOG = Logger.getInstance(ProjectMonitor::class.java)
    private val EMPTY_LIST: List<String> = listOf()
    private var mPlatformVersion = -1
    private val mConnection = mProject.messageBus.connect()
    private var mToolbar: BuildToolbar? = null

    init {
        mConnection.subscribe(ProjectTopics.MODULES, this)
    }

    private val EXCLUDE_FOLDER_INITIAL = arrayOf(
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
            "tools")

    private val EXCLUDE_FOLDER_TEMPLATES = arrayOf(
            "eclipse",
            "host",
            "target/common/docs",
            "target/common/obj/JAVA_LIBRARIES/android_stubs_current_intermediates",
            "target/common/R",
            "target/product")

    private val FRAMEWORK_LIBRARY_SOURCES = arrayOf(
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
            "frameworks/opt/telephony/src/java")

    private val FRAMEWORK_LIBRARY_NAMES = arrayOf(
            "framework",
            "services.core",
            "services",
            "telephony-common",
            "voip-common")

    private val FRAMEWORK_LIBNAME = "framework"

    private val TARGET_COMMON_LIBPATH = "/target/common/obj/JAVA_LIBRARIES/"

    private class ModuleInfo(val mModule: Module) {
        val mRootFile = mModule.getModuleFile()!!.parent
        val mRootPath = mRootFile.path
        val mRootUrl = mRootFile.url
        val mRootUrlForJar = "jar" + mRootUrl.substring(4)
    }

    private fun updateExcludeFoldersFirst(info: ModuleInfo) {
        LOG.info("exclude dirs first")
        val excludedDirs = EXCLUDE_FOLDER_INITIAL.map { it -> "${info.mRootUrl}/$it" }
        val unExcludedDirs = EMPTY_LIST

        ModuleRootModificationUtil.updateExcludedFolders(info.mModule, info.mRootFile, unExcludedDirs, excludedDirs)
    }

    private fun getFrameworkJar(info: ModuleInfo, outDir: String, libName: String): String {
        return "${info.mRootUrlForJar}/$outDir/$TARGET_COMMON_LIBPATH/${libName}_intermediates/classes-full-debug.jar!/"
    }

    private fun getFrameworkSource(info: ModuleInfo, outDir: String, libName: String): String {
        return "${info.mRootUrl}/$outDir/$TARGET_COMMON_LIBPATH/${libName}_intermediates/src/"
    }

    private fun updateModel(module: Module, task: Consumer<ModifiableRootModel>) {
        val model = ApplicationManager.getApplication().runReadAction(Computable { ModuleRootManager.getInstance(module).modifiableModel })
        try {
            task.consume(model)
            doWriteAction(Runnable { model.commit() })
        } catch (ex: RuntimeException) {
            model.dispose()
            throw ex
        } catch (ex: Error) {
            model.dispose()
            throw ex
        }
    }

    private fun doWriteAction(action: Runnable) {
        val app = ApplicationManager.getApplication()
        app.invokeAndWait({ app.runWriteAction(action) }, app.defaultModalityState)
    }

    private fun updateModuleLibrary(module: Module, classesRoots: List<String>,
                                    sourcesRoots: List<String>) {
        updateModel(module, Consumer<com.intellij.openapi.roots.ModifiableRootModel> { model ->
            val table = model.moduleLibraryTable
            table.libraries.forEach { table.removeLibrary(it) }

            val library = table.createLibrary(FRAMEWORK_LIBNAME) as LibraryEx
            val libraryModel = library.modifiableModel
            classesRoots.forEach { libraryModel.addRoot(it, OrderRootType.CLASSES) }
            sourcesRoots.forEach { libraryModel.addRoot(it, OrderRootType.SOURCES) }

            val entry = model.findLibraryOrderEntry(library) ?: error(library)
            entry.scope = DependencyScope.COMPILE
            entry.isExported = false

            doWriteAction(Runnable { libraryModel.commit() })
        })
    }

    fun updateOutDir(module: Module, outDir: String = "") {
        LOG.info("exclude dirs for \"$outDir\"")

        val info: ModuleInfo = ModuleInfo(module)
        val excludedDirs = File(info.mRootPath).list { f, s -> (s != outDir && s.startsWith("out")) }.map { it -> "${info.mRootUrl}/$it" }
        val unExcludedDirs = if (outDir == "") EMPTY_LIST else listOf("${info.mRootUrl}/$outDir")

        ModuleRootModificationUtil.updateExcludedFolders(info.mModule, info.mRootFile, unExcludedDirs, excludedDirs)

        val classesRoots = if (outDir == "") EMPTY_LIST else FRAMEWORK_LIBRARY_NAMES.map { it -> getFrameworkJar(info, outDir, it) }
        var sourcesRoots: List<String> = EMPTY_LIST

        if (outDir != "") {
            val roots: MutableList<String> = mutableListOf()
            roots.addAll(FRAMEWORK_LIBRARY_SOURCES.map { it -> "${info.mRootUrl}/$it" })
            roots.addAll(FRAMEWORK_LIBRARY_NAMES.map { it -> getFrameworkSource(info, outDir, it) })
            sourcesRoots = roots
        }

        updateModuleLibrary(module, classesRoots, sourcesRoots)
    }

    override fun getComponentName(): String {
        return "Android Builder Module Monitor"
    }

    override fun initComponent() {
        LOG.info("init")
    }

    override fun disposeComponent() {
        LOG.info("dispose")
        mToolbar = null
    }

    private val ANDROID_FACET_NAME = "Android"
    private val ANDROID_FACET_TYPE_NAME = "android"
    private fun updateFacet(module: Module) {
        val facetManager = FacetManager.getInstance(module)
        val hasAndroidFacet = facetManager.allFacets.any { f -> f.name == ANDROID_FACET_NAME }
        if (!hasAndroidFacet) {
            LOG.info("add dummy Android Facet")
            val model = facetManager.createModifiableModel()
            val facetType = FacetTypeRegistry.getInstance().findFacetType(ANDROID_FACET_TYPE_NAME)
            val facet = facetManager.createFacet(facetType!!, ANDROID_FACET_NAME, null)
            model.addFacet(facet)
            doWriteAction(Runnable { model.commit() })
        } else {
            LOG.info("Android Facet is configured already")
        }
    }

    private fun findPlatformVersion(info: ModuleInfo) {
        mPlatformVersion = -1
        for (line in File(info.mRootPath, Utils.VERSION_DEFAULT_MK).readLines()) {
            if (!line.contains("PLATFORM_SDK_VERSION := ")) {
                continue
            }
            val versionStr = line.split(" := ")[1]
            try {
                mPlatformVersion = versionStr.toInt()
            } catch (ex: NumberFormatException) {
            }
            break
        }
    }

    private fun showSdkSettingNotify(module: Module, javaVersion: String) {
        Notify.show("Module SDK is invalid.<br/>Please <a href=''>Set Module SDK</a> to ${javaVersion}",
                NotificationType.ERROR,
                com.intellij.notification.NotificationListener({ notification, event ->
                    notification.hideBalloon()
                    val config = ProjectStructureConfigurable.getInstance(module.project)
                    ShowSettingsUtil.getInstance().editConfigurable(module.project, config, Runnable {
                        config.selectOrderEntry(module, null)
                    })
                }));
    }

    private val BELOW_K = 20
    private val ABOVE_L = 21

    private fun checkModuleSdk(module: Module) {
        val sdk = ModuleRootManager.getInstance(module).sdk
        var requiredVersion = ""
        if (mPlatformVersion <= BELOW_K) {
            requiredVersion = "Sun JDK SE 1.6"
        } else {
            requiredVersion = "OpenJDK 1.7"
        }

        if (sdk == null) {
            showSdkSettingNotify(module, requiredVersion)
            return
        }

        val name = sdk.name
        val version = if (sdk.versionString != null) sdk.versionString!! else ""

        LOG.info("detected module sdk = \"${name}\" \"${version}\"")
        if (mPlatformVersion <= BELOW_K && version.contains("1.6")) {
            // below kitkat,
        } else if (mPlatformVersion >= ABOVE_L && version.contains("1.7")) {
            // above lollipop
        } else {
            showSdkSettingNotify(module, requiredVersion)
        }
    }

    private fun androidModuleAdded(module: Module) {
        LOG.info("android module is added")

        val info = ModuleInfo(module)
        findPlatformVersion(info)
        if (mPlatformVersion < 0) {
            LOG.warn("Cannot find platform version. This is not android platform project.")
            return
        }

        updateExcludeFoldersFirst(info)
        updateOutDir(module)
        updateFacet(module)
        checkModuleSdk(module)


        if (mToolbar != null) {
            LOG.warn("duplicated ToolWindow lifecycle")
            return
        }

        mToolbar = ServiceManager.getService(module.project, BuildToolbar::class.java)
    }

    private fun androidModuleRemoved() {
        LOG.info("android module is removed")

    }

    override fun projectClosed() {
        LOG.info("project is closed")
        mConnection.disconnect()
    }

    override fun projectOpened() {
        LOG.info("project is opened")
    }

    override fun moduleAdded(project: Project, module: Module) {
        LOG.info("module is added")
        if (isAndroidModule(module)) {
            androidModuleAdded(module)
            return
        }
        LOG.warn("This is not android platform project.")
    }

    override fun moduleRemoved(project: Project, module: Module) {
        LOG.info("module is removed")
    }

    override fun beforeModuleRemoved(project: Project, module: Module) {
        LOG.info("module before removed")
        if (isAndroidModule(module)) {
            if (mToolbar != null) {
                androidModuleRemoved()
            }
        }
    }

    override fun modulesRenamed(project: Project, modules: MutableList<Module>, func: Function<Module, String>) {
        LOG.info("module is renamed")
        val module = getAndroidModule(project)
        if (module != null) {
            androidModuleAdded(module)
        } else if (mToolbar != null) {
            androidModuleRemoved()
        }
    }

    fun onOutDirChanged(outDir: String) {
        val module = getAndroidModule(mProject)
        if (module != null) {
            updateOutDir(module, outDir)
        }
    }
}

private val ANDROID_MODULE_NAME = "android"

private fun isAndroidModule(module: Module?): Boolean {
    if (module == null) {
        return false
    }
    if (module.name != ANDROID_MODULE_NAME) {
        return false
    }
    val root = module.moduleFile!!.parent.path
    val files = arrayOf(Utils.MAKEFILE,
            Utils.ENVSETUP_SH,
            Utils.VERSION_DEFAULT_MK
    )
    return files.all { f -> File(root, f).canRead() }
}

fun getAndroidModule(project: Project): Module? {
    val module = ModuleManager.getInstance(project).findModuleByName(ANDROID_MODULE_NAME)
    return if (isAndroidModule(module)) module else null
}