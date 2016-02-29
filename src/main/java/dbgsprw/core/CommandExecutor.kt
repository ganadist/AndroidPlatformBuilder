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

import java.io.*

/**
 * Created by ganadist on 16. 2. 25.
 */
open class CommandExecutor {
    private val mProcessBuilder: ProcessBuilder = ProcessBuilder()

    interface CommandHandler {
        fun onOut(line: String)
        fun onError(line: String) {
        }

        fun onExit(code: Int) {
        }
    }

    private interface OutputReader {
        fun onRead(line: String)
        fun onExit() {
        }
    }

    class NullCommandHandler : CommandHandler {
        override fun onOut(line: String) {
        }
    }

    private fun readInputStream(stream: InputStream, reader: OutputReader) {
        val br = BufferedReader(InputStreamReader(stream))
        Thread({
            try {
                br.forEachLine { Utils.runOnUi { reader.onRead(it) } }
            } catch (ex: IOException) {}
            Utils.runOnUi { reader.onExit() }
        }).start()
    }

    fun setenv(key: String, value: String) {
        mProcessBuilder.environment().put(key, value)
    }

    fun getenv(key: String): String {
        return mProcessBuilder.environment().get(key)!!
    }

    fun directory(dir: String) {
        mProcessBuilder.directory(File(dir))
    }

    fun run(commands: List<String>, handler: CommandHandler = NullCommandHandler(), shell: Boolean = false): Process {
        var command = commands
        if (shell) {
            command = arrayOf("bash", "-c", commands.joinToString("&&")).asList()
        }

        Utils.log("Command", "run = " + command.joinToString(" "))
        val process = mProcessBuilder.command(command).start()
        readInputStream(process.errorStream, object : OutputReader {
            override fun onRead(line: String) {
                handler.onError(line)
            }
        })
        readInputStream(process.inputStream, object : OutputReader {
            override fun onRead(line: String) {
                handler.onOut(line)
            }

            override fun onExit() {
                val exitCode = process.waitFor();
                Utils.log("Command", command.joinToString(" ") + " is exited with $exitCode")
                handler.onExit(process.waitFor())
            }
        })
        return process
    }
}