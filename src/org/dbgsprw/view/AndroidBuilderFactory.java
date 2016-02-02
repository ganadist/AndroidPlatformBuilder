package org.dbgsprw.view;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.dbgsprw.core.Builder;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;

/**
 * Copyright 2016 dbgsprw / dbgsprw@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

public class AndroidBuilderFactory implements ToolWindowFactory {
    private static AndroidBuilderFactory mAndroidBuilderFactory;
    private JPanel mAndroidBuilderContent;
    private JButton mMakeButton;
    private JComboBox mTargetComboBox;
    private JComboBox mProductComboBox;
    private JComboBox mJobNumberComboBox;
    private JComboBox mResultPathComboBox;
    private JButton mChooseDirButton;
    private JLabel mTargetLabel;
    private JLabel mProductLabel;
    private JLabel mJobNumberLabel;
    private JLabel mResultPathLabel;
    private JPanel mMakeOptionPanel;
    private JLabel mMakeLabel;
    private ButtonGroup mButtonGroup;
    private JRadioButton mMakeRadioButton;
    private JRadioButton mMmRadioButton;
    private JTextArea mCommandTextArea;
    private JLabel mStatusLabel;
    private JButton mStopButton;
    private ToolWindow mToolWindow;
    private String mProjectPath;

    private Builder mBuilder;
    private Project mProject;

    private boolean mIsCreated;

    public AndroidBuilderFactory() {
        mAndroidBuilderFactory = this;
    }

    @Override
    public void createToolWindowContent(@NotNull final Project project, @NotNull ToolWindow toolWindow) {

        mProjectPath = project.getBasePath();
        mBuilder = new Builder(mProjectPath);
        if (!mBuilder.isAOSPPath()) {
            Messages.showMessageDialog(mProject, "This Project is Not AOSP.", "Android Builder",
                    Messages.getInformationIcon());
            return;
        }

        mProject = project;
        mToolWindow = toolWindow;
        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        Content content = contentFactory.createContent(mAndroidBuilderContent, "", false);
        toolWindow.getContentManager().addContent(content);

        new File(mProjectPath + "/out").mkdir();
        initComboBoxes();
        initButtons();
        initRadioButtons();
        writeCommand();
        mIsCreated = true;
    }
    public boolean isCreated() {
        return mIsCreated;
    }

    synchronized public static AndroidBuilderFactory getInstance() {
        if (mAndroidBuilderFactory != null ) {
            return mAndroidBuilderFactory;
        }
        return null;
    }

    private void writeCommand() {
        String[] product = mProductComboBox.getSelectedItem().toString().split("-");
        if (mMakeRadioButton.isSelected()) {
            mCommandTextArea.setText("make -j" + mJobNumberComboBox.getSelectedItem() + " " +
                    mTargetComboBox.getSelectedItem()
                    + " OUT_DIR=" + mResultPathComboBox.getSelectedItem()
                    + " TARGET_PRODUCT=" + product[0]
                    + " TARGET_BUILD_VARIANT=" + product[1]);
        } else {
            mCommandTextArea.setText("mm -j" + mJobNumberComboBox.getSelectedItem()
                    + " OUT_DIR=" + mResultPathComboBox.getSelectedItem()
                    + " TARGET_PRODUCT=" + product[0]
                    + " TARGET_BUILD_VARIANT=" + product[1]);
        }
    }

    private void initButtons() {
        final JFileChooser jFileChooser = new JFileChooser();
        jFileChooser.setCurrentDirectory(new File(mProjectPath + "/out"));
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
                } else {
                    Messages.showMessageDialog(mProject, "please select writable dir", "Android Builder",
                            Messages.getInformationIcon());
                }
            }
        });

        mChooseDirButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                jFileChooser.showDialog(mAndroidBuilderContent, "Choose");
            }
        });

        mMakeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                mStatusLabel.setText("Making... Please Wait");
                mBuilder.setOneShotMakefile(null);
                mBuilder.setTarget(null);
                String[] product = mProductComboBox.getSelectedItem().toString().split("-");
                mBuilder.setMakeOptions(mJobNumberComboBox.getSelectedItem().toString(),
                        mResultPathComboBox.getSelectedItem().toString(),
                        product[0], product[1]);

                if (mMmRadioButton.isSelected()) {
                    Document currentDoc = FileEditorManager.getInstance(mProject).getSelectedTextEditor().getDocument();
                    VirtualFile currentDir = FileDocumentManager.getInstance().getFile(currentDoc).getParent();
                    String path = currentDir.getPath();

                    if(path != mProjectPath) {
                        while (true) {
                            if(new File(path+File.separator+"Android.mk").exists()) {
                                path = path.replace(mProjectPath + File.separator, "");
                                mBuilder.setOneShotMakefile(path+File.separator+"Android.mk");
                                break;
                            } else if (path == mProjectPath) {
                                Messages.showMessageDialog(mProject, "Android.mk is not exist", "Android Builder",
                                        Messages.getInformationIcon());
                                return;
                            }
                            currentDir = currentDir.getParent();
                            path = currentDir.getPath();
                        }
                    }

                }
                else {
                    mBuilder.setTarget(mTargetComboBox.getSelectedItem().toString());
                }
                mBuilder.executeMake();

                mMakeButton.setEnabled(false);
                mStopButton.setEnabled(true);
                mBuilder.addMakeDoneListener(new Builder.MakeDoneListener() {
                    @Override
                    public void makeDone() {
                        mStatusLabel.setText("");
                        mMakeButton.setEnabled(true);
                        mStopButton.setEnabled(false);
                    }
                });
            }
        });
        mStopButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                mBuilder.stopMake();
                mMakeButton.setEnabled(true);
                mStopButton.setEnabled(false);
            }
        });
    }

    public void doMm() {
        mMmRadioButton.doClick();
        mMakeButton.doClick();
    }

    private void initRadioButtons() {
        mButtonGroup = new ButtonGroup();
        mButtonGroup.add(mMakeRadioButton);
        mButtonGroup.add(mMmRadioButton);

        mMakeRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                mTargetComboBox.setEnabled(true);
                writeCommand();
            }
        });

        mMmRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                mTargetComboBox.setEnabled(false);
                writeCommand();
            }
        });
        mMakeRadioButton.setSelected(true);
    }

    private void initComboBoxes() {

        mTargetComboBox.addItem("droid");
        mTargetComboBox.addItem("snod");
        mTargetComboBox.addItem("bootimage");
        mTargetComboBox.addItem("updatepackage");
        mTargetComboBox.addItem("clean");

        mTargetComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                writeCommand();
            }
        });


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
                writeCommand();
            }
        });

        for (int i = 1; i <= mBuilder.getNumberOfProcess() + 1; i++) {
            mJobNumberComboBox.addItem(i);
        }
        mJobNumberComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                writeCommand();
            }
        });

        mResultPathComboBox.addItem(mProjectPath + File.separator + "out");
        mResultPathComboBox.setPrototypeDisplayValue("XXXXXXXXX");
        mResultPathComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                writeCommand();
            }
        });
    }


}
