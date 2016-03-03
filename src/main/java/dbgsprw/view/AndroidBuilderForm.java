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

package dbgsprw.view;

import javax.swing.*;

/**
 * Created by ganadist on 16. 3. 3.
 */
public class AndroidBuilderForm {
    protected JPanel mAndroidBuilderContent;
    protected JPanel mMakeOptionPanel;
    protected JLabel mTargetLabel;
    protected JComboBox<String> mTargetComboBox;
    protected JRadioButton mMmRadioButton;
    protected JRadioButton mMakeRadioButton;
    protected JComboBox<String> mExtraArgumentsComboBox;
    protected JCheckBox mVerboseCheckBox;
    protected JButton mMakeButton;
    protected JSpinner mJobSpinner;
    protected JPanel mFlashOptionPanel;
    protected JComboBox<String> mDeviceListComboBox;
    protected JRadioButton mAdbSyncRadioButton;
    protected JRadioButton mFastbootRadioButton;
    protected JComboBox<String> mWriteArgumentComboBox;
    protected JCheckBox mWipeCheckBox;
    protected JButton mFlashButton;
    protected JComboBox<String> mProductComboBox;
    protected JComboBox<String> mVariantComboBox;
    protected JLabel mResultPathValueLabel;
    protected JButton mOpenDirectoryButton;

    public AndroidBuilderForm() {
        ButtonGroup group = new ButtonGroup();
        group.add(mMakeRadioButton);
        group.add(mMmRadioButton);

        group = new ButtonGroup();
        group.add(mFastbootRadioButton);
        group.add(mAdbSyncRadioButton);

        final int numberOfCpus = Runtime.getRuntime().availableProcessors();
        final int initialJobNumber = (numberOfCpus > 4) ? numberOfCpus - 1 : numberOfCpus;

        SpinnerNumberModel model = new SpinnerNumberModel(initialJobNumber,
                1, numberOfCpus, 1);
        mJobSpinner.setModel(model);
    }
}
