package ru.mailhandler.thread;

import com.j256.ormlite.stmt.QueryBuilder;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.mailhandler.Helpers;
import ru.mailhandler.MailBoxLocker;
import ru.mailhandler.MainFrame;
import ru.mailhandler.SocksManager;
import ru.mailhandler.db.Database;
import ru.mailhandler.settings.Settings;
import ru.mailhandler.stat.Stats;
import ru.misterparser.common.ControlledRunnable;
import ru.mailhandler.model.Proxy;
import ru.mailhandler.model.ResponseTime;
import ru.mailhandler.model.ScheduledMessage;
import ru.mailhandler.model.SentMessage;

import javax.activation.DataHandler;
import javax.mail.*;
import javax.mail.internet.*;
import javax.mail.util.ByteArrayDataSource;
import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Properties;

/**
 * Created with IntelliJ IDEA.
 * User: Stas
 * Date: 20.12.16
 * Time: 20:36
 */
public class MessageSender extends ControlledRunnable {

    private static final Logger log = LogManager.getLogger(MessageSender.class);

    private MainFrame mainFrame;

    public MessageSender(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
    }

    @Override
    public void run() {
        while (true) {
            try {
                while (!Helpers.workTime()) {
                    log.debug("Off hours");
                    Thread.sleep(10 * DateUtils.MILLIS_PER_MINUTE);
                }
                //log.debug("Поток отправки сообщений");
                int c = 0;
                QueryBuilder<ScheduledMessage, Long> queryBuilder = Database.get().getScheduledMessageDao().queryBuilder();
                List<ScheduledMessage> scheduledMessages = queryBuilder.where().le("sending_time", System.currentTimeMillis()).query();
                log.debug("To send scheduled emails: " + scheduledMessages.size());
                int requiredCountProxy = getRequiredCountProxy(scheduledMessages.size());
                long proxyCount = Database.get().getProxyDao().countOf();
                long additionalProxyCount = requiredCountProxy - proxyCount > 0 ? requiredCountProxy - proxyCount : 0;
                log.debug("Live proxy: " + proxyCount + ", need download additionally " + additionalProxyCount);
                SocksManager.get().fillProxies(additionalProxyCount);
                mainFrame.getMessageSenderProgressBar().setValue(0);
                mainFrame.getMessageSenderProgressBar().setMaximum(scheduledMessages.size());
                for (ScheduledMessage scheduledMessage : scheduledMessages) {
                    if (sendScheduledMessage(scheduledMessage, scheduledMessages.size() > 5)) {
                        Stats.get().incCountSentEmail();
                        MainFrame.get().getFetchEmailIntervalStats().incSentByCategories(scheduledMessage.getFolderName());

                    }
                    mainFrame.getMessageSenderProgressBar().setValue(mainFrame.getMessageSenderProgressBar().getValue() + 1);
                    log.debug("Pause between sending messages: " + Settings.get().TIMEOUT_BETWEEN_MESSAGE_SEND / 1000 + " s");
                    Thread.sleep(Settings.get().TIMEOUT_BETWEEN_MESSAGE_SEND);
                }
                mainFrame.getMessageSenderProgressBar().setValue(0);
                log.debug("Emails sent: " + c);
                Thread.sleep(1 * DateUtils.MILLIS_PER_MINUTE);
                checkForWait();
            } catch (InterruptedException e) {
                log.debug("Stopping MessageSender");
                return;
            } catch (Throwable t) {
                log.debug("Throwable", t);
            }
        }
    }

