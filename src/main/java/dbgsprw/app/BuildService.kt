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

package dbgsprw.app

import com.intellij.openapi.Disposable
import dbgsprw.device.Device

/**
 * Created by ganadist on 16. 3. 1.
 */
interface BuildService : Disposable {
    fun setProduct(product: String, variant: String)
    fun setTarget(target: String)
    fun setOneShotDirectory(directory: String)
    fun setOutPathListener(listener: OutPathListener?)
    fun runCombo(listener: ComboMenuListener)
    fun build(jobs: Int, verbose: Boolean, extras: String, listener: BuildConsole.ExitListener)
    fun sync(device: Device, partition: String, filename: String = "", wipe: Boolean = false, listener: BuildConsole.ExitListener)
    fun stopBuild()
    fun stopSync()
    fun canBuild(): Boolean
    fun canSync(): Boolean

    interface OutPathListener {
        fun onOutDirChanged(path: String) {
        }

        fun onAndroidProductOutChanged(path: String) {
        }
    }

    interface ComboMenuListener {
        fun onTargetAdded(target: String)
        fun onCompleted()
    }
}