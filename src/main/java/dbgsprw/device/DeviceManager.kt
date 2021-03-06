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

/**
 * Created by ganadist on 16. 2. 29.
 */
interface DeviceManager {
    fun addDeviceStateListener(listener: DeviceStateListener)
    fun removeDeviceStateListener(listener: DeviceStateListener)
    fun getAdbPath(): String
    fun getFastbootPath(): String

    interface DeviceStateListener {
        fun onDeviceAdded(device: Device)
        fun onDeviceRemoved(device: Device)
    }
}