package ru.mailhandler.settings;

import org.apache.commons.lang.StringUtils;
import ru.misterparser.common.model.NamedElement;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Stas
 * Date: 18.12.16
 * Time: 13:51
 */
public class Folder implements NamedElement, Cloneable {

    public static final Folder NEW_CATEGORY = new Folder("Новая категория", new ArrayList<String>(), false, false);

    private String title;
    private List<String> tags;
    private boolean emptySubject;
    private boolean isDefault;

    public Folder(String title, List<String> tags, boolean emptySubject, boolean isDefault) {
        this.title = title;
        this.tags = tags;
        this.emptySubject = emptySubject;
        this.isDefault = isDefault;
    }

    @Override
    public String getName() {
        String s = title + ", теги=(" + StringUtils.join(tags, ",") + "), пустая папка=" + emptySubject;
        if (isDefault) {
            s += ", по умолчанию";
        }
        return s;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public boolean isEmptySubject() {
        return emptySubject;
    }

    public void setEmptySubject(boolean emptySubject) {
        this.emptySubject = emptySubject;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean aDefault) {
        isDefault = aDefault;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Folder folder = (Folder) o;

        if (!title.equals(folder.title)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return title.hashCode();
    }

    @Override
    public String toString() {
        return "Folder{" +
                "title='" + title + '\'' +
                ", tags=" + tags +
                ", emptySubject=" + emptySubject +
                ", isDefault=" + isDefault +
                '}';
    }

    @Override
    public Folder clone() {
        return new Folder(title, new ArrayList<>(tags), emptySubject, isDefault);
    }
}
