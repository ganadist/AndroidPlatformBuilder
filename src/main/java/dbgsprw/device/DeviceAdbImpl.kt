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

package dbgsprw.device

import com.android.ddmlib.IDevice
import com.intellij.openapi.components.ServiceManager

/**
 * Created by ganadist on 16. 2. 29.
 */

class DeviceAdbImpl(val mDevice: IDevice) : Device, IDevice by mDevice {
    override fun getType() = DeviceType.ADB
    override fun getDeviceName() = "adb-${mDevice.serialNumber}"
    override fun canWrite() = true

    override fun canReboot(): Boolean {
        if (this.isEmulator()) {
            return false
        }
        return true
    }

    override fun reboot() {
        this.reboot("bootloader")
    }

    private fun getAdbPath(): String {
        return ServiceManager.getService(DeviceManager::class.java).getAdbPath()
    }

    override fun write(partition: String, filename: String, wipe: Boolean): List<String> {
        val adbPath = getAdbPath()
        return listOf(
                "$adbPath -s ${mDevice.serialNumber} root",
                "$adbPath -s ${mDevice.serialNumber} wait-for-device",
                "$adbPath -s ${mDevice.serialNumber} remount",
                "$adbPath -s ${mDevice.serialNumber} sync $partition"
        )
    }
}