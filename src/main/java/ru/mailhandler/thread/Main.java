package ru.mailhandler.thread;

import com.sun.mail.util.MailConnectException;
import httl.Engine;
import httl.Template;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.apache.http.client.utils.DateUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.sqlite.date.DateFormatUtils;
import ru.mailhandler.*;
import ru.mailhandler.db.Database;
import ru.mailhandler.filter.AntiDeleteBySubjectAndFromFilter;
import ru.mailhandler.filter.DeleteBySubjectAndFromFilter;
import ru.mailhandler.filter.OneAddressFilter;
import ru.mailhandler.filter.ResponseTimeFilter;
import ru.mailhandler.model.ResumeData;
import ru.mailhandler.model.ScheduledMessage;
import ru.mailhandler.model.SentMessage;
import ru.mailhandler.settings.Folder;
import ru.mailhandler.settings.Settings;
import ru.mailhandler.stat.FetchEmailIntervalStats;
import ru.mailhandler.stat.Stats;
import ru.misterparser.common.ControlledRunnable;
import ru.misterparser.common.configuration.ConfigurationUtils;
import ru.misterparser.common.flow.ThreadFinishStatus;
import ru.misterparser.common.mail.EmailUtils;

import javax.activation.DataHandler;
import javax.mail.*;
import javax.mail.internet.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: Stas
 * Date: 17.12.16
 * Time: 18:29
 */
public class Main extends ControlledRunnable {

    private static final Logger log = LogManager.getLogger(Main.class);

    private long lastTimeFetch;
    private long lastTimeSendAdminStats;

    private List<ResumeData> resumeDataList = new ArrayList<>();

    public Main() {
    }

