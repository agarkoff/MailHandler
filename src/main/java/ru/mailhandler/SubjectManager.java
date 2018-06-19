package ru.mailhandler;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.misterparser.common.ShuffleIterator;
import ru.misterparser.common.configuration.ConfigurationUtils;
import ru.mailhandler.settings.Folder;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: Stas
 * Date: 18.12.16
 * Time: 15:22
 */
public class SubjectManager {

    private static final Logger log = LogManager.getLogger(SubjectManager.class);

    private static final SubjectManager subjectManager = new SubjectManager();

    public static SubjectManager get() {
        return subjectManager;
    }

    private SubjectManager() {
    }

    private Map<Folder, ShuffleIterator<String>> subjects = new LinkedHashMap<>();

    public String getRandomSubject(Folder folder) {
        ShuffleIterator<String> shuffleIterator = subjects.get(folder);
        if (shuffleIterator == null) {
            String pathname = ConfigurationUtils.getCurrentDirectory() + "folders/" + folder.getTitle() + "/subject.txt";
            try {
                List<String> lines = FileUtils.readLines(new File(pathname), "UTF-8");
                shuffleIterator = new ShuffleIterator<>(lines);
                log.debug("From file " + pathname + " loaded subjects: " + shuffleIterator.size());
            } catch (IOException e) {
                log.debug("Cannot open file: " + pathname);
                log.debug("IOException", e);
                return null;
            }
            subjects.put(folder, shuffleIterator);
        }
        return shuffleIterator.next();
    }
}
