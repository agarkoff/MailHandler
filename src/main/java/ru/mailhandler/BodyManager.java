package ru.mailhandler;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.misterparser.common.ShuffleIterator;
import ru.misterparser.common.Utils;
import ru.misterparser.common.configuration.ConfigurationUtils;
import ru.mailhandler.settings.Folder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: Stas
 * Date: 18.12.16
 * Time: 15:22
 */
public class BodyManager {

    private static final Logger log = LogManager.getLogger(BodyManager.class);

    private static final BodyManager bodyManager = new BodyManager();

    public static BodyManager get() {
        return bodyManager;
    }

    private BodyManager() {
    }

    private Map<Folder, ShuffleIterator<String>> bodies = new LinkedHashMap<>();

    public String getRandomBody(Folder folder) {
        ShuffleIterator<String> shuffleIterator = bodies.get(folder);
        if (shuffleIterator == null) {
            String pathname = ConfigurationUtils.getCurrentDirectory() + "folders/" + folder.getTitle() + "/body.txt";
            try {
                String text = Utils.readFileAsString(new File(pathname), "UTF-8");
                shuffleIterator = new ShuffleIterator<>(new ArrayList<>(Arrays.asList(StringUtils.splitByWholeSeparator(text, "+++"))));
                log.debug("From file " + pathname + " loaded body texts: " + shuffleIterator.size());
            } catch (IOException e) {
                log.debug("Can not open file: " + pathname);
                log.debug("IOException", e);
                return null;
            }
            bodies.put(folder, shuffleIterator);
        }
        return shuffleIterator.next();
    }
}
