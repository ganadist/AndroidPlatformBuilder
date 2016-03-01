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

package dbgsprw.core.test

import com.android.ddmlib.*
import com.android.ddmlib.log.LogReceiver
import dbgsprw.device.DeviceAdbImpl
import dbgsprw.device.DeviceFastbootImpl
import junit.framework.TestCase

/**
 * Created by ganadist on 16. 2. 28.
 */
class DeviceTest: TestCase() {
    val mSerial = "12345678"
    fun testFastbootDevice() {
        val dev = DeviceFastbootImpl(mSerial)
        assertEquals("boot-$mSerial", dev.getDeviceName())
        assertTrue(dev.canReboot())
        assertTrue(dev.canWrite())
    }

    fun testAdbDevice() {
        val mockDevice = object: com.android.ddmlib.IDevice {
            override fun executeShellCommand(p0: String?, p1: IShellOutputReceiver?, p2: Int) {
                throw UnsupportedOperationException()
            }

            override fun getName(): String? {
                throw UnsupportedOperationException()
            }

            override fun isOffline(): Boolean {
                throw UnsupportedOperationException()
            }

            override fun pushFile(p0: String?, p1: String?) {
                throw UnsupportedOperationException()
            }

            override fun removeRemotePackage(p0: String?) {
                throw UnsupportedOperationException()
            }

            override fun reboot(p0: String?) {
                assertEquals(p0, "bootloader")
            }

            override fun getClient(p0: String?): Client? {
                throw UnsupportedOperationException()
            }

            override fun getMountPoint(p0: String?): String? {
                throw UnsupportedOperationException()
            }

            override fun executeShellCommand(p0: String?, p1: IShellOutputReceiver?) {
                throw UnsupportedOperationException()
            }

            override fun hasClients(): Boolean {
                throw UnsupportedOperationException()
            }

            override fun getPropertySync(p0: String?): String? {
                throw UnsupportedOperationException()
            }

            override fun getProperties(): MutableMap<String, String>? {
                throw UnsupportedOperationException()
            }

            override fun getProperty(p0: String?): String? {
                throw UnsupportedOperationException()
            }

            override fun getClients(): Array<out Client>? {
                throw UnsupportedOperationException()
            }

            override fun runLogService(p0: String?, p1: LogReceiver?) {
                throw UnsupportedOperationException()
            }

            override fun getAvdName(): String? {
                throw UnsupportedOperationException()
            }

            override fun installRemotePackage(p0: String?, p1: Boolean, vararg p2: String?): String? {
                throw UnsupportedOperationException()
            }

            override fun getClientName(p0: Int): String? {
                throw UnsupportedOperationException()
            }

            override fun isOnline(): Boolean {
                throw UnsupportedOperationException()
            }

            override fun runEventLogService(p0: LogReceiver?) {
                throw UnsupportedOperationException()
            }

            override fun getState(): IDevice.DeviceState? {
                throw UnsupportedOperationException()
            }

            override fun getPropertyCacheOrSync(p0: String?): String? {
                throw UnsupportedOperationException()
            }

            override fun isBootLoader(): Boolean {
                throw UnsupportedOperationException()
            }

            override fun pullFile(p0: String?, p1: String?) {
                throw UnsupportedOperationException()
            }

            override fun isEmulator(): Boolean {
                return false
            }

            override fun uninstallPackage(p0: String?): String? {
                throw UnsupportedOperationException()
            }

            override fun getSyncService(): SyncService? {
                throw UnsupportedOperationException()
            }

            override fun installPackage(p0: String?, p1: Boolean, vararg p2: String?): String? {
                throw UnsupportedOperationException()
            }

            override fun getFileListingService(): FileListingService? {
                throw UnsupportedOperationException()
            }

            override fun syncPackageToDevice(p0: String?): String? {
                throw UnsupportedOperationException()
            }

            override fun removeForward(p0: Int, p1: Int) {
                throw UnsupportedOperationException()
            }

            override fun removeForward(p0: Int, p1: String?, p2: IDevice.DeviceUnixSocketNamespace?) {
                throw UnsupportedOperationException()
            }

            override fun arePropertiesSet(): Boolean {
                throw UnsupportedOperationException()
            }

            override fun createForward(p0: Int, p1: Int) {
                throw UnsupportedOperationException()
            }

            override fun createForward(p0: Int, p1: String?, p2: IDevice.DeviceUnixSocketNamespace?) {
                throw UnsupportedOperationException()
            }

            override fun getSerialNumber(): String? {
                return mSerial
            }

            override fun getScreenshot(): RawImage? {
                throw UnsupportedOperationException()
            }

            override fun getBatteryLevel(): Int? {
                throw UnsupportedOperationException()
            }

            override fun getBatteryLevel(p0: Long): Int? {
                throw UnsupportedOperationException()
            }

            override fun getPropertyCount(): Int {
                throw UnsupportedOperationException()
            }

        }
        val dev = DeviceAdbImpl(mockDevice)
        assertEquals("adb-$mSerial", dev.getDeviceName())
        assertTrue(dev.canReboot())
        assertTrue(dev.canWrite())
    }
}