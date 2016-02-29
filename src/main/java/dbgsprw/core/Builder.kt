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

package dbgsprw.core

import com.intellij.util.io.SafeFileOutputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.*

/**
 * Created by ganadist on 16. 2. 25.
 */
class Builder : CommandExecutor() {
    private var mTargetProduct = ""
    private var mBuildVariant = ""
    private var mOutDir = ""
    private var mTarget = ""
    private var mTargetSdk = false
    private var mOneShotMakefile: String? = null
    private val CM_PRODUCT_PREFIX = "cm_"
    private var mLunchProcess: Process? = null

    init {
        setenv("USE_CCACHE", "1")
    }

    fun setAndroidJavaHome(home: String) {
        setenv("ANDROID_JAVA_HOME", home)
        val path = getenv("PATH")
        val jdkBinPath = arrayOf(home, "bin").joinToString(File.separator)
        if (!path.startsWith(jdkBinPath)) {
            setenv("PATH", jdkBinPath + File.pathSeparator + path)
        }
    }

    fun setTargetProduct(product: String) {
        mTargetProduct = product
        updateOutDir()
    }

    fun setBuildVariant(variant: String) {
        mBuildVariant = variant
        updateOutDir()
    }

    fun setTarget(target: String) {
        mTarget = target
        mOneShotMakefile = null
        mTargetSdk = mTarget.contains("sdk")
        updateOutDir()
    }

    fun setOneShotDirectory(directory: String) {
        mTarget = "all_modules"
        mOneShotMakefile = directory + File.separator + Utils.ANDROID_MK
    }

    private fun updateOutDir() {
        if (mTargetProduct.isNullOrBlank() || mBuildVariant.isNullOrBlank()) {
            return
        }
        var outDir = arrayOf("out", mTargetProduct, mBuildVariant).joinToString("-")
        if (mTargetSdk) {
            outDir = arrayOf(outDir, "sdk").joinToString("-")
        }
        if (outDir != mOutDir) {
            mOutPathListener.onOutDirChanged(outDir)
            mOutDir = outDir
            setenv("OUT_DIR", outDir)
            generateBuildSpec()
            if (mLunchProcess != null) {
                mLunchProcess!!.destroy()
            }
            mLunchProcess = findProductOutPath()
        }
    }

    private fun generateBuildSpec() {
        val FIRST_LINE = "# generated from AndroidBuilder\n"
        val sb = StringBuilder(FIRST_LINE)
        sb.append("TARGET_PRODUCT?=$mTargetProduct\n")
        sb.append("TARGET_BUILD_VARIANT?=$mBuildVariant\n")
        sb.append("OUT_DIR?=$mOutDir\n")
        if (mTargetProduct.startsWith(CM_PRODUCT_PREFIX)) {
            val cmBuild = mTargetProduct.substring(CM_PRODUCT_PREFIX.length)
            sb.append("CM_BUILD?=$cmBuild\n")
        }

        val specFileName = "buildspec.mk.AndroidBuilder"
        val buildSpec = File(directory(), "buildspec.mk")
        if (!buildSpec.exists() && buildSpec.createNewFile()) {
            val bos = BufferedOutputStream(SafeFileOutputStream(buildSpec)).bufferedWriter()
            bos.write(FIRST_LINE)
            bos.newLine()
            bos.write("# If you don't want to associate AndroidBuilder anymore,\n")
            bos.write("# delete following line.\n")
            bos.write("-include $specFileName\n")
            bos.close()
        }

        val buildSpecForBuilder = File(directory(), specFileName)
        buildSpecForBuilder.delete()
        if (buildSpecForBuilder.createNewFile()) {
            val bos = BufferedOutputStream(SafeFileOutputStream(buildSpecForBuilder)).bufferedWriter()
            bos.write(sb.toString())
            bos.close()
        }
    }

    fun buildMakeCommand(jobs: Int, verbose: Boolean, extras: String?): List<String> {
        val command: MutableList<String> = mutableListOf()
        command.add("make")
        if (jobs > 1) {
            command.add("-j$jobs")
        }
        command.add("TARGET_PRODUCT=$mTargetProduct")
        if (mTargetProduct.startsWith(CM_PRODUCT_PREFIX)) {
            val cmBuild = mTargetProduct.substring(CM_PRODUCT_PREFIX.length)
            command.add("CM_BUILD=$cmBuild")
            command.add("BUILD_WITH_COLORS=0") // turn off color
            command.add("CLANG_CONFIG_EXTRA_CFLAGS=-fno-color-diagnostics")
            command.add("CLANG_CONFIG_EXTRA_CPPFLAGS=-fno-color-diagnostics")
        }
        command.add("TARGET_BUILD_VARIANT=$mBuildVariant");
        if (!mOneShotMakefile.isNullOrEmpty()) {
            command.add("ONE_SHOT_MAKEFILE=$mOneShotMakefile")
        }
        command.add(mTarget)
        if (verbose) {
            command.add("showcommands")
        }
        if (!extras.isNullOrBlank()) {
            command.addAll(extras!!.split("\\s+"))
        }

        return command
    }

    fun runCombo(listener: ComboMenuListener): Process {

        val command = arrayOf("source build/envsetup.sh > /dev/null",
                "printf '%s\n' \${LUNCH_MENU_CHOICES[@]} | cut -f 1 -d - | sort -u").asList()

        return run(command, object : CommandHandler {
            override fun onOut(line: String) {
                listener.onTargetAdded(line)
            }

            override fun onExit(code: Int) {
                listener.onCompleted()
            }
        }, true)
    }

    private fun findProductOutPath(): Process {
        val selectedTarget = mTargetProduct + '-' + mBuildVariant
        Utils.log("builder", "target = $selectedTarget out_dir = $mOutDir")
        val command = arrayOf("source build/envsetup.sh > /dev/null",
                "lunch $selectedTarget > /dev/null",
                "echo \$ANDROID_PRODUCT_OUT").asList()

        return run(command, object : CommandHandler {
            override fun onOut(line: String) {
                var path = line
                Utils.log("builder", "ANDROID_PRODUCT_OUT set " + path)
                setenv("ANDROID_PRODUCT_OUT", path);
                if (!path.startsWith(File.separator)) {
                    path = directory() + File.separator + path
                }
                mOutPathListener.onAndroidProductOutChanged(path)
            }
        }, true)
    }

    private var mOutPathListener: OutPathListener = object : OutPathListener {}

    fun setOutPathListener(listener: OutPathListener) {
        mOutPathListener = listener
    }

    interface OutPathListener {
        fun onOutDirChanged(path: String) {
        }

        fun onAndroidProductOutChanged(path: String) {
        }
    }

    interface ComboMenuListener {
        fun onTargetAdded(target: String)
        fun onCompleted()
    }
}
