/*
 * Copyright 2016 Young Ho Cha / ganadist@gmail.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package dbgsprw.core

import dbgsprw.view.toInt
import java.io.File
import java.util.regex.Pattern

/**
 * Created by ganadist on 16. 3. 12.
 */

fun String.join(vararg args: String): String = args.joinToString(this)

private val MAKEFILE = "Makefile"
private val ENVSETUP_SH = File.separator.join("build", "envsetup.sh")
private val BUILD_ID_MK = File.separator.join("build", "core", "build_id.mk")
private val VERSION_DEFAULT_MK = File.separator.join("build", "core", "version_defaults.mk")
private val ANDROID_PLATFORM_FILES = arrayOf(MAKEFILE, ENVSETUP_SH, BUILD_ID_MK, VERSION_DEFAULT_MK)

fun isPlatformDirectory(path: String): Boolean = ANDROID_PLATFORM_FILES.all { File(path, it).canRead() }

private val KEY_VAL_REGEX = "(export){0,1}\\s*(\\S+)\\s*[:]{0,1}=\\s*(\\S+)\\s*"
private val KEY_VAL_PATTERN = Pattern.compile(KEY_VAL_REGEX)

fun parseMakefileLine(line: String): Pair<String, String> {
    val m = KEY_VAL_PATTERN.matcher(line)
    m.matches()
    return Pair(m.group(2), m.group(3))
}

enum class AndroidJdk {
    JDK_NOT_SUPPORTED,
    JDK_1_5,
    JDK_1_6,
    JDK_1_7,
    JDK_1_8,
    JDK_1_9,
}

// android api levels
private val CUPCAKE = 3
private val FROYO = 8
private val GINGERBREAD = 9
private val KITKAT_WATCH = 20
private val LOLLIPOP = 21
private val M = 23
private val N = 24

fun getAndroidJdk(apiLevel: Int): AndroidJdk {
    when (apiLevel) {
        in CUPCAKE..FROYO -> return AndroidJdk.JDK_1_5
        in GINGERBREAD..KITKAT_WATCH -> return AndroidJdk.JDK_1_6
        in LOLLIPOP..M -> return AndroidJdk.JDK_1_7
        else ->
            return if (apiLevel < CUPCAKE) AndroidJdk.JDK_NOT_SUPPORTED else AndroidJdk.JDK_1_8
    }
}

class AndroidVersion(val mBasePath: String) {
    val mPlatformApiLevel: Int
    val mPlatformApiStr: String
    val mPlatformVersionStr: String
    val mBuildId: String
    val mJdk: AndroidJdk

    init {
        var m = parseMakefile(VERSION_DEFAULT_MK)
        mPlatformApiStr = m.getOrElse("PLATFORM_SDK_VERSION", { "-1" })
        mPlatformApiLevel = mPlatformApiStr.toInt()
        mPlatformVersionStr = m.getOrElse("PLATFORM_VERSION", { "" })
        m = parseMakefile(BUILD_ID_MK)
        mBuildId = m.getOrElse("BUILD_ID", { "MASTER" })
        var apiLevel = mPlatformApiLevel
        if (apiLevel != -1 && mBuildId == "MASTER") {
            apiLevel += 1
        }
        mJdk = getAndroidJdk(apiLevel)
    }

    private fun parseMakefile(filename: String): Map<String, String> {
        val map: MutableMap<String, String> = mutableMapOf()
        for (line in File(mBasePath, filename).readLines()) {
            if (line.startsWith("#")) {
                continue
            }
            if (line.contains("=")) {
                map.plusAssign(parseMakefileLine(line))
            }
        }
        return map
    }

    fun checkNeededJavaSdk(sdkVersion: String): Boolean {
        val jdkName = if (System.getProperty("os.name") == "Linux") "openjdk" else "java"
        when (mJdk) {
            AndroidJdk.JDK_NOT_SUPPORTED -> return false
            AndroidJdk.JDK_1_5 -> return sdkVersion.contains("1.5")
            AndroidJdk.JDK_1_6 -> return sdkVersion.contains("1.6")
            AndroidJdk.JDK_1_7 -> return sdkVersion.contains("1.7") // FIXME
            AndroidJdk.JDK_1_8 -> return sdkVersion.startsWith("${jdkName} version \"1.8")
            AndroidJdk.JDK_1_9 -> return sdkVersion.startsWith("${jdkName} version \"1.9")
        }
    }

    fun getRequiredModuleSdkName(): String {
        val jdkName = if (System.getProperty("os.name") == "Linux") "OpenJDK" else "Oracle JDK"
        when (mJdk) {
            AndroidJdk.JDK_1_5 -> return "Sun JDK 1.5"
            AndroidJdk.JDK_1_6 -> return "Sun JDK SE 1.6"
            AndroidJdk.JDK_1_7 -> return "${jdkName} 1.7"
            AndroidJdk.JDK_1_8 -> return "${jdkName} 1.8"
            AndroidJdk.JDK_1_9 -> return "${jdkName} 1.9"
            else -> return "not supported"
        }
    }

    fun hasValidVersion() = mPlatformApiLevel > 0
    fun getRequiredSdkName(): String = "Android API ${mPlatformApiLevel} Platform"
}