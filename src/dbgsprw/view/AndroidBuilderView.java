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
 */

package dbgsprw.view;

import com.android.ddmlib.IDevice;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.roots.ui.configuration.ProjectStructureConfigurable;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ex.ToolWindowManagerEx;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import dbgsprw.core.*;
import dbgsprw.exception.AndroidHomeNotFoundException;
import dbgsprw.exception.FileManagerNotFoundException;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.HyperlinkEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by ganadist on 16. 2. 23.
 */
public class AndroidBuilderView {
    private JPanel mAndroidBuilderContent;
    private JPanel mMakeOptionPanel;
    private JLabel mTargetLabel;
    private JComboBox mTargetComboBox;
    private JLabel mJobNumberLabel;
    private JLabel mMakeCommandLabel;
    private JLabel mMakeLabel;
    private JRadioButton mMmRadioButton;
    private JRadioButton mMakeRadioButton;
    private JLabel mExtraArgumentsLabel;
    private JComboBox mExtraArgumentsComboBox;
    private JTextArea mMakeCommandTextArea;
    private JLabel mTargetDirLabel;
    private JComboBox mTargetDirComboBox;
    private JCheckBox mVerboseCheckBox;
    private JButton mMakeButton;
    private JButton mMakeStopButton;
    private JSpinner mJobSpinner;
    private JPanel mFlashOptionPanel;
    private JLabel mDeviceListLabel;
    private JComboBox mDeviceListComboBox;
    private JLabel mFlashCommandLabel;
    private JLabel mModeLabel;
    private JRadioButton mAdbSyncRadioButton;
    private JRadioButton mFastbootRadioButton;
    private JTextArea mFlashCommandTextArea;
    private JLabel mAdbSyncArgumentLabel;
    private JComboBox mAdbSyncArgumentComboBox;
    private JLabel mFastBootArgumentLabel;
    private JComboBox mFastBootArgumentComboBox;
    private JCheckBox mWipeCheckBox;
    private JButton mRebootButton;
    private JButton mRebootBootloaderButton;
    private JButton mFlashButton;
    private JButton mSyncButton;
    private JButton mFlashStopButton;
    private JComboBox mProductComboBox;
    private JComboBox mVariantComboBox;
    private JLabel mProductLabel;
    private JLabel mVariantLabel;
    private JLabel mResultPathLabel;
    private JLabel mResultPathValueLabel;
    private JButton mOpenDirectoryButton;

    private final static String CURRENT_PATH = "Current Path";
    private final static String ADB_PROPERTIES_PATH = "properties/adb_sync_argument.properties";
    private final static String FASTBOOT_PROPERTIES_PATH = "properties/fastboot_argument.properties";
    private final static String TARGET_PROPERTIES_PATH = "properties/target_argument.properties";
    private final static String VARIANT_PROPERTIES_PATH = "properties/variant_argument.properties";


    private ButtonGroup mMakeButtonGroup;
    private ButtonGroup mFlashButtonGroup;

    private String mUpdateFilePath;
    private String mProjectPath;
    private Builder mBuilder;
    private Project mProject;
    private ToolWindow mToolWindow;
    private String mProductOut;
    private boolean mIsCreated;

    private static final ArgumentProperties sAdbSyncProperties;
    private static final ArgumentProperties sFastbootProperties;
    private static final ArgumentProperties sTargetProperties;
    private static final ArgumentProperties sVariantProperties;
    private DeviceManager mDeviceManager;
    private ShellCommandExecutor.ResultReceiver finalReceiver;
    private AndroidBuilderConsole mConsole;

    static {
        ArgumentPropertiesManager argumentPropertiesManager = new ArgumentPropertiesManager();
        sAdbSyncProperties = argumentPropertiesManager.loadProperties(ADB_PROPERTIES_PATH);
        sFastbootProperties = argumentPropertiesManager.loadProperties(FASTBOOT_PROPERTIES_PATH);
        sTargetProperties = argumentPropertiesManager.loadProperties(TARGET_PROPERTIES_PATH);
        sVariantProperties = argumentPropertiesManager.loadProperties(VARIANT_PROPERTIES_PATH);
    }