    @Override
    public void run() {
        CustomProxySelector.setNoProxy(Settings.get().NO_PROXY);
        ThreadFinishStatus threadFinishStatus = ThreadFinishStatus.ERROR;
        Throwable throwable = null;
        try {
            while (true) {
                try {
                    while (!Helpers.workTime()) {
                        log.debug("Off hours");
                        Thread.sleep(10 * org.apache.commons.lang3.time.DateUtils.MILLIS_PER_MINUTE);
                    }
                    if (System.currentTimeMillis() - lastTimeSendAdminStats > 3600*1000) {
                        sendAdminStats();
                        lastTimeSendAdminStats = System.currentTimeMillis();
                    }
                    if (System.currentTimeMillis() - lastTimeFetch > Settings.get().FETCH_EMAIL_INTERVAL) {
                        log.debug("Since last download messages passed over " + Settings.get().FETCH_EMAIL_INTERVAL / 1000 + " seconds");
                        MainFrame.get().getYellowLabel().setVisible(true);
                        synchronized (MailBoxLocker.lock) {
                            log.debug("Obtain exclusive access to the mailbox");
                            boolean successFetch = false;
                            while (!successFetch) {
                                Helpers.adjustSpeed();
                                try (MessageFetcher messageFetcher = new MessageFetcher()) {
                                    messageFetcher.fetch();
                                    List<Message> messages = messageFetcher.getMessages();
                                    new OneAddressFilter().filter(messages);
                                    new ResponseTimeFilter().filter(messages);
                                    log.debug("Message count before filters: " + messages.size());
                                    for (Iterator<Message> iterator = messages.iterator(); iterator.hasNext(); ) {
                                        Message message = iterator.next();
                                        {
                                            if (Settings.get().ANTI_DELETE_BY_SUBJECT_AND_FROM_FILTER) {
                                                log.debug("Проверка письма " + message.getSubject() + "фильтром неудаления");
                                                boolean r = new AntiDeleteBySubjectAndFromFilter().filter(message);
                                                if (r) {
                                                    continue;
                                                }
                                            } else {
                                                log.debug("Disable Anti-delete by subject and from filter");
                                            }
                                        }
                                        {
                                            if (Settings.get().DELETE_BY_SUBJECT_AND_FROM_FILTER) {
                                                log.debug("Проверка письма " + message.getSubject() + "фильтром неудаления");
                                                boolean r = new DeleteBySubjectAndFromFilter().filter(message);
                                                if (r) {
                                                    iterator.remove();
                                                }
                                            } else {
                                                log.debug("Disable Delete by subject and from filter");
                                            }
                                        }
                                    }
                                    log.debug("Message after filters: " + messages.size());

                                    for (Message message : messages) {
                                        try {
                                            log.debug("Process message: " + message.getSubject());
                                            if (repeatedMail(message)) {
                                                log.debug("Message-command by repeated mail");
                                                processRepeatedMail(message);
                                            } else if (!checkUndelivery(message)) {
                                                ru.mailhandler.settings.Folder folder = new FolderResolver().getFolder(message);
                                                String from = MimeUtility.decodeText(message.getFrom()[0].toString());
                                                if (folder != null) {
                                                    String formOfAddress = getFormOfAddress(message);
                                                    ResumeData resumeData = Helpers.getResumeData(message);
                                                    if (resumeData.isNotBlank()) {
                                                        resumeDataList.add(resumeData);
                                                    }
                                                    scheduleMail(message, folder, formOfAddress, resumeData.getLocation());
                                                    Stats.get().incCategoryStats(folder.getTitle());
                                                } else {
                                                    log.debug("Message " + message.getSubject() + " from " + from + " decline by folder not found");
                                                    MainFrame.get().getFetchEmailIntervalStats().incCountCanceled();
                                                }
                                            }
                                            Stats.get().incCountReceivedEmail();
                                            MainFrame.get().getFetchEmailIntervalStats().incCountReceived();
                                            Helpers.adjustSpeed();
                                        } catch (MessagingException e) {
                                            log.debug("MessagingException", e);
                                        } catch (InterruptedException e) {
                                            throw e;
                                        } catch (Throwable t) {
                                            log.debug("Throwable", t);
                                        }
                                    }
                                    successFetch = true;
                                } catch (InterruptedException e) {
                                    throw e;
                                } catch (MailConnectException e) {
                                    log.debug("MailConnectException", e);
                                    Helpers.incProxyErrorCount(SocksManager.get().getCurrentProxy());
                                    SocksManager.get().nextProxy();
                                } catch (Throwable t) {
                                    log.debug("Throwable", t);
                                }
                            }
                            lastTimeFetch = System.currentTimeMillis();
                        }
                        log.debug("Access to the mailbox is unlocked");
                        MainFrame.get().getYellowLabel().setVisible(false);
                    }
                } catch (InterruptedException e) {
                    throw e;
                } catch (Throwable t) {
                    log.debug("Throwable", t);
                }
                checkForWait();
                Thread.sleep(1000);
            }
            //threadFinishStatus = ThreadFinishStatus.COMPLETED;
        } catch (InterruptedException e) {
            log.debug("Stopping Main thread");
            threadFinishStatus = ThreadFinishStatus.INTERRUPTED;
        } catch (Throwable t) {
            log.debug("Throwable", t);
            throwable = t;
        } finally {
            log.debug("Processing completed");
            MainFrame.get().resetButtonState();
        }
    }

