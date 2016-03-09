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

import com.intellij.execution.filters.OpenFileHyperlinkInfo
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.execution.ui.RunnerLayoutUi
import com.intellij.execution.ui.layout.PlaceInGrid
import com.intellij.notification.NotificationType
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.openapi.wm.ex.ToolWindowManagerEx
import com.intellij.ui.content.ContentFactory
import dbgsprw.app.BuildConsole
import dbgsprw.core.CommandExecutor
import java.awt.BorderLayout
import java.util.regex.Pattern
import javax.swing.JPanel

/**
 * Created by ganadist on 16. 3. 1.
 */

fun String?.length() = if (this == null) 0 else this.length
fun String?.toInt() = if (this == null) 0 else Integer.parseInt(this)

class BuildConsoleImpl(val mProject: Project) : BuildConsole {
    private val LOG = Logger.getInstance(BuildConsoleImpl::class.java)
    private val CONSOLE_ID = "Android Builder Console"
    private val TOOL_WINDOW_ID = "Android Build Console"

    private val mPanel = JPanel(BorderLayout())
    private val mConsoleView = TextConsoleBuilderFactory.getInstance().createBuilder(mProject).console
    private val mWindow = ToolWindowManagerEx.getInstanceEx(mProject).registerToolWindow(TOOL_WINDOW_ID, false,
            ToolWindowAnchor.BOTTOM, mProject, true)

    init {
        LOG.info("init")
        setupToolWindow()
    }

    override fun dispose() {
        LOG.info("dispose")
        ToolWindowManagerEx.getInstanceEx(mProject).unregisterToolWindow(TOOL_WINDOW_ID)
    }

    private fun setupToolWindow() {
        val factory = RunnerLayoutUi.Factory.getInstance(mProject)
        val layoutUi = factory.create("", "", "session", mProject)
        val console = layoutUi.createContent(CONSOLE_ID, mConsoleView.component,
                "", null, null)
        layoutUi.addContent(console, 0, PlaceInGrid.right, false)

        val uiComponent = layoutUi.component
        mPanel.add(uiComponent, BorderLayout.CENTER)

        val contentFactory = ContentFactory.SERVICE.getInstance()
        val content = contentFactory.createContent(uiComponent, "", false)
        mWindow.contentManager.addContent(content)
    }

    fun show(focus: Boolean) {
        mWindow.activate(null, focus)
    }

    override fun run(listener: BuildConsole.ExitListener): CommandExecutor.CommandHandler {
        LOG.info("run")
        assert(mExitListener == null)
        mExitListener = listener
        mConsoleView.clear()
        show(true)
        return this
    }

    override fun print(line: String) {
        mConsoleView.print(line, ConsoleViewContentType.NORMAL_OUTPUT)
        mConsoleView.print("\n", ConsoleViewContentType.NORMAL_OUTPUT)
    }

    override fun onOut(line: String) {
        print(line)
    }

    private val FILE_POSITION_REGEX = "(.*: )?(.+?):(\\d+):(?:(\\d+):)? (.*)$"
    private val FILE_POSITION_PATTERN = Pattern.compile(FILE_POSITION_REGEX)
    private val SEPERATOR = ": "

    override fun onError(line: String) {
        val m = FILE_POSITION_PATTERN.matcher(line)
        var file: VirtualFile? = null
        if (m.find()) {
            val messageType = m.group(1)
            val filename = m.group(2)
            val lineNo = m.group(3).toInt() - 1
            val column = m.group(4).toInt() - 1
            val message = m.group(5)
            val location = m.group(0).substring(
                    messageType.length(),
                    m.start(5) - SEPERATOR.length)

            file = VfsUtil.findRelativeFile(filename, mProject.baseDir)
            file?.apply {
                val linkInfo = OpenFileHyperlinkInfo(mProject, file!!, lineNo, column)
                if (messageType != null) {
                    mConsoleView.print(messageType, ConsoleViewContentType.ERROR_OUTPUT)
                }
                mConsoleView.printHyperlink(location, linkInfo)
                mConsoleView.print(SEPERATOR, ConsoleViewContentType.ERROR_OUTPUT)
                mConsoleView.print(message, ConsoleViewContentType.ERROR_OUTPUT)
            }
        }
        if (file == null) {
            mConsoleView.print(line, ConsoleViewContentType.ERROR_OUTPUT)
        }
        mConsoleView.print("\n", ConsoleViewContentType.ERROR_OUTPUT)
    }

    override fun onExit(code: Int) {
        LOG.info("exit with $code")
        mExitListener?.onExit()
        mExitListener = null

        if (code == 0) {
            mWindow.hide(null)
        } else if (code < 128) {
            val message = "execution is failed with exit code: $code"
            Notify.show(message, NotificationType.ERROR)
            mConsoleView.print(message, ConsoleViewContentType.SYSTEM_OUTPUT)
            mConsoleView.print("\n", ConsoleViewContentType.SYSTEM_OUTPUT)
        }
    }

    private var mExitListener: BuildConsole.ExitListener? = null

}