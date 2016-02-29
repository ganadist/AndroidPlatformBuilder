package dbgsprw.action;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ShortcutSet;
import com.intellij.openapi.ui.Messages;
import dbgsprw.view.AndroidBuilderFactory;
import dbgsprw.view.AndroidBuilderView;

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

public class MakeAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {

        AndroidBuilderView view = AndroidBuilderFactory.getInstance(e.getProject());
        if (view == null) {
            AndroidBuilderFactory.showNotification("Please Enable Tool Window First.\nSelect View -> Tool Windows -> Android Builder",
                    NotificationType.ERROR);
        } else if (view.canBuild()) {
            view.doMake();
        } else {
            AndroidBuilderFactory.showNotification("Other build is processing.",
                    NotificationType.ERROR);
        }
    }

    public void setShortcutSet(ShortcutSet shortcutSet) {
        super.setShortcutSet(shortcutSet);
    }
}
