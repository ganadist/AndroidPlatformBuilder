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

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.*
import com.intellij.openapi.roots.impl.libraries.LibraryEx
import com.intellij.openapi.util.Computable
import com.intellij.util.Consumer
import dbgsprw.core.Utils
import dbgsprw.device.DeviceManager
import java.io.File


/**
 * Created by ganadist on 16. 2. 29.
 */
class ProjectManagerService(val mProject: Project) : ProjectComponent {
    val TAG = "ProjectManager"
    var mRootUrl = ""
    var mRootUrlForJar = ""
    val EMPTY_LIST: List<String> = listOf()
    var mToolbar: BuildToolbar? = null

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

    private fun updateExcludeFoldersFirst() {
        Utils.log(TAG, "exclude dirs first")
        val module = getAndroidModule()
        val root = module!!.getModuleFile()!!.parent

        val excludedDirs = EXCLUDE_FOLDER_INITIAL.map { it -> "$mRootUrl/$it" }
        val unExcludedDirs = EMPTY_LIST

        ModuleRootModificationUtil.updateExcludedFolders(module, root, unExcludedDirs, excludedDirs)
    }

    private fun getFrameworkJar(outDir: String, libName: String): String {
        return "$mRootUrlForJar/$outDir/$TARGET_COMMON_LIBPATH/${libName}_intermediates/classes-full-debug.jar!/"
    }

    private fun getFrameworkSource(outDir: String, libName: String): String {
        return "$mRootUrl/$outDir/$TARGET_COMMON_LIBPATH/${libName}_intermediates/src/"
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

    private fun updateModuleLibrary(classesRoots: List<String>,
                                    sourcesRoots: List<String>) {

        val module = getAndroidModule()
        updateModel(module!!, Consumer<com.intellij.openapi.roots.ModifiableRootModel> { model ->
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

    fun updateOutDir(outDir: String = "") {
        Utils.log(TAG, "exclude dirs for \"$outDir\"")
        val module = getAndroidModule()
        val root = module!!.getModuleFile()!!.parent
        val excludedDirs = File(root.path).list { f, s -> (s != outDir && s.startsWith("out")) }.map { it -> "$mRootUrl/$it" }
        val unExcludedDirs = if (outDir == "") EMPTY_LIST else listOf("$mRootUrl/$outDir")

        ModuleRootModificationUtil.updateExcludedFolders(module, root, unExcludedDirs, excludedDirs)

        val classesRoots = if (outDir == "") EMPTY_LIST else FRAMEWORK_LIBRARY_NAMES.map { it -> getFrameworkJar(outDir, it) }
        var sourcesRoots: List<String> = EMPTY_LIST

        if (outDir != "") {
            val roots: MutableList<String> = mutableListOf()
            roots.addAll(FRAMEWORK_LIBRARY_SOURCES.map { it -> "$mRootUrl/$it" })
            roots.addAll(FRAMEWORK_LIBRARY_NAMES.map { it -> getFrameworkSource(outDir, it) })
            sourcesRoots = roots
        }

        updateModuleLibrary(classesRoots, sourcesRoots)
    }

    override fun getComponentName(): String {
        return "Android Builder Project Manager"
    }

    override fun disposeComponent() {
        Utils.log(TAG, "dispose")
    }

    override fun initComponent() {
        Utils.log(TAG, "init")
    }

    override fun projectClosed() {
        Utils.log(TAG, "project is closed")
        if (mToolbar != null) {
            getDeviceManager().removeDeviceStateListener(mToolbar!!)
            mToolbar = null
        }
    }

    private fun isAndroidModule(): Boolean {
        val module = getAndroidModule()
        if (module == null) {
            return false
        }
        val root = module.getModuleFile()!!.parent
        val files = arrayOf("Makefile", "build/envsetup.sh")
        return files.all { f -> File(root.path, f).exists() }
    }

    override fun projectOpened() {
        Utils.log(TAG, "project is opened")

        if (!isAndroidModule()) {
            Utils.log(TAG, "This is not android platform project.")
            // TODO
            // add module monitor
            return
        }
        val module = getAndroidModule()!!
        val root = module.getModuleFile()!!.parent
        mRootUrl = root.url
        mRootUrlForJar = "jar" + mRootUrl.substring(4)
        updateExcludeFoldersFirst()
        updateOutDir()

        mToolbar = ServiceManager.getService(mProject, BuildToolbar::class.java)
        getDeviceManager().addDeviceStateListener(mToolbar!!)
    }

    fun onOutDirChanged(outDir: String) {
        if (isAndroidModule()) {
            updateOutDir(outDir)
        }
    }

    fun getAndroidModule(): Module? {
        return ModuleManager.getInstance(mProject).findModuleByName("android")
    }

    private fun getDeviceManager(): DeviceManager {
        return ServiceManager.getService(DeviceManager::class.java)!!
    }
}