    private boolean sendScheduledMessage(ScheduledMessage scheduledMessage, boolean changeProxy) throws InterruptedException {
        boolean result = false;
        synchronized (MailBoxLocker.lock) {
            log.debug("Obtain exclusive access to the mailbox");
            try {
                log.debug("Send message with subject: " + scheduledMessage.getSubject() + " by email: " + scheduledMessage.getTo());
                log.debug("Connect to mailbox: " + Settings.get().POP3_EMAIL_LOGIN);

                Properties properties = System.getProperties();
                properties.setProperty("mail.smtp.connectionpooltimeout", String.valueOf(Settings.get().SMTP_MAILBOX_CONNECTION_TIMEOUT));
                properties.setProperty("mail.smtp.connectiontimeout", String.valueOf(Settings.get().SMTP_MAILBOX_CONNECTION_TIMEOUT));
                properties.setProperty("mail.smtp.timeout", String.valueOf(Settings.get().SMTP_MAILBOX_CONNECTION_TIMEOUT));
                properties.setProperty("mail.smtp.host", Settings.get().SMTP_HOST);
                properties.setProperty("mail.smtp.port", String.valueOf(Settings.get().SMTP_PORT));
                properties.setProperty("mail.smtp.auth", "true");
                //properties.setProperty("java.net.useSystemProxies", "true");

                if (Settings.get().SMTP_SSL) {
                    properties.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
                    properties.put("mail.smtp.socketFactory.fallback", "false");
                }

                if (changeProxy) {
                    SocksManager.get().nextProxy();
                } else {
                    log.debug("Do not change the proxy for each letter");
                }

                //System.setProperty("socksProxyHost", proxy.getHost());
                //System.setProperty("socksProxyPort", String.valueOf(proxy.getPort()));

                Session session = Session.getInstance(properties, new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(Settings.get().SMTP_EMAIL_LOGIN, Settings.get().SMTP_EMAIL_PASSWORD);
                    }
                });
                MimeMessage mimeMessage = new MimeMessage(session);
                InternetAddress internetAddress = null;
                if (StringUtils.isNotBlank(Settings.get().SENDER_FROM)) {
                    InternetAddress[] internetAddresses = null;
                    try {
                        internetAddresses = InternetAddress.parse(Settings.get().SENDER_FROM);
                    } catch (AddressException ignored) {
                    }
                    if (internetAddresses != null && internetAddresses.length > 0) {
                        internetAddress = internetAddresses[0];
                    }
                }
                if (internetAddress == null) {
                    internetAddress = new InternetAddress(Settings.get().SMTP_EMAIL_LOGIN, Settings.get().SENDER_NAME);
                }
                log.debug("Email will be sent with FROM: " + internetAddress);
                mimeMessage.setFrom(internetAddress);
                String recipient = StringUtils.stripEnd(scheduledMessage.getTo(), ".");
                mimeMessage.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));

                mimeMessage.setSubject(scheduledMessage.getSubject(), "UTF-8");
                {
                    Multipart multipart = new MimeMultipart();
                    if (StringUtils.isNotBlank(scheduledMessage.getAttachmentFilenames())) {
                        for (String attachmentFilename : StringUtils.split(scheduledMessage.getAttachmentFilenames(), ":")) {
                            try {
                                MimeBodyPart messageBodyPart = new MimeBodyPart();
                                byte[] data = FileUtils.readFileToByteArray(new File(attachmentFilename));
                                messageBodyPart.setDataHandler(new DataHandler(new ByteArrayDataSource(data, Helpers.getMimeType(attachmentFilename))));
                                messageBodyPart.setFileName(FilenameUtils.getBaseName(attachmentFilename) + "." + FilenameUtils.getExtension(attachmentFilename));
                                multipart.addBodyPart(messageBodyPart);
                                Thread.sleep(1);
                            } catch (InterruptedException e) {
                                throw e;
                            } catch (Throwable t) {
                                log.debug("Throwable", t);
                            }
                        }
                    }
                    {
                        MimeBodyPart messageBodyPart = new MimeBodyPart();
                        messageBodyPart.setContent(scheduledMessage.getBody(), "text/html; charset=utf-8");
                        multipart.addBodyPart(messageBodyPart);
                    }
                    mimeMessage.setContent(multipart);
                }
                {
                    InternetAddress[] replyTos = InternetAddress.parse(Settings.get().REPLY_TO, false);
                    for (InternetAddress replyTo : replyTos) {
                        log.debug("Process reply-to: " + replyTo);
                        String personal = replyTo.getPersonal();
                        if (replyTo.getPersonal() != null) {
                            replyTo.setPersonal(MimeUtility.encodeText(personal));
                        }
                    }
                    mimeMessage.setReplyTo(replyTos);
                }
                {
                    mimeMessage.setHeader("X-Priority", String.valueOf(scheduledMessage.getPriority()));
                }
                Transport.send(mimeMessage);
                Database.get().getResponseTimeDao().createOrUpdate(new ResponseTime(scheduledMessage.getTo(), System.currentTimeMillis()));
                Database.get().getScheduledMessageDao().delete(scheduledMessage);
                Proxy proxy = SocksManager.get().getCurrentProxy();
                String proxyString = proxy.getHost() + ":" + proxy.getPort();
                Database.get().getSentMessageDao().create(new SentMessage(scheduledMessage.getName(), scheduledMessage.getTo(), scheduledMessage.getSubject(), proxyString, new Date().getTime()));
                log.debug("Message sent");
                result = true;
            } catch (InterruptedException e) {
                throw e;
            } catch (Throwable t) {
                log.debug("Throwable", t);
            }
            log.debug("Access to the mailbox is unlocked");
        }
        return result;
    }

    private int getRequiredCountProxy(int size) {
        int r;
        if (size < 0) {
            throw new RuntimeException("Количество писем не может быть отрицательным");
        } else if (size == 0) {
            r = 0;
        } else if (size <= 5) {
            r = 1;
        } else if (size >= 6 && size <= 25) {
            r = 2;
        } else if (size >= 26 && size <= 50) {
            r = 5;
        } else {
            r = size / 5 + 1;
        }
        log.debug("For send " + size + " messages need " + r + " proxy");
        return r;
    }
}
