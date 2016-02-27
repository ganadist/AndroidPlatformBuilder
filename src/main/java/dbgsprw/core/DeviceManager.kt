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

import com.android.ddmlib.AndroidDebugBridge
import com.android.ddmlib.IDevice
import com.intellij.openapi.projectRoots.ProjectJdkTable
import java.io.File

/**
 * Created by ganadist on 16. 2. 27.
 */

public var mAdbPath: String = ""
public var mFastbootPath: String = ""

class DeviceManager : AndroidDebugBridge.IDeviceChangeListener,
        FastbootMonitor.FastbootDeviceStateListener {
    val mDeviceMap: MutableMap<String, Device> = mutableMapOf()
    var mInited = false

    private fun getAdbLocation(androidHome: String?): File {
        var paths = arrayOf(androidHome, "platform-tools", "adb")
        return File(paths.joinToString(File.separator))
    }

    private fun findAndroidSdkHome(): String? {
        var envSdkHome = System.getenv("ANDROID_HOME")
        if (getAdbLocation(envSdkHome).exists()) {
            return envSdkHome
        }

        for (sdk in ProjectJdkTable.getInstance().allJdks.filter {
            it.sdkType.name == "Android SDK" && getAdbLocation(it.homePath).exists()
        }) {
            return sdk.homePath
        }
        Utils.log("Device", "there is no android sdk")
        return null
    }

    fun adbInit(): Boolean {
        if (mInited) {
            return true;
        }
        var sdkHome = findAndroidSdkHome()
        if (sdkHome.isNullOrBlank()) {
            return false;
        }

        AndroidDebugBridge.initIfNeeded(false)
        val platformToolHome = File(sdkHome, "platform-tools")
        mAdbPath = File(platformToolHome, "adb").getCanonicalPath()
        mFastbootPath = File(platformToolHome, "fastboot").getCanonicalPath()
        AndroidDebugBridge.createBridge(mAdbPath, true)
        AndroidDebugBridge.addDeviceChangeListener(this)
        FastbootMonitor(mFastbootPath).setDeviceStateListener(this)
        mInited = true
        return mInited;
    }

    override fun deviceChanged(device: IDevice?, state: Int) {

    }

    override fun deviceConnected(device: IDevice?) {
        Utils.runOnUi { deviceAdded(device!!.serialNumber, DeviceAdbImpl(device)) }
    }

    override fun deviceDisconnected(device: IDevice?) {
        Utils.runOnUi { deviceRemoved(device!!.serialNumber) }
    }

    override fun onFastbootDeviceRemoved(serial: String) {
        Utils.runOnUi { deviceRemoved(serial) }
    }

    override fun onFastbootDeviceAdded(serial: String) {
        Utils.runOnUi { deviceAdded(serial, DeviceFastbootImpl(serial)) }
    }

    val mListeners: MutableList<DeviceStateListener> = mutableListOf()

    fun addDeviceStateListener(listener: DeviceStateListener) {
        for (device in mDeviceMap.values) {
            listener.onDeviceAdded(device)
        }
        mListeners.add(listener)
    }

    fun removeDeviceStateListener(listener: DeviceStateListener) {
        mListeners.remove(listener)
    }

    private fun deviceAdded(serial: String, device: Device) {
        Utils.log("DeviceManager", "device is added: $serial")
        mDeviceMap.put(serial, device)
        for (listener in mListeners) {
            listener.onDeviceAdded(device)
        }
    }

    private fun deviceRemoved(serial: String) {
        Utils.log("DeviceManager", "device is removed: $serial")
        var device = mDeviceMap.remove(serial)!!
        for (listener in mListeners) {
            listener.onDeviceRemoved(device)
        }
    }
}

interface DeviceStateListener {
    fun onDeviceAdded(device: Device)
    fun onDeviceRemoved(device: Device)
}