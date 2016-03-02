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

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

/**
 * Created by ganadist on 16. 2. 29.
 */
class FastBootMonitorImpl(val mFastbootPath: String) : FastbootMonitor {
    private var mRunning = false
    private var mDevices: List<String> = listOf()
    private var mThread: Thread? = null;

    override fun start(listener: FastbootMonitor.FastbootDeviceStateListener) {
        mRunning = true

        val builder = ProcessBuilder()
        mThread = Thread(
                {
                    while (mRunning) {
                        val newDevices: MutableList<String> = mutableListOf()
                        val process = builder.command(mFastbootPath, "devices").start()

                        val br = BufferedReader(InputStreamReader(process.inputStream))
                        try {
                            br.forEachLine { newDevices.add(it.split("\t")[0]) }
                        } catch (ex: IOException) {
                        }

                        val removed = mDevices.toSet().subtract(newDevices)
                        val added = newDevices.toSet().subtract(mDevices)
                        removed.forEach { listener.onFastbootDeviceRemoved(it) }
                        added.forEach { listener.onFastbootDeviceAdded(it) }

                        mDevices = newDevices

                        Thread.sleep(1000)
                    }
                })
        mThread!!.start()
    }

    override fun stop() {
        assert(mRunning == true)
        assert(mThread != null)

        mRunning = false;
        mThread!!.join()
        mThread = null
    }
}