    private void processRepeatedMail(Message repeatedMessage) throws InterruptedException {
        try {
            String text = EmailUtils.getTextFromMessage(repeatedMessage, true);
            String[] strings = StringUtils.split(text, "\n");
            Date date1 = null, date2 = null;
            List<String> excludes = new ArrayList<>();
            for (String s : strings) {
                s = StringUtils.stripStart(s, "\t\n\r ");
                if (StringUtils.isBlank(s)) {
                    continue;
                }
                if (date1 == null) {
                    date1 = Helpers.parseDate(StringUtils.trim(s));
                } else if (date2 == null) {
                    date2 = Helpers.parseDate(StringUtils.trim(s));
                } else if (StringUtils.startsWithIgnoreCase(s, "-- ")) {
                    break;
                } else {
                    excludes.add(StringUtils.lowerCase(StringUtils.trim(s)));
                }
            }
            ru.mailhandler.settings.Folder folder = new ru.mailhandler.settings.Folder("Repeated", null, false, false);
            Set<String> sents = new LinkedHashSet<>();
            List<SentMessage> sentMessages = Database.get().getSentMessageDao().getSentMessages(date1, date2);
            for (SentMessage sentMessage : sentMessages) {
                if (!sents.add(sentMessage.getEmail())) {
                    continue;
                }
                if (excludes.contains(StringUtils.lowerCase(sentMessage.getEmail()))) {
                    log.debug("Skip email " + sentMessage.getEmail());
                    continue;
                }
                long sendingTime = System.currentTimeMillis();
                String subject = SubjectManager.get().getRandomSubject(folder);
                if (subject == null) {
                    log.debug("Email subject not found. The message will not be sent.");
                    return;
                }
                String body = BodyManager.get().getRandomBody(folder);
                if (body == null) {
                    log.debug("Email body not found. The message will not be sent.");
                    return;
                }

                List<String> attachmentFilenames = new ArrayList<>();
                List<String> bodyLines = new ArrayList<>();
                {
                    for (String s : StringUtils.split(body, "\n")) {
                        if (StringUtils.startsWith(StringUtils.trim(s), "#")) {
                            s = StringUtils.substringAfter(s, "#");
                            for (String filename : StringUtils.split(s, ":")) {
                                filename = ConfigurationUtils.getCurrentDirectory() + "folders/" + folder.getTitle() + "/" + filename;
                                attachmentFilenames.add(filename);
                            }
                        } else {
                            bodyLines.add(s);
                        }
                    }
                }

                TemplateManager templateManager = new TemplateManager();
                String join = StringUtils.join(bodyLines, "\n");
                String template = "Dear %OFromFName,\n<br/>\n%Cursor";
                body = templateManager.format(template, sentMessage.getName(), null, "", join);

                //body = body.replace("\n", "\n<br/>");
                //body = body.replace("\r", "");

                //String sign = MisterParserFileUtils.readFileToStringQuietly(new File(ConfigurationUtils.getCurrentDirectory() + "sign.html"), "UTF-8");
                //if (StringUtils.isNotBlank(sign)) {
                //    body += "\n<br/>-- \n<br/>";
                //    body += sign;
                //}
                ScheduledMessage scheduledMessage = new ScheduledMessage(sentMessage.getEmail(), sentMessage.getName(), subject, body, StringUtils.join(attachmentFilenames, ":"), folder.getTitle(), sendingTime, templateManager.getPriority(Settings.get().MESSAGE_TEMPLATE));
                try {
                    Database.get().getScheduledMessageDao().create(scheduledMessage);
                    log.debug("Message scheduled: " + DateFormatUtils.format(sendingTime, "yyyy-MM-dd HH:mm:ss"));
                    sents.add(sentMessage.getEmail());
                } catch (SQLException e) {
                    log.debug("SQLException", e);
                }
                Thread.sleep(1);
            }
        } catch (InterruptedException e) {
            throw e;
        } catch (Throwable t) {
            log.debug("Throwable", t);
        }
    }

    private boolean repeatedMail(Message message) throws MessagingException {
        if (StringUtils.isNotBlank(message.getSubject())) {
            return StringUtils.equalsIgnoreCase(message.getSubject(), Settings.get().REPEATED_SUBJECT);
        } else {
            return false;
        }
    }

