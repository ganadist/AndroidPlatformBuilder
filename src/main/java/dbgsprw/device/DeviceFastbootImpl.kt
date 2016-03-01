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

import com.intellij.openapi.components.ServiceManager

/**
 * Created by ganadist on 16. 2. 29.
 */

class DeviceFastbootImpl(val mSerial: String) : Device {
    override fun getType() = DeviceType.FASTBOOT
    override fun getDeviceName() = "boot-$mSerial"
    override fun canWrite() = true
    override fun canReboot() = true

    private fun getFastbootPath(): String {
        return ServiceManager.getService(DeviceManager::class.java).getFastbootPath()
    }

    override fun write(partition: String, filename: String, wipe: Boolean): List<String> {
        val fastbootPath = getFastbootPath()
        val wipeOption = if (wipe) "-w" else ""
        val command = if (partition.isEmpty()) "update" else "flash"
        return listOf("$fastbootPath -s $mSerial $wipeOption $command $partition $filename")
    }

    override fun reboot() {
        val fastbootPath = getFastbootPath()
        Thread({
            val process = ProcessBuilder().command(fastbootPath, "-s", mSerial, "reboot").start();
            process.waitFor()
        }).start();
    }
}