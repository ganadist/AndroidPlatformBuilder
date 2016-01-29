package org.dbgsprw.view;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
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
    private ToolWindow mToolWindow;
    private String mProjectPath;

    private Builder mBuilder;
    private Project mProject;

    public AndroidBuilderFactory() {

    }

    @Override
    public void createToolWindowContent(@NotNull final Project project, @NotNull ToolWindow toolWindow) {

        mProjectPath = project.getBasePath();
        mBuilder = new Builder(mProjectPath);

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

    }

    private void writeCommand() {
        String[] product = mProductComboBox.getSelectedItem().toString().split("-");
        if (mMakeRadioButton.isSelected()) {
            mCommandTextArea.setText("make -j" + mJobNumberComboBox.getSelectedItem() + " " + mTargetComboBox.getSelectedItem()
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
                String[] product = mProductComboBox.getSelectedItem().toString().split("-");
                mBuilder.setMakeOptions(mJobNumberComboBox.getSelectedItem().toString(),
                        mResultPathComboBox.getSelectedItem().toString(),
                        product[0], product[1]);
                mBuilder.addMakeDoneListener(new Builder.MakeDoneListener() {
                    @Override
                    public void makeDone() {
                        mStatusLabel.setText("Make Done");
                    }
                });
                mBuilder.executeMake();
            }
        });

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

        mResultPathComboBox.addItem(mProjectPath + "/out");
        mResultPathComboBox.setPrototypeDisplayValue("XXXXXXXXX");
        mResultPathComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                writeCommand();
            }
        });
    }


}
