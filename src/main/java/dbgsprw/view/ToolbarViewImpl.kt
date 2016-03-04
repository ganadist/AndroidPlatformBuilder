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

import com.intellij.notification.NotificationType
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.openapi.wm.ex.ToolWindowManagerEx
import com.intellij.ui.content.ContentFactory
import dbgsprw.app.*
import dbgsprw.core.Utils
import dbgsprw.device.Device
import dbgsprw.device.DeviceManager
import dbgsprw.device.DeviceType
import java.io.File
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JFileChooser
import javax.swing.filechooser.FileFilter
import javax.swing.filechooser.FileNameExtensionFilter

/**
 * Created by ganadist on 16. 3. 3.
 */

enum class FlashButtonState {
    NONE, FASTBOOT, ADB, STOP, REBOOT
}

class ToolbarViewImpl(val mProject: Project) : AndroidBuilderForm(),
        BuildToolbar,
        DeviceManager.DeviceStateListener,
        BuildService.OutPathListener {
    private val LOG = Logger.getInstance(ToolbarViewImpl::class.java);
    private val TOOLBAR_WINDOW_ID = "Android Builder"
    private val mDevices: MutableMap<String, Device> = mutableMapOf()
    private val mState = StateStore.getState(mProject)

    private val CURRENT_PATH = "Current Path"
    private val mProductComboModel = HistoryComboModel(null)
    private val mVariantComboModel = HistoryComboModel(null, *Utils.sVariants)
    private val mTargetComboModel = HistoryComboModel(null, *Utils.sTargets)
    private val mTargetDirectoryComboModel = HistoryComboModel(CURRENT_PATH)
    private val mExtraComboModel = HistoryComboModel("")
    private val mFastbootComboModel = HistoryComboModel(null, *Utils.sFastbootArguments)
    private val mAdbSyncComboModel = HistoryComboModel(null, *Utils.sAdbSyncArguments)

    private val mProjectPath = mProject.basePath
    private var mProductOut = File(mProjectPath)
    private var mBuildProgress = false
    private var mSyncProgress = false

    init {
        LOG.info("init")

        val wm = ToolWindowManagerEx.getInstanceEx(mProject)
        val window = wm.registerToolWindow(TOOLBAR_WINDOW_ID, false, ToolWindowAnchor.RIGHT,
                mProject, true)
        val contentFactory = ContentFactory.SERVICE.getInstance()
        val content = contentFactory.createContent(mAndroidBuilderContent, "", false)
        window.contentManager.addContent(content)

        getBuilder().setOutPathListener(this)
        ServiceManager.getService(DeviceManager::class.java).addDeviceStateListener(this)

        initUi()
    }

    override fun dispose() {
        LOG.info("dispose")
        getBuilder().setOutPathListener(null)
        ServiceManager.getService(DeviceManager::class.java).removeDeviceStateListener(this)
        ToolWindowManagerEx.getInstanceEx(mProject).unregisterToolWindow(TOOLBAR_WINDOW_ID)
    }

    private fun initUi() {
        mProductComboBox.addActionListener { e -> selectedProduct() }
        mVariantComboBox.addActionListener { e -> selectedProduct() }
        mOpenDirectoryButton.addActionListener { e -> handleOpenDirectoryButton() }

        mMakeRadioButton.addActionListener { e -> updateMakeUi() }
        mMmRadioButton.addActionListener { e -> updateMakeUi() }
        mTargetComboBox.addActionListener { e -> selectedTarget() }
        mExtraArgumentsComboBox.addActionListener { e -> mState.mExtras = mExtraArgumentsComboBox.selectedItem as String }
        mMakeButton.addActionListener { e -> handleMakeButton() }

        mFastbootRadioButton.addActionListener { e -> updateFlashUi() }
        mAdbSyncRadioButton.addActionListener { e -> updateFlashUi() }
        mWriteArgumentComboBox.addActionListener { e -> selectedWriteComboItem() }
        mDeviceListComboBox.addActionListener { e -> updateFlashUi() }
        mFlashButton.addActionListener { e -> handleFlashButton() }

        mMakeRadioButton.isSelected = true
        mFastbootRadioButton.isSelected = true

        selectItemInCombo(mVariantComboBox, mVariantComboModel, mState.mBuildVariant)

        updateMakeUi()
        if (!mState.mExtras.isNullOrBlank()) {
            mExtraComboModel.addHistory(mState.mExtras)
        }
        selectItemInCombo(mExtraArgumentsComboBox, mExtraComboModel, mState.mExtras)

        updateFlashUi()

        mMakeButton.isEnabled = false
        getBuilder().runCombo(object : BuildService.ComboMenuListener {
            override fun onTargetAdded(target: String) {
                mProductComboModel.addElement(target);
            }

            override fun onCompleted() {
                selectItemInCombo(mProductComboBox, mProductComboModel, mState.mProduct)
                mMakeButton.isEnabled = true
            }
        })
    }

    private fun updateMakeUi() {
        val makeMode = mMakeRadioButton.isSelected
        if (makeMode) {
            mTargetComboBox.isEditable = true
            mTargetLabel.text = "Target"
            selectItemInCombo(mTargetComboBox, mTargetComboModel, mState.mTarget)

        } else {
            mTargetComboBox.isEditable = false
            mTargetLabel.text = "Target Dir"
            if (!mState.mTargetDirectory.isNullOrBlank()) {
                mTargetDirectoryComboModel.addHistory(mState.mTargetDirectory)
            }
            selectItemInCombo(mTargetComboBox, mTargetDirectoryComboModel, mState.mTargetDirectory)
        }
    }

    private fun updateFlashUi() {
        val fastbootMode = mFastbootRadioButton.isSelected
        if (fastbootMode) {
            mWriteArgumentComboBox.model = mFastbootComboModel
            selectItemInCombo(mWriteArgumentComboBox, mFastbootComboModel, mState.mFastbootTarget)
        } else {
            mWriteArgumentComboBox.model = mAdbSyncComboModel
            selectItemInCombo(mWriteArgumentComboBox, mAdbSyncComboModel, mState.mAdbSyncTarget)
        }
        mWipeCheckBox.isEnabled = fastbootMode

        val widgets: Array<JComponent> = arrayOf(mFastbootRadioButton, mAdbSyncRadioButton, mWipeCheckBox, mDeviceListComboBox, mWriteArgumentComboBox)

        val state = getFlashButtonState()
        when (state) {
            FlashButtonState.NONE -> {
                widgets.forEach { it.isEnabled = false }
                mFlashButton.isVisible = false
            }
            FlashButtonState.STOP -> {
                widgets.forEach { it.isEnabled = false }
                mFlashButton.isVisible = true
                mFlashButton.text = "Stop"
            }
            else -> {
                widgets.forEach { it.isEnabled = true }
                mFlashButton.isVisible = true
                when (state) {
                    FlashButtonState.ADB -> mFlashButton.text = "Sync"
                    FlashButtonState.FASTBOOT -> mFlashButton.text = "Flash"
                    FlashButtonState.REBOOT -> mFlashButton.text = "Reboot"
                    else -> {
                        LOG.warn("unknown fastboot state: $state")
                    }
                }
            }
        }
    }

    override fun onOutDirChanged(path: String) {
        mResultPathValueLabel.text = path
        mOpenDirectoryButton.isEnabled = false
        mFlashButton.isEnabled = false

        ServiceManager.getService(mProject, ProjectMonitor::class.java).onOutDirChanged(path)
    }

    override fun onAndroidProductOutChanged(path: String) {
        mProductOut = File(path)
        mOpenDirectoryButton.isEnabled = true
        mFlashButton.isEnabled = true
    }

    private fun handleMakeButton() {
        if (mBuildProgress) {
            getBuilder().stopBuild()
        } else {
            if (mMakeRadioButton.isSelected) {
                doMake()
            } else {
                val targetDirectory = mTargetComboBox.selectedItem as String
                if (setupPartialMake(targetDirectory)) {
                    executeMake()
                }
            }
        }
    }

    private fun setupPartialMake(directory: String): Boolean {
        var realPath: String? = directory
        if (realPath == CURRENT_PATH) {
            try {
                val currentDoc = FileEditorManager.getInstance(mProject).selectedTextEditor!!.document
                val currentPath = FileDocumentManager.getInstance().getFile(currentDoc)!!
                realPath = Utils.findAndroidMkOnParent(mProjectPath, currentPath.path)
            } catch (ex: NullPointerException) {
                Notify.show("There is no opened file on editor.", NotificationType.ERROR)
                return false;
            }
            if (realPath == null) {
                Notify.show("cannot find Android.mk", NotificationType.ERROR)
                return false;
            }

            mTargetDirectoryComboModel.addHistory(realPath)
        }
        mState.mTargetDirectory = directory
        getBuilder().setOneShotDirectory(realPath!!)
        return true
    }

    private fun executeMake() {
        FileDocumentManager.getInstance().saveAllDocuments();

        val builder = getBuilder()

        if (!builder.canBuild()) {
            Notify.show("Project is not ready or other job is processing.", NotificationType.ERROR);
            return;
        }

        val jobs = mJobSpinner.value as Int
        val verbose = mVerboseCheckBox.isSelected
        val extras = mExtraArgumentsComboBox.selectedItem as String

        builder.build(jobs, verbose, extras, object : BuildConsole.ExitListener {
            override fun onExit() {
                mBuildProgress = false
                updateMakeButton()
            }
        })
        mBuildProgress = true
        updateMakeButton()
    }

    private fun updateMakeButton() {
        if (mBuildProgress) {
            mMakeButton.text = "Stop"
        } else {
            mMakeButton.text = "Make"
        }
    }

    override fun doMake() {
        mMakeRadioButton.isSelected = true
        val builder = getBuilder()
        val target = mTargetComboBox.selectedItem as String
        builder.setTarget(target)
        executeMake()
    }

    override fun doMm() {
        mMmRadioButton.isSelected = true
        if (setupPartialMake(CURRENT_PATH)) {
            executeMake()
        }
    }

    override fun onDeviceAdded(device: Device) {
        val name = device.getDeviceName()
        LOG.info("device is added: " + name)
        mDevices[name] = device
        mDeviceListComboBox.addItem(name)
        updateFlashUi()
    }

    override fun onDeviceRemoved(device: Device) {
        val name = device.getDeviceName()
        LOG.info("device is removed: " + name)
        mDeviceListComboBox.removeItem(name)
        mDevices.remove(name)
        updateFlashUi()
    }

    private fun selectedTarget() {
        if (mMakeRadioButton.isSelected) {
            mState.mTarget = mTargetComboBox.selectedItem as String
        } else {
            mState.mTargetDirectory = mTargetComboBox.selectedItem as String
        }
    }

    private fun selectItemInCombo(combo: JComboBox<String>, model: HistoryComboModel, value: String?) {
        combo.model = model

        var index = 0
        if (!value.isNullOrBlank()) {
            index = model.getIndexOf(value)
            if (index < 0) {
                index = 0
            }
        }
        combo.selectedIndex = index
    }

    private fun selectedProduct() {
        val product = mProductComboBox.selectedItem as String?
        val variant = mVariantComboBox.selectedItem as String?
        if (product.isNullOrEmpty() || variant.isNullOrEmpty()) {
            LOG.info("product or variant is not selected")
            return
        }

        val builder = getBuilder()
        builder.setProduct(product!!, variant!!)

        mState.mProduct = product
        mState.mBuildVariant = variant
    }

    private fun handleOpenDirectoryButton() {
        val out = mProductOut
        if (!out.exists()) {
            if (!out.mkdirs()) {
                Notify.show("cannot open ANDROID_PRODUCT_OUT directory.", NotificationType.ERROR)
            }
        }
        DirectoryOpener.openDirectory(out.getPath())
    }

    private fun startSync(device: Device, partition: String, filename: String = "", wipe: Boolean = false) {
        if (!getBuilder().canSync()) {
            Notify.show("Project is not ready or other job is processing.", NotificationType.ERROR);
            return
        }

        getBuilder().sync(device, partition, filename, wipe, listener = object : BuildConsole.ExitListener {
            override fun onExit() {
                mSyncProgress = false
                updateFlashUi()
            }
        })
        mSyncProgress = true
        updateFlashUi()
    }

    private fun doAdbSync(device: Device) {
        var partition = mWriteArgumentComboBox.selectedItem as String
        if (partition == "All") {
            partition = ""
        }
        startSync(device, partition)
    }

    private val mUpdateFileFilter = FileNameExtensionFilter("update package file", "zip")
    private val mBootloaderFileFilter = FileNameExtensionFilter("bootloader file", "img")

    private fun doFastboot(device: Device) {
        var partition = mWriteArgumentComboBox.selectedItem as String
        val wipe = mWipeCheckBox.isSelected
        var filename = ""
        if (partition == "update" || partition == "bootloader") {
            val chooser = JFileChooser()

            val lastSelectedFile: File
            val msg: String
            val update = partition == "update"
            if (update) {
                lastSelectedFile = File(mState.mLastSelectedUpdatePackage)
                chooser.fileFilter = mUpdateFileFilter
                msg = "Update Package"

            } else {
                lastSelectedFile = File(mState.mLastSelectedBootloaderFilename)
                chooser.fileFilter = mBootloaderFileFilter
                msg = "Flash bootloader"
            }

            if (lastSelectedFile.canRead()) {
                chooser.selectedFile = lastSelectedFile
            } else {
                chooser.currentDirectory = if (mProductOut.exists()) mProductOut else File(mProjectPath)
            }

            chooser.fileSelectionMode = JFileChooser.FILES_ONLY
            if (chooser.showDialog(mAndroidBuilderContent, msg) == JFileChooser.APPROVE_OPTION) {
                val selected = chooser.selectedFile
                if (selected.canRead()) {
                    filename = selected.canonicalPath
                } else {
                    Notify.show("cannot read selected file: ${selected.name}", NotificationType.WARNING)
                    return
                }
            } else {
                return
            }

            if (update) {
                mState.mLastSelectedUpdatePackage = filename
            } else {
                mState.mLastSelectedBootloaderFilename = filename
            }
        }
        startSync(device, partition, filename, wipe)
    }

    private fun doReboot(device: Device) {
        device.reboot()
    }

    private fun doSyncStop(device: Device) {
        getBuilder().stopSync()
    }

    private fun handleFlashButton() {
        val state = getFlashButtonState()
        when (state) {
            FlashButtonState.NONE -> {
            }
            else -> {
                val device = getSelectedDevice()!!
                when (state) {
                    FlashButtonState.ADB -> doAdbSync(device)
                    FlashButtonState.FASTBOOT -> doFastboot(device)
                    FlashButtonState.REBOOT -> doReboot(device)
                    FlashButtonState.STOP -> doSyncStop(device)
                    else -> {
                        LOG.warn("unknown fastboot state: $state")
                    }
                }
            }
        }
    }

    private fun selectedWriteComboItem() {
        if (mAdbSyncRadioButton.isSelected) {
            mState.mAdbSyncTarget = mWriteArgumentComboBox.selectedItem as String
        } else {
            val fastbootTarget = mWriteArgumentComboBox.selectedItem as String
            mState.mFastbootTarget = fastbootTarget
        }
    }

    private fun getSelectedDevice(): Device? {
        val deviceName = mDeviceListComboBox.selectedItem as String?
        return if (deviceName.isNullOrEmpty()) null else mDevices[deviceName]
    }

    private fun getFlashButtonState(): FlashButtonState {
        val device = getSelectedDevice()
        if (device == null) {
            return FlashButtonState.NONE
        }

        if (mSyncProgress) {
            return FlashButtonState.STOP
        }

        when (device.getType()) {
            DeviceType.ADB -> {
                return if (mAdbSyncRadioButton.isSelected) FlashButtonState.ADB else FlashButtonState.REBOOT
            }
            DeviceType.FASTBOOT -> {
                return if (mFastbootRadioButton.isSelected) FlashButtonState.FASTBOOT else FlashButtonState.REBOOT
            }
        }
    }

    private fun getBuilder(): BuildService {
        return ServiceManager.getService(mProject, BuildService::class.java)
    }
}