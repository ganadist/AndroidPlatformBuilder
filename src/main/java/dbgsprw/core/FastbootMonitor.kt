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

import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Created by ganadist on 16. 2. 27.
 */

class FastbootMonitor(val mFastbootPath: String) {
    var mDevices: List<String> = listOf()

    init {
        val builder = ProcessBuilder()
        Thread(
                {
                    while (true) {
                        val newDevices: MutableList<String> = mutableListOf()
                        builder.command(mFastbootPath, "devices")
                        val process = builder.start()
                        val br = BufferedReader(InputStreamReader(process.inputStream))
                        for (line in br.readLines()) {
                            newDevices.add(line.split("\t")[0])
                        }

                        val removed = mDevices.toSet().subtract(newDevices)
                        val added = newDevices.toSet().subtract(mDevices)
                        for (dev in removed) {
                            mListener.onFastbootDeviceRemoved(dev)
                        }
                        for (dev in added) {
                            mListener.onFastbootDeviceAdded(dev)
                        }
                        mDevices = newDevices

                        Thread.sleep(1000)
                    }
                }).start()
    }

    fun setDeviceStateListener(listener: FastbootDeviceStateListener) {
        mListener = listener
        for (dev in mDevices) {
            mListener.onFastbootDeviceAdded(dev)
        }
    }

    var mListener = object : FastbootDeviceStateListener {
        override fun onFastbootDeviceRemoved(serial: String) {
        }

        override fun onFastbootDeviceAdded(serial: String) {
        }
    }

    interface FastbootDeviceStateListener {
        fun onFastbootDeviceRemoved(serial: String)
        fun onFastbootDeviceAdded(serial: String)
    }
}