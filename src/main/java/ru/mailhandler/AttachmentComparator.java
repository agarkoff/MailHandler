package ru.mailhandler;

import org.apache.commons.lang3.StringUtils;
import ru.mailhandler.model.Attachment;
import ru.mailhandler.settings.Settings;

import java.util.Comparator;

public class AttachmentComparator implements Comparator<Attachment> {
    @Override
    public int compare(Attachment o1, Attachment o2) {
        int i1 = Integer.MAX_VALUE;
        int i2 = Integer.MAX_VALUE;
        for (int i = 0; i < Settings.get().EMAIL_FOR_RESPONSE_FINDER_FILENAME_TAGS.size(); i++) {
            String tag = Settings.get().EMAIL_FOR_RESPONSE_FINDER_FILENAME_TAGS.get(i);
            if (StringUtils.containsIgnoreCase(o1.getFilename(), tag)) {
                i1 = i + 1;
            }
            if (StringUtils.containsIgnoreCase(o2.getFilename(), tag)) {
                i2 = i + 1;
            }
        }
        for (int i = 0; i < Settings.get().EMAIL_FOR_RESPONSE_FINDER_EXTENSIONS.size(); i++) {
            String ext = Settings.get().EMAIL_FOR_RESPONSE_FINDER_EXTENSIONS.get(i);
            if (i1 == Integer.MAX_VALUE && StringUtils.endsWithIgnoreCase(o1.getFilename(), ext)) {
                i1 = 100 * (i + 1);
            }
            if (i2 == Integer.MAX_VALUE && StringUtils.endsWithIgnoreCase(o2.getFilename(), ext)) {
                i2 = 100 * (i + 1);
            }
        }
        if (i1 == Integer.MAX_VALUE && o1.getFilename() == null) {
            i1 = 1001;
        }
        if (i2 == Integer.MAX_VALUE && o2.getFilename() == null) {
            i2 = 1001;
        }
        //System.out.println(o1.filename + "=" + i1 + "\t\t\t" + o2.filename + "=" + i2);
        return i1 - i2;
    }
}
