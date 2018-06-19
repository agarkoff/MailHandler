package ru.mailhandler.filter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.mailhandler.EmailForResponseFinder;

import javax.mail.Message;
import javax.mail.MessagingException;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: Stas
 * Date: 28.12.16
 * Time: 16:19
 */
public class OneAddressFilter {

    private static final Logger log = LogManager.getLogger(OneAddressFilter.class);

    public void filter(List<Message> messages) {
        if (true) {
            log.debug("One address filter");
            log.debug("Message count before: " + messages.size());
            Set<String> emails = new LinkedHashSet<>();
            for (Iterator<Message> iterator = messages.iterator(); iterator.hasNext(); ) {
                try {
                    Message message = iterator.next();
                    String email = EmailForResponseFinder.get().getEmail(message);
                    if (email != null) {
                        if (!emails.add(email)) {
                            log.debug("Message decline by email address " + email);
                            iterator.remove();
                        }
                    } else {
                        log.debug("Delete message without email for response, message subject: " + message.getSubject());
                    }
                } catch (MessagingException e) {
                    log.debug("MessagingException", e);
                }
            }
            log.debug("Message count after: " + messages.size());
        } else {
            log.debug("Disable One address filter");
        }
    }
}
