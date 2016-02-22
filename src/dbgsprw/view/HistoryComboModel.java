package dbgsprw.view;

import javax.swing.*;
import java.util.ArrayList;

/**
 * Created by ganadist on 16. 2. 19.
 */
public class HistoryComboModel extends DefaultComboBoxModel<String> {
    public HistoryComboModel() {
        super();
    }

    public HistoryComboModel(ArrayList<String> items) {
        super();
        for (String item: items) {
            addElement(item);
        }
    }

    protected void addHistory(String obj) {
        removeElement(obj);
        insertElementAt((String)obj, 0);
    }

    @Override
    public void setSelectedItem(Object obj) {
        addHistory((String)obj);
        super.setSelectedItem(obj);
    }
}
