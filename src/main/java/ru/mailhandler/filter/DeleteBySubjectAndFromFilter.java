package ru.mailhandler.filter;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.mailhandler.settings.Settings;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.MimeUtility;
import java.io.UnsupportedEncodingException;

/**
 * Created with IntelliJ IDEA.
 * User: Stas
 * Date: 18.12.16
 * Time: 13:05
 */
public class DeleteBySubjectAndFromFilter {

    private static final Logger log = LogManager.getLogger(DeleteBySubjectAndFromFilter.class);

    public boolean filter(Message message) {
        //log.debug("Delete by subject and from filter");
        try {
            for (String s : Settings.get().DELETE_BY_SUBJECT_AND_FROM_FILTER_TAGS) {
                if (StringUtils.isNotBlank(s)) {
                    for (Address address : message.getFrom()) {
                        if (StringUtils.containsIgnoreCase(MimeUtility.decodeText(address.toString()), s)) {
                            log.debug("Message with From " + address.toString() + " decline by tag " + s);
                            return true;
                        }
                    }
                    if (StringUtils.containsIgnoreCase(MimeUtility.decodeText(message.getSubject()), s)) {
                        log.debug("Message with subject " + message.getSubject() + " decline by tag " + s);
                        return true;
                    }
                }
            }
        } catch (MessagingException e) {
            log.debug("MessagingException", e);
        } catch (UnsupportedEncodingException e) {
            log.debug("UnsupportedEncodingException", e);
        }
        return false;
    }
}
