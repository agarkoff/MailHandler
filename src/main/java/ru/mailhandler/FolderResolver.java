package ru.mailhandler;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.mailhandler.settings.Folder;
import ru.mailhandler.settings.Settings;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.MimeUtility;
import java.io.UnsupportedEncodingException;

/**
 * Created with IntelliJ IDEA.
 * User: Stas
 * Date: 18.12.16
 * Time: 13:47
 */
public class FolderResolver {

    private static final Logger log = LogManager.getLogger(FolderResolver.class);

    public Folder getFolder(Message message) {
        try {
            String subject = message.getSubject();
            log.debug("Subject: " + subject);
            boolean emptySubject = StringUtils.isBlank(message.getSubject());
            String from = MimeUtility.decodeText(message.getFrom()[0].toString());
            for (Folder folder : Settings.get().FOLDERS) {
                if (folder.isEmptySubject() == emptySubject) {
                    for (String tag : folder.getTags()) {
                        if (StringUtils.containsIgnoreCase(subject, tag) || StringUtils.containsIgnoreCase(from, tag)) {
                            log.debug("By tags resolved folder: " + folder.getTitle());
                            return folder;
                        }
                    }
                }
            }
            for (Folder folder : Settings.get().FOLDERS) {
                if (folder.isDefault()) {
                    log.debug("Folder by default: " + folder.getTitle());
                    return folder;
                }
            }
        } catch (MessagingException e) {
            log.debug("MessagingException", e);
        } catch (UnsupportedEncodingException e) {
            log.debug("UnsupportedEncodingException", e);
        }
        log.debug("Fit folder not found");
        return null;
    }
}
