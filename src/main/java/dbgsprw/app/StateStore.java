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

package dbgsprw.app;

import com.intellij.openapi.components.*;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;

/**
 * Created by ganadist on 16. 2. 29.
 */
@State(
  name="StateStore",
  storages= {@Storage(file = StoragePathMacros.PROJECT_FILE)}
)
public class StateStore implements PersistentStateComponent<StateStore> {

    public String mProduct;
    public String mBuildVariant = "eng";
    public String mTarget = "droid";
    public String mTargetDirectory = "";
    public String mExtras = "";
    public String mFastbootTarget;
    public String mAdbSyncTarget;
    public String mLastSelectedBootloaderFilename = "";
    public String mLastSelectedUpdatePackage = "";

    @Override
    public StateStore getState() {
        return this;
    }

    @Override
    public void loadState(StateStore state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    @NotNull
    public static StateStore getState(@NotNull Project project) {
        return ServiceManager.getService(project, StateStore.class);
    }
}
