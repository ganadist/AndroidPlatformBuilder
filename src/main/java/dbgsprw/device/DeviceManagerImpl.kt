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

package dbgsprw.device

import com.android.ddmlib.AndroidDebugBridge
import com.android.ddmlib.IDevice
import com.intellij.openapi.Disposable
import com.intellij.openapi.projectRoots.ProjectJdkTable
import dbgsprw.core.Utils
import java.io.File

/**
 * Created by ganadist on 16. 2. 29.
 */
class DeviceManagerImpl() : DeviceManager,
        AndroidDebugBridge.IDeviceChangeListener,
        FastbootMonitor.FastbootDeviceStateListener,
        Disposable {
    private val TAG = "DeviceManagerImpl"
    private val mListeners: MutableList<DeviceManager.DeviceStateListener> = mutableListOf()
    private val mDeviceMap: MutableMap<String, Device> = mutableMapOf()
    private var mFastbootMonitor: FastbootMonitor? = null
    private var mFastbootPath: String? = null
    private var mAdbPath: String? = null

    private var mSdkLocationMonitorRunning = false
    private var mSdkLocationMonitorThread: Thread? = null

    private var mRunning = false

    private val mSdkLocationListener = object : AndroidSdkLocation {
        override fun onSdkFound(path: String) {
            Utils.log(TAG, "Android Sdk is found on $path")
            start(path)
        }
    }

    init {
        Utils.log(TAG, "init")
        findAndroidSdkHome(mSdkLocationListener)
    }

    override fun dispose() {
        Utils.log(TAG, "dispose")
        if (mSdkLocationMonitorRunning) {
            mSdkLocationMonitorRunning = false
            //mSdkLocationMonitorThread!!.join()
        }
        if (mRunning) {
            stop()
        }
    }

    private fun deviceAdded(serial: String, device: Device) {
        Utils.log(TAG, "device is added: $serial")
        if (!mDeviceMap.containsKey(serial)) {
            mDeviceMap.put(serial, device)
            for (listener in mListeners) {
                listener.onDeviceAdded(device)
            }
        } else {
            Utils.log(TAG, "device name is duplicated. ignore.")
        }
    }

    private fun deviceRemoved(serial: String) {
        Utils.log(TAG, "device is removed: $serial")
        var device = mDeviceMap.remove(serial)
        if (device != null) {
            for (listener in mListeners) {
                listener.onDeviceRemoved(device)
            }
        } else {
            Utils.log(TAG, "no such device name. ignore.")
        }
    }

    override fun addDeviceStateListener(listener: DeviceManager.DeviceStateListener) {
        for (device in mDeviceMap.values) {
            listener.onDeviceAdded(device)
        }
        mListeners.add(listener)
    }

    override fun removeDeviceStateListener(listener: DeviceManager.DeviceStateListener) {
        mListeners.remove(listener)
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

    override fun getAdbPath(): String {
        return mAdbPath!!
    }

    override fun getFastbootPath(): String {
        return mFastbootPath!!
    }

    private fun isPlatformToolsInstalled(androidHome: String?): Boolean {
        if (androidHome == null) {
            return false
        }
        val toolPath = File(androidHome, "platform-tools")
        val tools = arrayOf("adb", "fastboot")
        return tools.all { it -> File(toolPath, it).exists() }
    }

    private interface AndroidSdkLocation {
        fun onSdkFound(path: String)
    }

    private fun findAndroidSdkHome(listener: AndroidSdkLocation) {
        var envSdkHome = System.getenv("ANDROID_HOME")
        if (isPlatformToolsInstalled(envSdkHome)) {
            listener.onSdkFound(envSdkHome)
            return
        }

        mSdkLocationMonitorRunning = true
        // FIXME
        mSdkLocationMonitorThread = Thread({
            while (true) {
                val sdk = ProjectJdkTable.getInstance().allJdks.filter {
                    it.sdkType.name == "Android SDK" && isPlatformToolsInstalled(it.homePath)
                }
                if (sdk.size != 0) {
                    Utils.runOnUi { listener.onSdkFound(sdk[0].homePath!!) }
                    mSdkLocationMonitorRunning = false
                    break
                }
                Thread.sleep(1000)
            }
        })
        mSdkLocationMonitorThread!!.start()
    }


    fun start(path: String) {
        assert(!mRunning)

        val platformToolHome = File(path, "platform-tools")
        mAdbPath = File(platformToolHome, "adb").getCanonicalPath()
        mFastbootPath = File(platformToolHome, "fastboot").getCanonicalPath()

        AndroidDebugBridge.initIfNeeded(true)
        AndroidDebugBridge.createBridge(mAdbPath, true)
        AndroidDebugBridge.addDeviceChangeListener(this)

        val fastbootMonitor = FastBootMonitorImpl(mFastbootPath!!)
        fastbootMonitor.start(this)
        mFastbootMonitor = fastbootMonitor
        mRunning = true
        Utils.log(TAG, "device manager is started")
    }

    fun stop() {
        mRunning = false
        val fastbootMonitor = mFastbootMonitor!!
        mFastbootMonitor = null
        fastbootMonitor.stop()
        AndroidDebugBridge.removeDeviceChangeListener(this)

        Utils.log(TAG, "device manager is stopped")
    }
}