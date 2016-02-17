package dbgsprw.view;

import com.android.ddmlib.IDevice;
import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.ide.actions.ShowSettingsAction;
import com.intellij.ide.actions.TemplateProjectSettingsGroup;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.module.ModuleConfigurationEditor;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.options.ex.ConfigurablesGroupBase;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.impl.SdkConfigurationUtil;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.roots.ui.configuration.ModuleConfigurationEditorProvider;
import com.intellij.openapi.roots.ui.configuration.ModuleConfigurationEditorProviderEx;
import com.intellij.openapi.roots.ui.configuration.ProjectJdkConfigurable;
import com.intellij.openapi.roots.ui.configuration.ProjectStructureConfigurable;
import com.intellij.openapi.roots.ui.configuration.projectRoot.BaseStructureConfigurable;
import com.intellij.openapi.roots.ui.configuration.projectRoot.FacetStructureConfigurable;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import dbgsprw.core.Builder;
import dbgsprw.core.DeviceManager;
import dbgsprw.core.FastBootMonitor;
import dbgsprw.core.ShellCommandExecutor;
import dbgsprw.exception.AndroidHomeNotFoundException;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;

/**
 * Copyright 2016 dbgsprw / dbgsprw@gmail.com
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

public class AndroidBuilderFactory implements ToolWindowFactory {
    private final static String CURRENT_PATH = "Current Path";
    private final static String ANDROID_MK = "Android.mk";
    private final static String OUT_DIR = "out";
    private static AndroidBuilderFactory mAndroidBuilderFactory;
    private JPanel mAndroidBuilderContent;
    private JButton mMakeButton;
    private JComboBox mTargetComboBox;
    private JComboBox mProductComboBox;
    private JComboBox mJobNumberComboBox;
    private JComboBox mResultPathComboBox;
    private JButton mChooseResultPathButton;
    private JLabel mTargetLabel;
    private ButtonGroup mMakeButtonGroup;
    private JRadioButton mMakeRadioButton;
    private JRadioButton mMmRadioButton;
    private JTextArea mMakeCommandTextArea;
    private JLabel mMakeStatusLabel;
    private JButton mMakeStopButton;
    private JLabel mMakeCommandLabel;
    private JComboBox mTargetDirComboBox;
    private JLabel mTargetDirLabel;
    private ButtonGroup mFlashButtonGroup;
    private JButton mSyncButton;
    private JButton mFlashButton;
    private JRadioButton mFastbootRadioButton;
    private JRadioButton mAdbSyncRadioButton;
    private FilteredTextArea mFilteredLogArea;
    private JPanel mMakeOptionPanel;
    private JLabel mProductLabel;
    private JLabel mJobNumberLabel;
    private JLabel mResultPathLabel;
    private JLabel mMakeLabel;
    private JPanel mFlashOptionPanel;
    private JLabel mDeviceListLabel;
    private JComboBox mDeviceListComboBox;
    private JLabel mFlashCommandLabel;
    private JLabel mModeLabel;
    private JTextArea mFlashCommandTextArea;
    private JButton mFlashStopButton;
    private JLabel mOutDirLabel;
    private JComboBox mOutDirComboBox;
    private JButton mChooseOutDirButton;
    private JButton mRebootButton;
    private JButton mRebootBootloaderButton;
    private JLabel mFlashStatusLabel;
    private JLabel mAdbSyncArgumentLabel;
    private JComboBox mAdbSyncArgumentComboBox;
    private JComboBox mFastBootArgumentComboBox;
    private JLabel mFastBootArgumentLabel;
    private JCheckBox mWipeCheckBox;
    private JScrollPane mLogScroll;
    private JCheckBox mVerboseCheckBox;
    private ConsoleViewImpl mConsoleView;
    private JFileChooser jFlashFileChooser;
    private ToolWindow mToolWindow;
    private String mProjectPath;
    private Builder mBuilder;
    private Project mProject;

    private DeviceManager mDeviceManager;

    private boolean mIsCreated;

    private final static String ADB_PROPERTIES_PATH = "adb_sync_argument.properties";
    private final static String FASTBOOT_PROPERTIES_PATH = "fastboot_argument.properties";
    private final static String TARGET_PROPERTIES_PATH = "target_argument.properties";

    private ArgumentProperties mAdbSyncProperties;
    private ArgumentProperties mFastbootProperties;
    private ArgumentProperties mTargetProperties;


    public AndroidBuilderFactory() {
        mAndroidBuilderFactory = this;

        ArgumentPropertiesManager argumentPropertiesManager = new ArgumentPropertiesManager();
        mAdbSyncProperties = argumentPropertiesManager.loadProperties(ADB_PROPERTIES_PATH);
        mFastbootProperties = argumentPropertiesManager.loadProperties(FASTBOOT_PROPERTIES_PATH);
        mTargetProperties = argumentPropertiesManager.loadProperties(TARGET_PROPERTIES_PATH);

    }

    synchronized public static AndroidBuilderFactory getInstance() {
        if (mAndroidBuilderFactory != null) {
            return mAndroidBuilderFactory;
        }
        return null;
    }

    public void notifySetSdk(final Project project) {
        if (ProjectRootManager.getInstance(project).getProjectSdk() == null) {
            Notifications.Bus.notify(new Notification("Android Builder", "Android Builder",
                    "<a href=''>Project SDK is not selected. Set project SDK</a>",
                    NotificationType.WARNING,
                    new NotificationListener() {
                        @Override
                        public void hyperlinkUpdate(@NotNull Notification notification, @NotNull HyperlinkEvent event) {
                            ProjectStructureConfigurable configurable =  ProjectStructureConfigurable.getInstance(project);
                            configurable.selectProjectGeneralSettings(true);
                            ShowSettingsUtil.getInstance().editConfigurable(project,
                                    configurable);
                        }
                    }));
        }
    }

    @Override
    public void createToolWindowContent(@NotNull final Project project, @NotNull ToolWindow toolWindow) {

        notifySetSdk(project);

        // set Make configuration
        mProjectPath = project.getBasePath();
        mProjectPath = "/home/myoo/WORKSPACE";
        mBuilder = new Builder(mProjectPath);
        if (!mBuilder.isAOSPPath()) {
            Messages.showMessageDialog(mProject, "This Project is Not AOSP.", "Android Builder",
                    Messages.getInformationIcon());
            return;
        }
        new File(pathJoin(mProjectPath, OUT_DIR)).mkdir();

        // set FastBoot configuration


        mDeviceManager = new DeviceManager();
        jFlashFileChooser = new JFileChooser();


        mProject = project;
        mToolWindow = toolWindow;
        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        Content content = contentFactory.createContent(mAndroidBuilderContent, "", false);
        toolWindow.getContentManager().addContent(content);

        // make panel setScroll

        initMakePanelComboBoxes();
        initMakePanelButtons();
        initMakePanelRadioButtons();
        writeMakeCommand();
        mIsCreated = true;

        try {
            mDeviceManager.adbInit();
            mDeviceManager.fastBootMonitorInit();
        } catch (AndroidHomeNotFoundException e) {
            mOutDirComboBox.setPrototypeDisplayValue("XXXXXXXXX");
            printLog("Can't find Android Home. Can't use flash function.\n Please set Android SDK or Android Home");
            e.printStackTrace();
            return;
        }

        // flash panel setScroll

        initFlashPanelRadioButtons();
        initFlashPanelComboBoxes();
        initFlashButtons();
    }

    public boolean isCreated() {
        return mIsCreated;
    }

    private void writeMakeCommand() {
        String[] product = mProductComboBox.getSelectedItem().toString().split("-");
        if (mMakeRadioButton.isSelected()) {
            mMakeCommandTextArea.setText("make -j" + mJobNumberComboBox.getSelectedItem() + " " +
                    mTargetComboBox.getSelectedItem()
                    + " OUT_DIR=" + mResultPathComboBox.getSelectedItem()
                    + " TARGET_PRODUCT=" + product[0]
                    + " TARGET_BUILD_VARIANT=" + product[1]);
        } else {
            mMakeCommandTextArea.setText("mm -j" + mJobNumberComboBox.getSelectedItem()
                    + " OUT_DIR=" + mResultPathComboBox.getSelectedItem()
                    + " TARGET_PRODUCT=" + product[0]
                    + " TARGET_BUILD_VARIANT=" + product[1]);
        }
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

    public void doMm() {
        mMmRadioButton.doClick();
        mMakeButton.doClick();
    }

    private void initMakePanelButtons() {
        final JFileChooser jFileChooser;
        jFileChooser = new JFileChooser();
        jFileChooser.setCurrentDirectory(new File(pathJoin(mProjectPath, OUT_DIR)));
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
                    mResultPathComboBox.addItem(selectedDir);
                    mResultPathComboBox.setSelectedItem(selectedDir);
                    jFileChooser.setCurrentDirectory(selectedDir);
                    if (mOutDirComboBox.getSelectedItem() == null) {
                        String path = pathJoin(selectedDir.toString(), "target", "product");
                        jFlashFileChooser.setCurrentDirectory(new File(path));
                    }
                } else {
                    Messages.showMessageDialog(mProject, "Please select writable dir", "Android Builder",
                            Messages.getInformationIcon());
                }
            }
        });

        mChooseResultPathButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                jFileChooser.showDialog(mAndroidBuilderContent, "Choose");
            }
        });

        mMakeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                mBuilder.setOneShotMakefile(null);
                mBuilder.setTarget(null);
                mBuilder.setVerbose(mVerboseCheckBox.isSelected());
                String[] product = mProductComboBox.getSelectedItem().toString().split("-");
                mBuilder.setMakeOptions(mJobNumberComboBox.getSelectedItem().toString(),
                        mResultPathComboBox.getSelectedItem().toString(),
                        product[0], product[1]);

                if (mMmRadioButton.isSelected()) {
                    String selectedPath = mTargetDirComboBox.getSelectedItem().toString();
                    if (CURRENT_PATH.equals(selectedPath)) {
                        Document currentDoc = FileEditorManager.getInstance(mProject).getSelectedTextEditor().
                                getDocument();
                        VirtualFile currentDir = FileDocumentManager.getInstance().getFile(currentDoc).getParent();
                        String path = currentDir.getPath();

                        if (path != mProjectPath) {
                            while (true) {
                                if (new File(pathJoin(path, ANDROID_MK)).exists()) {
                                    path = path.replace(mProjectPath + File.separator, "");
                                    mBuilder.setOneShotMakefile(pathJoin(path, ANDROID_MK));
                                    int i;
                                    for (i = 0; i < mTargetDirComboBox.getItemCount(); i++) {
                                        if (mTargetDirComboBox.getItemAt(i).equals(path)) {
                                            break;
                                        }
                                    }
                                    if (i == mTargetDirComboBox.getItemCount()) {
                                        mTargetDirComboBox.addItem(path);
                                    }
                                    break;
                                } else if (path == mProjectPath) {
                                    Messages.showMessageDialog(mProject, "Android.mk is not exist", "Android Builder",
                                            Messages.getInformationIcon());
                                    return;
                                }
                                currentDir = currentDir.getParent();
                                if (currentDir == null) {
                                    Messages.showMessageDialog(mProject, "Android.mk is not exist", "Android Builder",
                                            Messages.getInformationIcon());
                                    return;
                                }
                                path = currentDir.getPath();
                            }
                        }
                    } else {
                        mBuilder.setOneShotMakefile(pathJoin(selectedPath, ANDROID_MK));
                    }
                    mBuilder.setTarget("all_modules");


                } else {
                    mBuilder.setTarget(mTargetComboBox.getSelectedItem().toString());
                }

                clearLog();
                Sdk projectSdk = ProjectRootManager.getInstance(mProject).getProjectSdk();
                if (projectSdk != null) {
                    mBuilder.setAndroidJavaHome(projectSdk.getHomePath());
                }

                mBuilder.executeMake(new ShellCommandExecutor.ThreadResultReceiver() {
                    @Override
                    public void newOut(String line) {
                        printLog(line);

                    }

                    @Override
                    public void newError(String line) {
                        printLog(line);

                    }

                    public void shellThreadDone() {
                        mMakeButton.setVisible(true);
                        mMakeStopButton.setVisible(false);
                    }
                });

                mMakeButton.setVisible(false);
                mMakeStopButton.setVisible(true);

                mBuilder.findOriginalProductOutPath(mProductComboBox.getSelectedItem().toString(),
                        new ShellCommandExecutor.ThreadResultReceiver() {
                            @Override
                            public void shellThreadDone() {

                            }

                            @Override
                            public void newOut(String line) {
                                String outDirectoryPath = mResultPathComboBox.getSelectedItem().toString() + File.separator
                                        + "target" + line.split("target")[1];
                                if (((DefaultComboBoxModel) mOutDirComboBox.getModel()).getIndexOf(outDirectoryPath)
                                        == -1) {
                                    mOutDirComboBox.addItem(outDirectoryPath);
                                }
                                mOutDirComboBox.setSelectedItem(outDirectoryPath);
                                jFlashFileChooser.setCurrentDirectory(new File(outDirectoryPath));
                                mDeviceManager.setTargetProductPath(new File(outDirectoryPath));
                            }

                            @Override
                            public void newError(String line) {

                            }
                        });
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
                writeMakeCommand();
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
                writeMakeCommand();
            }
        });

        mMakeRadioButton.doClick();
    }

    public void addPropertiesToComboBox(ArgumentProperties properties, JComboBox jComboBox) {
        for (String name : properties.getPropertyNames()) {
            jComboBox.addItem(name);
        }
    }

    private void initMakePanelComboBoxes() {
        addPropertiesToComboBox(mTargetProperties, mTargetComboBox);
        mTargetComboBox.setSelectedItem("droid");

        mTargetComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                writeMakeCommand();
            }
        });

        mTargetDirComboBox.addItem(CURRENT_PATH);
        mTargetDirComboBox.setPrototypeDisplayValue("XXXXXXXXX");


        ArrayList<String> lunchMenuList = mBuilder.getLunchMenuList();
        if (lunchMenuList != null) {
            for (String lunchMenu : lunchMenuList) {
                mProductComboBox.addItem(lunchMenu);
            }
        } else {
        }
        mProductComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                writeMakeCommand();
            }
        });

        for (int i = mBuilder.getNumberOfProcess() + 1; i > 0; i--) {
            mJobNumberComboBox.addItem(i);
        }

        // set default job number to mBuilder.getNumberOfProcess() - 1
        mJobNumberComboBox.setSelectedIndex(2);

        mJobNumberComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                writeMakeCommand();
            }
        });

        mResultPathComboBox.addItem(pathJoin(mProjectPath, OUT_DIR));
        mResultPathComboBox.setPrototypeDisplayValue("XXXXXXXXX");
        mResultPathComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                writeMakeCommand();
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
                mWipeCheckBox.setVisible(true);
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
                mWipeCheckBox.setVisible(false);
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
        mOutDirComboBox.setPrototypeDisplayValue("XXXXXXXXX");
        jFlashFileChooser.setCurrentDirectory(new File(pathJoin(mProjectPath, OUT_DIR)));
        jFlashFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        jFlashFileChooser.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                File selectedDir = jFlashFileChooser.getSelectedFile();
                if (selectedDir == null) return;
                if (selectedDir.exists()) {
                    mOutDirComboBox.addItem(selectedDir);
                    mOutDirComboBox.setSelectedItem(selectedDir);
                    jFlashFileChooser.setCurrentDirectory(selectedDir);
                    mDeviceManager.setTargetProductPath(selectedDir);
                } else {
                    Messages.showMessageDialog(mProject, "Please select exist dir", "Android Builder",
                            Messages.getInformationIcon());
                }
                writeFlashCommand();
            }
        });

        mChooseOutDirButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                jFlashFileChooser.showDialog(mAndroidBuilderContent, "Choose");
                writeFlashCommand();
            }
        });

        mFlashButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (mOutDirComboBox.getSelectedItem() == null) {
                    Messages.showMessageDialog(mProject, "Please choose out directory", "Android Builder",
                            Messages.getInformationIcon());

                } else if (mDeviceListComboBox.getSelectedItem() == null) {
                    Messages.showMessageDialog(mProject, "Can't find device", "Android Builder",
                            Messages.getInformationIcon());
                } else {
                    boolean wipe = mWipeCheckBox.isSelected();
                    mDeviceManager.flash(mDeviceListComboBox.getSelectedItem().toString().split(" ")[1], wipe,
                            fastBootArgumentComboBoxInterpreter(mFastBootArgumentComboBox.getSelectedItem()
                                    .toString()), new ShellCommandExecutor.ThreadResultReceiver() {
                                @Override
                                public void newOut(String line) {
                                    printLog(line);

                                }

                                @Override
                                public void newError(String line) {
                                    printLog(line);
                                }

                                @Override
                                public void shellThreadDone() {
                                    boolean isFastBootRadioButtonClicked = mFastbootRadioButton.isSelected();
                                    mFlashButton.setVisible(isFastBootRadioButtonClicked);
                                    mSyncButton.setVisible(!isFastBootRadioButtonClicked);
                                    mFlashStopButton.setVisible(false);
                                }
                            });
                    mFlashButton.setVisible(false);
                    mSyncButton.setVisible(false);
                    mFlashStopButton.setVisible(true);
                }
            }
        });

        mSyncButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (mOutDirComboBox.getSelectedItem() == null) {
                    Messages.showMessageDialog(mProject, "Please choose out directory", "Android Builder",
                            Messages.getInformationIcon());
                } else if (mDeviceListComboBox.getSelectedItem() == null) {
                    Messages.showMessageDialog(mProject, "Can't find device", "Android Builder",
                            Messages.getInformationIcon());
                } else {
                    String argument = mAdbSyncArgumentComboBox.getSelectedItem().toString();
                    if ("All".equals(argument)) {
                        argument = null;
                    }

                    mDeviceManager.adbRemount((IDevice) mDeviceListComboBox.getSelectedItem(),
                            new ShellCommandExecutor.ResultReceiver() {
                                @Override
                                public void newOut(String line) {
                                    printLog(line);
                                }

                                @Override
                                public void newError(String line) {
                                    printLog(line);
                                }
                            });

                    mDeviceManager.adbSync((IDevice) mDeviceListComboBox.getSelectedItem(),
                            argument, new ShellCommandExecutor.ThreadResultReceiver() {
                                @Override
                                public void newOut(String line) {
                                    printLog(line);

                                }

                                @Override
                                public void newError(String line) {
                                    printLog(line);

                                }

                                @Override
                                public void shellThreadDone() {
                                    boolean isFastBootRadioButtonClicked = mFastbootRadioButton.isSelected();
                                    mFlashButton.setVisible(isFastBootRadioButtonClicked);
                                    mSyncButton.setVisible(!isFastBootRadioButtonClicked);
                                    mFlashStopButton.setVisible(false);
                                }
                            });
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
                mDeviceManager.rebootDevice(mDeviceListComboBox.getSelectedItem().toString().split(" ")[1],
                        new ShellCommandExecutor.ResultReceiver() {
                            @Override
                            public void newOut(String line) {
                                printLog(line);
                            }

                            @Override
                            public void newError(String line) {
                                printLog(line);
                            }
                        });
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
            public void deviceConnected(IDevice device) {
                mDeviceListComboBox.addItem(device);
                printLog("device connected : " + device);
                mDeviceManager.adbRoot(device, new ShellCommandExecutor.ResultReceiver() {
                    @Override
                    public void newOut(String line) {
                        printLog(line);
                    }

                    @Override
                    public void newError(String line) {
                        printLog(line);
                    }
                });


            }

            @Override
            public void deviceDisconnected(IDevice device) {
                mDeviceListComboBox.removeItem(device);
                printLog("device disconnected : " + device);
            }

            @Override
            public void deviceChanged(IDevice device, int changeMask) {
                mDeviceManager.adbRoot(device, new ShellCommandExecutor.ResultReceiver() {
                    @Override
                    public void newOut(String line) {
                        printLog(line);
                    }

                    @Override
                    public void newError(String line) {
                        printLog(line);
                    }
                });
            }

            @Override
            public void fastBootDeviceConnected(String serialNumber) {


                mDeviceListComboBox.addItem("fastboot " + serialNumber);
                printLog("fastboot device connected : " + serialNumber);

            }

            @Override
            public void fastBootDeviceDisconnected(String serialNumber) {
                mDeviceListComboBox.removeItem("fastboot " + serialNumber);
                printLog("fastboot device disconnected : " + serialNumber);
            }
        });

        mOutDirComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                jFlashFileChooser.setCurrentDirectory(new File(mOutDirComboBox.getSelectedItem().toString()));
                mDeviceManager.setTargetProductPath(new File(mOutDirComboBox.getSelectedItem().toString()));
            }
        });

        addPropertiesToComboBox(mFastbootProperties, mFastBootArgumentComboBox);
        mFastBootArgumentComboBox.setSelectedItem("flashall");


        mFastBootArgumentComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                writeFlashCommand();
            }
        });

        addPropertiesToComboBox(mAdbSyncProperties, mAdbSyncArgumentComboBox);


        mAdbSyncArgumentComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                writeFlashCommand();
            }
        });
    }

    private String[] fastBootArgumentComboBoxInterpreter(String argument) {
        return mFastbootProperties.getArguments(argument, "/");
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

    private void printLog(String log) {
        mFilteredLogArea.postAppendEvent(log + "\n");
    }

    private void clearLog() {
        mFilteredLogArea.setText("");
    }

    private static String pathJoin(String... filepath) {
        StringBuilder sb = new StringBuilder();
        for (String path : filepath) {
            sb.append(path).append(File.separator);
        }
        sb.setLength(sb.length() - 1);
        return sb.toString();
    }
}
