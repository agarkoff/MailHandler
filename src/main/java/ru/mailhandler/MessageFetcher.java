package ru.mailhandler;

import com.sun.mail.pop3.POP3Folder;
import com.sun.mail.pop3.POP3Store;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.mailhandler.db.Database;
import ru.mailhandler.model.SeenMessage;
import ru.mailhandler.settings.Settings;

import javax.mail.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Created with IntelliJ IDEA.
 * User: Stas
 * Date: 17.12.16
 * Time: 18:45
 */
public class MessageFetcher implements AutoCloseable {

    private static final Logger log = LogManager.getLogger(MessageFetcher.class);

    private List<Message> messages;
    private POP3Store store;
    private POP3Folder folder;

    public MessageFetcher() throws SQLException, InterruptedException, MessagingException {
        log.debug("Connect to mailbox: " + Settings.get().POP3_EMAIL_LOGIN);

        Properties properties = new Properties();
        properties.setProperty("mail.pop3.connectionpooltimeout", String.valueOf(Settings.get().POP3_MAILBOX_CONNECTION_TIMEOUT));
        properties.setProperty("mail.pop3.connectiontimeout", String.valueOf(Settings.get().POP3_MAILBOX_CONNECTION_TIMEOUT));
        properties.setProperty("mail.pop3.timeout", String.valueOf(Settings.get().POP3_MAILBOX_CONNECTION_TIMEOUT));
        properties.setProperty("mail.pop3.host", Settings.get().POP3_HOST);
        properties.setProperty("mail.pop3.port", String.valueOf(Settings.get().POP3_PORT));
        properties.setProperty("java.net.useSystemProxies", "true");

        //final String SSL_FACTORY = "javax.net.ssl.SSLSocketFactory";
        //properties.setProperty("mail.pop3.socketFactory.class", SSL_FACTORY);

        Session session = Session.getInstance(properties);
        store = (POP3Store) session.getStore("pop3");
        store.connect(Settings.get().POP3_EMAIL_LOGIN, Settings.get().POP3_EMAIL_PASSWORD);
        folder = (POP3Folder) store.getFolder("INBOX");
        folder.open(Folder.READ_WRITE);
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void fetch() throws InterruptedException {
        try {
            Message[] messages = folder.getMessages();
            log.debug("Message count in mailbox: " + messages.length);

            FetchProfile fetchProfile = new FetchProfile();
            fetchProfile.add(UIDFolder.FetchProfileItem.UID);
            folder.fetch(messages, fetchProfile);

            List<Message> messagesList = new ArrayList<>();
            for (Message message : messages) {
                String uid = getUID(message);
                SeenMessage seenMessage = Database.get().getSeenMessageDao().queryBuilder().where().eq("uid", uid).queryForFirst();
                if (seenMessage == null) {
                    messagesList.add(message);
                    Database.get().getSeenMessageDao().create(new SeenMessage(uid, System.currentTimeMillis()));
                }
                if (messagesList.size() >= Settings.get().ONCE_MESSAGE_COUNT) {
                    log.debug("Limit once message count: " + messagesList.size());
                    break;
                }
            }
            this.messages = messagesList;
            log.debug("New messages count: " + this.messages.size());
        } catch (Throwable t) {
            log.debug("Throwable", t);
        }
    }

    public String getUID(Message message) throws MessagingException {
        return folder.getUID(message);
    }

    @Override
    public void close() throws Exception {
        log.debug("Closed folder and session...");
        if (folder.isOpen()) {
            folder.close(true);
        }
        store.close();

    }
}
