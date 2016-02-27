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

import com.android.ddmlib.IDevice

/**
 * Created by ganadist on 16. 2. 27.
 */

val processBuilder = ProcessBuilder();

enum class DeviceType {
    FASTBOOT, ADB
}

interface Device {
    fun reboot()
    fun canReboot(): Boolean
    fun canWrite(): Boolean
    fun write(partition: String, filename: String = "", wipe: Boolean = false): List<String>
    fun getType(): DeviceType
    fun getDeviceName(): String
}

class DeviceAdbImpl(val mDevice: IDevice) : IDevice by mDevice, Device {
    override fun getDeviceName(): String {
        return "adb-${mDevice.serialNumber}"
    }

    override fun canWrite(): Boolean {
        return true;
    }

    override fun reboot() {
        this.reboot("bootloader")
    }

    override fun canReboot(): Boolean {
        if (this.isEmulator()) {
            return false
        }
        return true
    }

    override fun write(partition: String, filename: String, wipe: Boolean): List<String> {
        val cmd : MutableList<String> = mutableListOf()
        cmd.add("$mAdbPath -s ${mDevice.serialNumber} root")
        cmd.add("$mAdbPath -s ${mDevice.serialNumber} wait-for-device")
        cmd.add("$mAdbPath -s ${mDevice.serialNumber} remount")
        cmd.add("$mAdbPath -s ${mDevice.serialNumber} sync $partition")
        return cmd
    }

    override fun getType(): DeviceType {
        return DeviceType.ADB
    }
}

class DeviceFastbootImpl(val mSerial: String) : Device {
    override fun getDeviceName(): String {
        return "boot-$mSerial"
    }

    override fun canWrite(): Boolean {
        return true;
    }

    override fun canReboot(): Boolean {
        return true
    }

    override fun write(partition: String, filename: String, wipe: Boolean): List<String> {
        val cmd : MutableList<String> = mutableListOf()
        val wipeOption = if (wipe) "-w" else ""
        val command = if (partition.isEmpty()) "update" else "flash"
        cmd.add("$mFastbootPath -s $mSerial $wipeOption $command $partition $filename")
        return cmd
    }

    override fun getType(): DeviceType {
        return DeviceType.FASTBOOT
    }

    override fun reboot() {
        Thread({
            val process = processBuilder.command(mFastbootPath, "-s", mSerial, "reboot").start();
            process.waitFor()
        }).start();
    }
}