    private synchronized void sendAdminStats() {
        try {
            CustomProxySelector.setNoProxy(true);
            FetchEmailIntervalStats fetchEmailIntervalStats = MainFrame.get().getFetchEmailIntervalStats();
            fetchEmailIntervalStats.setDate2(new Date());
            MainFrame.get().setFetchEmailIntervalStats(new FetchEmailIntervalStats());

            Map<String, Object> parameters = new LinkedHashMap<>();
            parameters.put("date1", Helpers.formatDate(fetchEmailIntervalStats.getDate1().getTime()));
            parameters.put("date2", Helpers.formatDate(fetchEmailIntervalStats.getDate2().getTime()));
            parameters.put("countReceived", fetchEmailIntervalStats.getCountReceived());
            parameters.put("countCanceled", fetchEmailIntervalStats.getCountCanceled());
            parameters.put("sentByCategories", fetchEmailIntervalStats.getSentByCategories());

            List<SentMessage> sentMessages = Database.get().getSentMessageDao().getSentMessages(fetchEmailIntervalStats.getDate1(), fetchEmailIntervalStats.getDate2());
            parameters.put("sentMessages", sentMessages);

            final StringWriter stringWriter = new StringWriter();
            Engine engine = Engine.getEngine();
            Template template = engine.getTemplate("/fetch_email_interval_stats.httl");
            template.render(parameters, stringWriter);

            String body = stringWriter.toString();

            log.debug("Send admin stats on last " + Settings.get().FETCH_EMAIL_INTERVAL / 1000 + " seconds on email: " + Settings.get().ADMIN_EMAIL);

            Properties properties = System.getProperties();
            properties.setProperty("mail.smtp.connectionpooltimeout", String.valueOf(Settings.get().SMTP_MAILBOX_CONNECTION_TIMEOUT));
            properties.setProperty("mail.smtp.connectiontimeout", String.valueOf(Settings.get().SMTP_MAILBOX_CONNECTION_TIMEOUT));
            properties.setProperty("mail.smtp.timeout", String.valueOf(Settings.get().SMTP_MAILBOX_CONNECTION_TIMEOUT));
            properties.setProperty("mail.smtp.host", Settings.get().SMTP_HOST);
            properties.setProperty("mail.smtp.port", String.valueOf(Settings.get().SMTP_PORT));
            properties.setProperty("mail.smtp.auth", "true");

            if (Settings.get().SMTP_SSL) {
                properties.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
                properties.put("mail.smtp.socketFactory.fallback", "false");
            }

            Session session = Session.getInstance(properties, new javax.mail.Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(Settings.get().SMTP_EMAIL_LOGIN, Settings.get().SMTP_EMAIL_PASSWORD);
                }
            });
            MimeMessage mimeMessage = new MimeMessage(session);
            mimeMessage.setFrom(new InternetAddress(Settings.get().SMTP_EMAIL_LOGIN, Settings.get().SENDER_NAME));
            mimeMessage.addRecipient(Message.RecipientType.TO, new InternetAddress(Settings.get().ADMIN_EMAIL));
            mimeMessage.setSubject("Statistics Handler from " + Helpers.formatDate(fetchEmailIntervalStats.getDate1().getTime()) + " to " + Helpers.formatDate(fetchEmailIntervalStats.getDate2().getTime()), "UTF-8");

            Multipart multipart = new MimeMultipart();
            {
                MimeBodyPart messageBodyPart = new MimeBodyPart();
                messageBodyPart.setContent(body, "text/html; charset=utf-8");
                multipart.addBodyPart(messageBodyPart);
            }
            {
                MimeBodyPart messageBodyPart = new MimeBodyPart();
                messageBodyPart.setDataHandler(new DataHandler(getXlsxByteArray(), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
                messageBodyPart.setFileName("resumeDataList.xlsx");
                multipart.addBodyPart(messageBodyPart);
            }
            mimeMessage.setContent(multipart);

            Transport.send(mimeMessage);
            log.debug("Admin stats sent");
            resumeDataList.clear();
        } catch (Throwable t) {
            log.debug("Throwable", t);
        } finally {
            CustomProxySelector.setNoProxy(false);
        }
    }

