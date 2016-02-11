package dbgsprw.view;


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

public class LogAppendAwtEvent extends AWTEvent {

    public static final int EVENT_ID = AWTEvent.RESERVED_ID_MAX + 427;
    private String mTextLine;

    LogAppendAwtEvent(Object target, String textLine) {
        super(target, EVENT_ID);
        this.mTextLine = textLine;
    }

    public String getString() {
        return (mTextLine);
    }


}
