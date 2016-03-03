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

package dbgsprw.view

import javax.swing.DefaultComboBoxModel

/**
 * Created by ganadist on 16. 3. 3.
 */
class HistoryComboModel(val mDefault: String? = null, vararg values: String) : DefaultComboBoxModel<String>() {
    private val mDefaultIndex = if (mDefault == null) 0 else 1

    init {
        if (mDefault != null) addElement(mDefault)
        values.forEach { addElement(it) }
    }

    fun addHistory(value: String) {
        if (mDefault != value) {
            removeElement(value)
            insertElementAt(value, mDefaultIndex)
        }
    }

    override fun setSelectedItem(obj: Any?) {
        val value = obj as String?
        if (value != null) {
            addHistory(value)
        }
        super.setSelectedItem(value)
    }
}