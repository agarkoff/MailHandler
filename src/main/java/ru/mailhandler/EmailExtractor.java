package ru.mailhandler;

import org.apache.commons.collections4.set.ListOrderedSet;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: Stas
 * Date: 18.12.16
 * Time: 11:49
 */
public class EmailExtractor {

    private static final Logger log = LogManager.getLogger(EmailExtractor.class);

    private static final Pattern EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+");

    public ListOrderedSet<String> getEmails(String text) {
        ListOrderedSet<String> result = new ListOrderedSet<>();
        Matcher matcher = EMAIL_PATTERN.matcher(text);
        while (matcher.find()) {
            String email = matcher.group();
            email = StringUtils.stripEnd(email, ".");
            log.debug("Extracted email: " + email);
            result.add(email);
        }
        log.debug("Extracted email count: " + result.size());
        return result;
    }
}
