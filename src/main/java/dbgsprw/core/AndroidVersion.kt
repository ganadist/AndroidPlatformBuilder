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

import java.io.File

/**
 * Created by ganadist on 16. 3. 12.
 */

fun String.join(vararg args: String): String = args.joinToString(this)

private val MAKEFILE = "Makefile"
private val ENVSETUP_SH = File.separator.join("build", "envsetup.sh")
private val VERSION_DEFAULT_MK = File.separator.join("build", "core", "version_defaults.mk")
private val ANDROID_PLATFORM_FILES = arrayOf(MAKEFILE, ENVSETUP_SH, VERSION_DEFAULT_MK)

fun isPlatformDirectory(path: String): Boolean = ANDROID_PLATFORM_FILES.all { File(path, it).canRead() }

class AndroidVersion(val mBasePath: String) {
    var mPlatformApiLevel = -1
    var mPlatformApiStr = ""
    var mPlatformVersionStr = ""
    private val BELOW_K = 20
    private val ABOVE_L = 21

    init {
        parsePlatformVersion()
    }

    private fun parsePlatformVersion() {
        for (line in File(mBasePath, VERSION_DEFAULT_MK).readLines()) {
            if (line.contains("PLATFORM_SDK_VERSION := ")) {
                mPlatformApiStr = line.split(" := ")[1]
                try {
                    mPlatformApiLevel = mPlatformApiStr.toInt()
                } catch (ex: NumberFormatException) {
                }
                continue
            }
            if (line.contains("PLATFORM_VERSION := ")) {
                mPlatformVersionStr = line.split(" := ")[1]
                continue
            }
        }
    }

    fun checkNeededJavaSdk(sdkVersion: String): Boolean {
        if (mPlatformApiLevel <= BELOW_K && sdkVersion.contains("1.6")) {
            return false
        } else if (mPlatformApiLevel >= ABOVE_L && sdkVersion.contains("1.7")) {
            return false
        }
        return true
    }
    fun hasValidVersion() = mPlatformApiLevel > 0
    fun getRequiredModuleSdkName(): String = if (mPlatformApiLevel <= BELOW_K) "Sun JDK SE 1.6" else "OpenJDK 1.7"
    fun getRequiredSdkName(): String = "Android API ${mPlatformApiLevel} Platform"
}