package dbgsprw.view;

import com.intellij.openapi.components.*;
import com.intellij.openapi.components.impl.ServiceManagerImpl;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;

/**
 * Created by ganadist on 16. 2. 29.
 */
@State(
  name="AndroidBuilderSettingStore",
  storages= {@Storage(file = StoragePathMacros.PROJECT_FILE)}
)
public class AndroidBuilderSettingStore implements PersistentStateComponent<AndroidBuilderSettingStore> {

    public String mProduct;
    public String mBuildVariant = "eng";
    public String mTarget = "droid";
    public String mExtras = "";
    public String mFastbootTarget;
    public String mAdbSyncTarget;

    @Override
    public AndroidBuilderSettingStore getState() {
        return this;
    }

    @Override
    public void loadState(AndroidBuilderSettingStore state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    @NotNull
    public static AndroidBuilderSettingStore getSettings(@NotNull Project project) {
        return ServiceManager.getService(project, AndroidBuilderSettingStore.class);
    }
}
