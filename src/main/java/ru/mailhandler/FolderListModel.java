package ru.mailhandler;

import ru.mailhandler.settings.Folder;

import javax.swing.DefaultListModel;
import java.util.Collection;

/**
 * Created with IntelliJ IDEA.
 * User: Stas
 * Date: 23.12.16
 * Time: 22:45
 */
public class FolderListModel extends DefaultListModel<Folder> {

    public FolderListModel() {
        super();
    }

    public void setCollection(Collection<Folder> folders) {
        clear();
        for (Folder folder : folders) {
            addElement(folder);
        }
    }

    @Override
    public void setElementAt(Folder element, int index) {
        super.setElementAt(element, index);
    }

    @Override
    public void insertElementAt(Folder element, int index) {
        super.insertElementAt(element, index);
    }

    @Override
    public void addElement(Folder element) {
        super.addElement(element);
    }

    @Override
    public Folder set(int index, Folder element) {
        return super.set(index, element);
    }

    @Override
    public void add(int index, Folder element) {
        super.add(index, element);
    }

    private void resetDefaultFlag(Folder exclude) {
        for (int i = 0; i < getSize(); i++) {
            Folder folder = getElementAt(i);
            if (!folder.equals(exclude)) {
                folder.setDefault(false);
            }
        }
    }

    public void setDefaultFlag(Folder folder, boolean isDefault) {
        folder.setDefault(isDefault);
        resetDefaultFlag(folder);
    }
}
