package dbgsprw.view;

/**
 * Created by ganadist on 16. 2. 22.
 */
public class TargetDirHistoryComboModel extends HistoryComboModel {
    private String mDefault;
    public TargetDirHistoryComboModel(String def) {
        super();
        mDefault = def;
        super.addElement(def);
    }

    @Override
    protected void addHistory(String obj) {
        if (!mDefault.equals(obj)) {
            removeElement(obj);
            insertElementAt(obj, 1);
        }
    }

    @Override
    public void addElement(String obj) {
        addHistory(obj);
    }
}
