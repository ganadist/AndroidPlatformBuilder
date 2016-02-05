package org.dbgsprw.view;

import javax.swing.*;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;

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

public class FilteredTextArea {

    private final JScrollPane mJScrollPanel;
    private JTextArea mJTextArea;
    private int lineSize = 500;

    private int autoDownHeight = 500;

    public FilteredTextArea(JTextArea jTextArea, JScrollPane jScrollPane) {
        mJTextArea = jTextArea;
        mJScrollPanel = jScrollPane;
    }

    public void filteringAppend(String log) {

        boolean isViewingBottom = false;
        final JScrollBar jScrollBar = mJScrollPanel.getVerticalScrollBar();
        if (jScrollBar.getMaximum() - jScrollBar.getValue() <= autoDownHeight) {
            isViewingBottom = true;
        }
        int excessLine = mJTextArea.getLineCount() - lineSize;
        if (excessLine >= 0) {
            mJTextArea.replaceRange("", 0, excessLine);
        }
        String filteredString = filter(log);
        mJTextArea.append(filteredString);
        if (isViewingBottom) {
            jScrollBar.addAdjustmentListener(new AdjustmentListener() {
                @Override
                public void adjustmentValueChanged(AdjustmentEvent adjustmentEvent) {
                    adjustmentEvent.getAdjustable().setValue(adjustmentEvent.getAdjustable().getMaximum());
                    jScrollBar.removeAdjustmentListener(this);
                }
            });

        }
    }

    public String filter(String log) {
        return log;
    }

    public void setLineSize(int lineSize) {
        this.lineSize = lineSize;
    }

    public int getLineSize() {
        return lineSize;
    }

    public int getAutoDownHeight() {
        return autoDownHeight;
    }

    public void setAutoDownHeight(int autoDownHeight) {
        this.autoDownHeight = autoDownHeight;
    }
}
