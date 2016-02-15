package dbgsprw.view;


import javax.swing.JTextArea;
import java.awt.Toolkit;
import java.awt.AWTEvent;

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

public class FilteredTextArea extends JTextArea {

    private int mLineSize = 1000;
    private java.awt.EventQueue mEventQueue;

    public FilteredTextArea() {
        setLineWrap(true);
        mEventQueue = Toolkit.getDefaultToolkit().getSystemEventQueue();
        enableEvents(LogAppendAwtEvent.EVENT_ID);
    }

    public void postAppendEvent(String text) {
        mEventQueue.postEvent(new LogAppendAwtEvent(this, text));
    }


    private void filteringAppend(String log) {

        int excessLine = getLineCount() - mLineSize;
        if (excessLine >= 1) {
            replaceRange("", 0, excessLine);
            setCaretPosition(getDocument().getLength());
        }
        String filteredString = filter(log);
        append(filteredString);
    }

    @Override
    protected void processEvent(AWTEvent event) {
        if (event instanceof LogAppendAwtEvent) {
            LogAppendAwtEvent logAppendAwtEvent = (LogAppendAwtEvent) event;
            filteringAppend(logAppendAwtEvent.getString());
        } else {
            super.processEvent(event);
        }

    }

    public String filter(String log) {
        return log;
    }

    public int getLineSize() {
        return mLineSize;
    }

    public void setLineSize(int lineSize) {
        this.mLineSize = lineSize;
    }

}