    AndroidBuilderView(Project project, ToolWindow toolWindow) {
        mProject = project;
        mProjectPath = mProject.getBasePath();


        mBuilder = new Builder(mProjectPath, new Builder.LunchDoneListener() {
            @Override
            public void lunchDone(ArrayList<String> mLunchMenuList) {
                HistoryComboModel history = new HistoryComboModel(mLunchMenuList);
                mProductComboBox.setPrototypeDisplayValue("XXXXXXXXX");
                mProductComboBox.setModel(history);
                mProductComboBox.setSelectedIndex(0); // set explicitly for fire action
                mProductComboBox.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent actionEvent) {
                        mBuilder.setTargetProduct(String.valueOf(mProductComboBox.getSelectedItem()));
                        updateResultPath();
                        updateCommandTextView();
                    }
                });
            }
        });

        notifySetSdk(project);

        mDeviceManager = new DeviceManager();

        mToolWindow = toolWindow;
        setupConsole(project);

        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        Content content = contentFactory.createContent(mAndroidBuilderContent, "", false);
        toolWindow.getContentManager().addContent(content);


        initMakePanelComboBoxes();
        initMakePanelButtons();
        initMakePanelRadioButtons();

        try {
            mDeviceManager.adbInit();
            mDeviceManager.fastBootMonitorInit();
        } catch (AndroidHomeNotFoundException e) {
            showNotification("Can't find Android Home. Can't use flash function.\n Please set Android SDK or Android Home", NotificationType.ERROR);
            e.printStackTrace();
            return;
        }

        initFlashPanelRadioButtons();
        initFlashPanelComboBoxes();
        initFlashButtons();
    }

    JComponent getRootComponent() {
        return mAndroidBuilderContent;
    }

    public boolean isAvailable() {
        return mIsCreated ;
    }

    public void notifySetSdk(final Project project) {
        if (ProjectRootManager.getInstance(project).getProjectSdk() == null) {
            Notifications.Bus.notify(new Notification("Android Builder", "Android Builder",
                    "<a href=''>Project SDK is not selected. Set project SDK</a>",
                    NotificationType.WARNING,
                    new NotificationListener() {
                        @Override
                        public void hyperlinkUpdate(@NotNull Notification notification, @NotNull HyperlinkEvent event) {
                            ProjectStructureConfigurable configurable = ProjectStructureConfigurable.getInstance(project);
                            configurable.selectProjectGeneralSettings(true);
                            ShowSettingsUtil.getInstance().editConfigurable(project,
                                    configurable);
                        }
                    }));
        }
    }

    private void updateResultPath() {
        mOpenDirectoryButton.setEnabled(false);
        mFlashButton.setEnabled(false);
        mSyncButton.setEnabled(false);

        mResultPathValueLabel.setText(mBuilder.getOutDir());
        finalReceiver = new ShellCommandExecutor.ResultReceiver() {
            @Override
            public void newOut(String line) {
                if(this.equals(finalReceiver)) {
                    mProductOut = line;
                    mDeviceManager.setTargetProductPath(new File(mProductOut));
                    mOpenDirectoryButton.setEnabled(true);
                    mFlashButton.setEnabled(true);
                    mSyncButton.setEnabled(true);
                }
            }

            @Override
            public void newError(String line) {

            }

            @Override
            public void onExit(int code) {
            }
        };

        mBuilder.findOriginalProductOutPath(finalReceiver);
    }

    private void updateCommandTextView() {
        mMakeCommandTextArea.setText(Utils.join(' ', mBuilder.buildMakeCommand().toArray()));
    }

    private void writeFlashCommand() {
        if (mRebootButton.isVisible()) {
            mFlashCommandTextArea.setText("fastboot -s " +
                    mDeviceListComboBox.getSelectedItem().toString().split(" ")[1] +
                    " reboot");
        } else if (mRebootBootloaderButton.isVisible()) {
            mFlashCommandTextArea.setText("adb -s " + mDeviceListComboBox.getSelectedItem().toString() +
                    " reboot");
        } else if (mFlashButton.isVisible()) {
            String arguments = "";
            if (mWipeCheckBox.isSelected()) {
                arguments = "-w ";
            }
            for (String argument :
                    fastBootArgumentComboBoxInterpreter(mFastBootArgumentComboBox.getSelectedItem().toString())) {
                arguments += argument + " ";
            }

            mFlashCommandTextArea.setText("fastboot -s " +
                    mDeviceListComboBox.getSelectedItem().toString().split(" ")[1] + " " +
                    arguments);
        } else if (mSyncButton.isVisible()) {
            if ("All".equals(mAdbSyncArgumentComboBox.getSelectedItem().toString())) {
                mFlashCommandTextArea.setText("adb -s " + mDeviceListComboBox.getSelectedItem().toString() +
                        " sync");
            } else {
                mFlashCommandTextArea.setText("adb -s " + mDeviceListComboBox.getSelectedItem().toString() +
                        " sync " +
                        mAdbSyncArgumentComboBox.getSelectedItem().toString());
            }
        } else {
            mFlashCommandTextArea.setText("");
        }
    }

    public void doMake() {
        if (mMakeStopButton.isVisible()) {
            mMakeStopButton.doClick();
        } else {
            mMakeRadioButton.doClick();
            mMakeButton.doClick();
        }
    }

    public void doMm() {
        if (mMakeStopButton.isVisible()) {
            mMakeStopButton.doClick();
        } else {
            mMmRadioButton.doClick();
            mMakeButton.doClick();
        }
    }

    private void showNotification(String message, NotificationType type) {
        Notifications.Bus.notify(new Notification("Android Builder", "Android Builder", message, type));
    }

    private void initMakePanelButtons() {

        /*
        final JFileChooser jFileChooser;
        jFileChooser = new JFileChooser();
        jFileChooser.setCurrentDirectory(new File(Utils.pathJoin(mProjectPath, OUT_DIR)));
        jFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        jFileChooser.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                File selectedDir = jFileChooser.getSelectedFile();
                if (selectedDir == null) return;
                if (!selectedDir.exists()) {
                    selectedDir.mkdirs();
                }
                if (selectedDir.canWrite()) {
                  //  mResultPathValueLabel.addItem(selectedDir);
                  //  mResultPathValueLabel.setSelectedItem(selectedDir);
                    jFileChooser.setCurrentDirectory(selectedDir);
                    if (mOutDirComboBox.getSelectedItem() == null) {
                        String path = Utils.pathJoin(selectedDir.toString(), "target", "product");
                        jFlashFileChooser.setCurrentDirectory(new File(path));
                    }
                } else {
                    Messages.showMessageDialog(mProject, "Please select writable dir", "Android Builder",
                            Messages.getInformationIcon());
                }
            }
        });*/

        mOpenDirectoryButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                File out = new File(mProductOut);
                if (!out.exists()) {
                    if (!out.mkdirs()) {
                        showNotification("cannot open ANDROID_PRODUCT_OUT directory.", NotificationType.ERROR);
                    }
                }
                try {
                    DirectoryOpener.openDirectory(mProductOut);
                } catch (FileManagerNotFoundException e) {
                    showNotification("can't find file manager command.", NotificationType.ERROR);
                }
            }
        });

        mMakeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                saveCurrentProject();

                if (mMmRadioButton.isSelected()) {
                    String selectedPath = mTargetDirComboBox.getSelectedItem().toString();
                    if (CURRENT_PATH.equals(selectedPath)) {
                        Document currentDoc;
                        VirtualFile currentPath;
                        try {
                            currentDoc = FileEditorManager.getInstance(mProject).getSelectedTextEditor().
                                    getDocument();
                            currentPath = FileDocumentManager.getInstance().getFile(currentDoc);
                            selectedPath = Utils.findAndroidMkOnParent(mProjectPath, currentPath.getPath());
                        } catch (NullPointerException e) {
                            showNotification("There is no opened file on editor.", NotificationType.ERROR);
                            return;
                        }

                        if (selectedPath == null) {
                            showNotification("cannot find Android.mk", NotificationType.ERROR);
                            return;
                        }
                        ((TargetDirHistoryComboModel) mTargetDirComboBox.getModel()).addElement(selectedPath);
                    }
                    mBuilder.setOneShotMakefile(selectedPath);
                } else {
                    mBuilder.setTarget(mTargetComboBox.getSelectedItem().toString());
                }

                Module module = AndroidBuilderFactory.getAndroidModule(mProject);
                Sdk moduleSdk = ModuleRootManager.getInstance(module).getSdk();

                if (moduleSdk != null) {
                    mBuilder.setAndroidJavaHome(moduleSdk.getHomePath());
                }

                mBuilder.executeMake(mConsole.run(
                        new AndroidBuilderConsole.ExitListener() {

                            @Override
                            public void onExit() {
                                mMakeButton.setVisible(true);
                                mMakeStopButton.setVisible(false);
                            }
                        }));

                mMakeButton.setVisible(false);
                mMakeStopButton.setVisible(true);
            }
        });
        mMakeStopButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                mBuilder.stopMake();
                mMakeButton.setVisible(true);
                mMakeStopButton.setVisible(false);
            }
        });
        mMakeStopButton.setVisible(false);
    }

    private void saveCurrentProject() {
        mProject.save();
        showNotification("project is saved for build.", NotificationType.INFORMATION);
    }

    private void initMakePanelRadioButtons() {
        mMakeButtonGroup = new ButtonGroup();
        mMakeButtonGroup.add(mMakeRadioButton);
        mMakeButtonGroup.add(mMmRadioButton);

        mMakeRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                mTargetComboBox.setVisible(true);
                mTargetLabel.setVisible(true);
                mMakeCommandLabel.setVisible(true);
                mTargetDirLabel.setVisible(false);
                mTargetDirComboBox.setVisible(false);
                updateCommandTextView();
            }
        });

        mMmRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                mTargetComboBox.setVisible(false);
                mTargetLabel.setVisible(false);
                mMakeCommandLabel.setVisible(true);
                mTargetDirLabel.setVisible(true);
                mTargetDirComboBox.setVisible(true);
                updateCommandTextView();
            }
        });

        mMakeRadioButton.doClick();

        mVerboseCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mBuilder.setVerbose(mVerboseCheckBox.isSelected());
                updateCommandTextView();
            }
        });
    }

    public void addPropertiesToComboBox(ArgumentProperties properties, JComboBox jComboBox) {
        for (String name : properties.getPropertyNames()) {
            jComboBox.addItem(name);
        }
    }

    private void initMakePanelComboBoxes() {
        HistoryComboModel history;

        addPropertiesToComboBox(sTargetProperties, mTargetComboBox);

        mTargetComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                mBuilder.setTarget(String.valueOf(mTargetComboBox.getSelectedItem()));
                updateCommandTextView();
            }
        });
        mTargetComboBox.setSelectedItem("droid");

        mTargetDirComboBox.setModel(new TargetDirHistoryComboModel(CURRENT_PATH));
        mTargetDirComboBox.setPrototypeDisplayValue("XXXXXXXXX");
        mTargetDirComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                mBuilder.setOneShotMakefile(String.valueOf(mTargetDirComboBox.getSelectedItem()));
                updateCommandTextView();
            }
        });

        mVariantComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                mBuilder.setTargetBuildVariant(String.valueOf(mVariantComboBox.getSelectedItem()));
                updateResultPath();
                updateCommandTextView();
            }
        });

        addPropertiesToComboBox(sVariantProperties, mVariantComboBox);

        history = new HistoryComboModel();
        mExtraArgumentsComboBox.setModel(history);
        mExtraArgumentsComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mBuilder.setExtraArguments(String.valueOf(mExtraArgumentsComboBox.getSelectedItem()));
                updateCommandTextView();
            }
        });

        final int numberOfCpus = Runtime.getRuntime().availableProcessors();
        final int initialJobNumber = (numberOfCpus > 4) ? numberOfCpus - 1 : numberOfCpus;

        SpinnerNumberModel model = new SpinnerNumberModel(initialJobNumber,
                1, numberOfCpus, 1);
        mJobSpinner.setModel(model);
        mBuilder.setJobNumber(initialJobNumber);
        mJobSpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                mBuilder.setJobNumber((int) mJobSpinner.getValue());
                updateCommandTextView();
            }
        });
    }

    private void initFlashPanelRadioButtons() {
        mFlashButtonGroup = new ButtonGroup();
        mFlashButtonGroup.add(mFastbootRadioButton);
        mFlashButtonGroup.add(mAdbSyncRadioButton);

        mFastbootRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                mAdbSyncArgumentComboBox.setVisible(false);
                mAdbSyncArgumentLabel.setVisible(false);
                mFastBootArgumentComboBox.setVisible(true);
                mFastBootArgumentLabel.setVisible(true);
                mWipeCheckBox.setEnabled(true);
                changeFlashButton();
                writeFlashCommand();
            }
        });

        mAdbSyncRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                mAdbSyncArgumentComboBox.setVisible(true);
                mAdbSyncArgumentLabel.setVisible(true);
                mFastBootArgumentComboBox.setVisible(false);
                mFastBootArgumentLabel.setVisible(false);

                mWipeCheckBox.setEnabled(false);
                changeFlashButton();
                writeFlashCommand();
            }
        });

        mWipeCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                writeFlashCommand();
            }
        });

        mFastbootRadioButton.doClick();
    }

    private void initFlashButtons() {


        mFlashButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (!new File(mProductOut).exists()) {
                    Messages.showMessageDialog(mProject, mProductOut + " is not exist path.\n Please make first",
                            "Android Builder",
                            Messages.getInformationIcon());
                } else if (mDeviceListComboBox.getSelectedItem() == null) {
                    Messages.showMessageDialog(mProject, "Can't find device", "Android Builder",
                            Messages.getInformationIcon());
                } else {
                    boolean wipe = mWipeCheckBox.isSelected();
                    mDeviceManager.flash(mDeviceListComboBox.getSelectedItem().toString().split(" ")[1], wipe,
                            fastBootArgumentComboBoxInterpreter(mFastBootArgumentComboBox.getSelectedItem()
                                    .toString()),
                                    mConsole.run( new AndroidBuilderConsole.ExitListener() {

                                @Override
                                public void onExit() {
                                    boolean isFastBootRadioButtonClicked = mFastbootRadioButton.isSelected();
                                    mFlashButton.setVisible(isFastBootRadioButtonClicked);
                                    mSyncButton.setVisible(!isFastBootRadioButtonClicked);
                                    mFlashStopButton.setVisible(false);
                                }
                            }));
                    mFlashButton.setVisible(false);
                    mSyncButton.setVisible(false);
                    mFlashStopButton.setVisible(true);
                }
            }
        });

        mSyncButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (!new File(mProductOut).exists()) {
                    Messages.showMessageDialog(mProject, mProductOut + " is not exist path.\n Please make first",
                            "Android Builder",
                            Messages.getInformationIcon());
                } else if (mDeviceListComboBox.getSelectedItem() == null) {
                    Messages.showMessageDialog(mProject, "Can't find device", "Android Builder",
                            Messages.getInformationIcon());
                } else {
                    String argument = mAdbSyncArgumentComboBox.getSelectedItem().toString();
                    if ("All".equals(argument)) {
                        argument = null;
                    }
                    IDevice iDevice = (IDevice) mDeviceListComboBox.getSelectedItem();

                    mDeviceManager.adbSync(iDevice, argument,mConsole.run( new AndroidBuilderConsole.ExitListener() {
                        @Override
                        public void onExit() {
                            boolean isFastBootRadioButtonClicked = mFastbootRadioButton.isSelected();
                            mFlashButton.setVisible(isFastBootRadioButtonClicked);
                            mSyncButton.setVisible(!isFastBootRadioButtonClicked);
                            mFlashStopButton.setVisible(false);
                        }
                    }));

                    mFlashButton.setVisible(false);
                    mSyncButton.setVisible(false);
                    mFlashStopButton.setVisible(true);
                }
            }
        });

        mRebootBootloaderButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                mDeviceManager.rebootDeviceBootloader((IDevice) mDeviceListComboBox.getSelectedItem());
              /*  for (String fastBootDevice : mDeviceManager.getFastBootDevices()) {
                    mDeviceListComboBox.addItem("fastboot " + fastBootDevice);
                }*/
            }
        });

        mRebootButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                mDeviceManager.rebootDevice(mDeviceListComboBox.getSelectedItem().toString().split(" ")[1]);
            }
        });

        mFlashStopButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                mDeviceManager.stopFlash();
                mDeviceManager.stopAdbSync();
                boolean isFastBootRadioButtonClicked = mFastbootRadioButton.isSelected();
                mFlashButton.setVisible(isFastBootRadioButtonClicked);
                mSyncButton.setVisible(!isFastBootRadioButtonClicked);

                mFlashStopButton.setVisible(false);
            }
        });
        mFlashStopButton.setVisible(false);

    }

    private void initFlashPanelComboBoxes() {
        mDeviceListComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                changeFlashButton();
                writeFlashCommand();

            }
        });
        mDeviceManager.addDeviceChangeListener(new FastBootMonitor.DeviceChangeListener() {
            @Override
            public void deviceConnected(final IDevice device) {
                Utils.runOnUi(new Runnable() {
                    @Override
                    public void run() {
                        mDeviceListComboBox.addItem(device);
                        mConsole.print("device connected : " + device);
                    }
                });
            }

            @Override
            public void deviceDisconnected(final IDevice device) {
                Utils.runOnUi(new Runnable() {
                    @Override
                    public void run() {
                        mDeviceListComboBox.removeItem(device);
                        mConsole.print("device disconnected : " + device);
                    }
                });
            }

            @Override
            public void deviceChanged(IDevice device, int changeMask) {
            }

            @Override
            public void fastBootDeviceConnected(final String serialNumber) {
                Utils.runOnUi(new Runnable() {
                    @Override
                    public void run() {
                        mDeviceListComboBox.addItem("fastboot " + serialNumber);
                        mConsole.print("fastboot device connected : " + serialNumber);
                    }
                });
            }

            @Override
            public void fastBootDeviceDisconnected(final String serialNumber) {
                Utils.runOnUi(new Runnable() {
                    @Override
                    public void run() {
                        mDeviceListComboBox.removeItem("fastboot " + serialNumber);
                        mConsole.print("fastboot device disconnected : " + serialNumber);
                    }
                });
            }
        });


        addPropertiesToComboBox(sFastbootProperties, mFastBootArgumentComboBox);
        mFastBootArgumentComboBox.setSelectedItem("flashall");


        mFastBootArgumentComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (mFastBootArgumentComboBox.getSelectedItem().toString().equals("update")) {
                    JFileChooser jFileChooser = new JFileChooser();
                    File productOutDirectory = new File(mProductOut);
                    if (productOutDirectory.exists()) {
                        jFileChooser.setCurrentDirectory(productOutDirectory);
                    } else {
                        jFileChooser.setCurrentDirectory(new File(mProjectPath));
                    }

                    if (jFileChooser.showDialog(mAndroidBuilderContent, "Choose Update Package File") ==
                            JFileChooser.APPROVE_OPTION) {
                        File selectedFile = jFileChooser.getSelectedFile();
                        if (selectedFile.exists()) {
                            try {
                                mUpdateFilePath = selectedFile.getCanonicalPath();
                                sFastbootProperties.setProperty("update", "update\t" + mUpdateFilePath);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                    }
                }
                writeFlashCommand();
            }
        });

        addPropertiesToComboBox(sAdbSyncProperties, mAdbSyncArgumentComboBox);

        mAdbSyncArgumentComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                writeFlashCommand();
            }
        });
    }

    private String[] fastBootArgumentComboBoxInterpreter(String argument) {
        return sFastbootProperties.getArguments(argument, "\t");
    }

    private void changeFlashButton() {
        if (mDeviceListComboBox.getSelectedItem() == null) {
            mRebootButton.setVisible(false);
            mSyncButton.setVisible(false);
            mRebootBootloaderButton.setVisible(false);
            mFlashButton.setVisible(false);
        } else if (mFastbootRadioButton.isSelected()) {
            mRebootButton.setVisible(false);
            mSyncButton.setVisible(false);
            if (mDeviceListComboBox.getSelectedItem().toString().contains("fastboot")) {
                mRebootBootloaderButton.setVisible(false);
                mFlashButton.setVisible(true);
            } else {
                mRebootBootloaderButton.setVisible(true);
                mFlashButton.setVisible(false);
            }
        } else {
            mRebootBootloaderButton.setVisible(false);
            mFlashButton.setVisible(false);
            if (mDeviceListComboBox.getSelectedItem().toString().contains("fastboot")) {
                mRebootButton.setVisible(true);
                mSyncButton.setVisible(false);
            } else {
                mRebootButton.setVisible(false);
                mSyncButton.setVisible(true);
            }
        }
    }

    private void setupConsole(Project project) {
        mConsole = new AndroidBuilderConsole(project);
    }

}