    private Object getXlsxByteArray() {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Лист1");
            int rowIndex = 0;
            List<ResumeData> list = new ArrayList<>(resumeDataList);
            list.add(0, new ResumeData("First name", "Last name", "Email", "Phone", "Location"));
            for (ResumeData resumeData : list) {
                Row row = sheet.createRow(rowIndex++);
                {
                    Cell cell = row.getCell(0, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                    cell.setCellValue(resumeData.getFirstName());
                }
                {
                    Cell cell = row.getCell(1, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                    cell.setCellValue(resumeData.getLastName());
                }
                {
                    Cell cell = row.getCell(2, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                    cell.setCellValue(resumeData.getLocation());
                }
                {
                    Cell cell = row.getCell(3, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                    cell.setCellValue(resumeData.getEmail());
                }
                {
                    Cell cell = row.getCell(4, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                    cell.setCellValue(resumeData.getPhone());
                }
            }
            Row row0 = sheet.getRow(0);
            Cell cell = row0.getCell(0);
            XSSFFont original = ((XSSFWorkbook) workbook).getFontAt(cell.getCellStyle().getFontIndex());
            XSSFFont font = (XSSFFont) workbook.createFont();
            font.setFontHeightInPoints(original.getFontHeightInPoints());
            font.setFontName(original.getFontName());
            font.setColor(original.getColor());
            font.setBold(true);
            font.setItalic(original.getItalic());
            for (Cell c : row0) {
                c.getCellStyle().setFont(font);
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.debug("Exception", e);
            return null;
        }
    }

    private boolean checkUndelivery(Message message) throws MessagingException {
        if (StringUtils.containsIgnoreCase(message.getSubject(), "Delivery Status Notification (Failure)") ||
                StringUtils.containsIgnoreCase(message.getSubject(), "Undelivered Mail Returned to Sender") ||
                StringUtils.containsIgnoreCase(message.getSubject(), "Mail delivery failed")) {
            log.debug("Message with return info");
            String email = EmailForResponseFinder.get().getEmail(message);
            log.debug("Email: " + email);
            Stats.get().incUndeliveredToday();
            return true;
        }
        return false;
    }

    private void scheduleMail(Message message, Folder folder, String formOfAddress, String location) throws IOException, MessagingException {
        String to = EmailForResponseFinder.get().getEmail(message);
        if (to == null) {
            log.debug("Email sender not found. The message will not be sent.");
            return;
        }
        String subject = SubjectManager.get().getRandomSubject(folder);
        if (subject == null) {
            log.debug("Email subject not found. The message will not be sent.");
            return;
        }
        String body = BodyManager.get().getRandomBody(folder);
        if (body == null) {
            log.debug("Email body not found. The message will not be sent.");
            return;
        }
        List<String> attachmentFilenames = new ArrayList<>();
        List<String> bodyLines = new ArrayList<>();
        {
            for (String s : StringUtils.split(body, "\n")) {
                if (StringUtils.startsWith(StringUtils.trim(s), "#")) {
                    s = StringUtils.substringAfter(s, "#");
                    for (String filename : StringUtils.split(s, ":")) {
                        filename = ConfigurationUtils.getCurrentDirectory() + "folders/" + folder.getTitle() + "/" + filename;
                        attachmentFilenames.add(filename);
                        log.debug("Added attachment: " + filename);
                    }
                } else {
                    bodyLines.add(s);
                }
            }
        }
        String textFromMessage = EmailUtils.getTextFromMessage(message, true);
        if (message.isMimeType("text/plain")) {
            textFromMessage = textFromMessage.replace("\n", "\n<br/>");
        }

        TemplateManager templateManager = new TemplateManager();
        Date sentDate = message.getSentDate();
        if (sentDate == null) {
            sentDate = DateUtils.parseDate(message.getHeader("Delivery-Date")[0]);
        }
        String join = StringUtils.join(bodyLines, "\n");
        body = templateManager.format(Settings.get().MESSAGE_TEMPLATE, formOfAddress, sentDate, textFromMessage, join);
        body = StringUtils.replace(body, "your location", location);

        //body = body.replace("\n", "\n<br/>");
        //body = body.replace("\r", "");

        //String sign = MisterParserFileUtils.readFileToStringQuietly(new File(ConfigurationUtils.getCurrentDirectory() + "sign.html"), "UTF-8");
        //if (StringUtils.isNotBlank(sign)) {
        //    body += "\n<br/>-- \n<br/>";
        //    body += sign;
        //}
        long sendingTime = System.currentTimeMillis() + Settings.get().SENDING_DELAY;
        ScheduledMessage scheduledMessage = new ScheduledMessage(to, formOfAddress, subject, body, StringUtils.join(attachmentFilenames, ":"), folder.getTitle(), sendingTime, templateManager.getPriority(Settings.get().MESSAGE_TEMPLATE));
        try {
            Database.get().getScheduledMessageDao().create(scheduledMessage);
            log.debug("Message scheduled: " + DateFormatUtils.format(sendingTime, "yyyy-MM-dd HH:mm:ss"));
        } catch (SQLException e) {
            log.debug("SQLException", e);
        }
    }

    private String getFormOfAddress(Message message) {
        String result = "";
        try {
            Address[] address = message.getFrom();
            if (address.length > 0) {
                String email = address[0].toString();
                String phrase = StringUtils.substringBefore(email, "<");
                phrase = MimeUtility.decodeText(phrase);
                phrase = StringUtils.trim(phrase);
                phrase = StringUtils.strip(phrase, "\"");
                phrase = StringUtils.substringBefore(phrase, " ");
                phrase = StringUtils.substringBefore(phrase, "@");
                if (StringUtils.length(phrase) > 1) {
                    result = WordUtils.capitalize(phrase);
                }
            }
        } catch (MessagingException e) {
            log.debug("MessagingException", e);
        } catch (UnsupportedEncodingException e) {
            log.debug("UnsupportedEncodingException", e);
        }
        log.debug("Found form of address: " + result);
        return result;
    }
}
