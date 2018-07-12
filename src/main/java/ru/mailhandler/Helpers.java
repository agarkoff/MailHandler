package ru.mailhandler;

import org.apache.commons.collections4.set.ListOrderedSet;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.io.RandomAccessBuffer;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.detect.Detector;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.javatuples.Pair;
import org.sqlite.date.DateFormatUtils;
import ru.mailhandler.db.Database;
import ru.mailhandler.model.Attachment;
import ru.mailhandler.model.Proxy;
import ru.mailhandler.model.ResumeData;
import ru.mailhandler.settings.Settings;
import ru.mailhandler.stat.Stats;

import javax.mail.*;
import javax.mail.internet.*;
import javax.swing.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: Stas
 * Date: 20.12.16
 * Time: 21:30
 */
public class Helpers {

    private static final Logger log = LogManager.getLogger(Helpers.class);

    public static String formatDate(Long time) {
        return DateFormatUtils.format(time, "yyyy-MM-dd HH:mm:ss");
    }

    private static TreeSet<String> cityNames = new TreeSet<>();

    static {
        try {
            List<String> strings = IOUtils.readLines(Helpers.class.getClassLoader().getResourceAsStream("cityNames.csv"), StandardCharsets.UTF_8);
            for (String s : strings) {
                s = StringUtils.lowerCase(s);
                cityNames.add(s);
            }
        } catch (Exception e) {
            log.debug("Exception", e);
        }
        log.debug("Loaded city names: " + cityNames.size());
    }

    public static String getMimeType(String filename) {
        try {
            TikaConfig config = TikaConfig.getDefaultConfig();
            Detector detector = config.getDetector();
            Path path = FileSystems.getDefault().getPath(filename);
            TikaInputStream stream = TikaInputStream.get(path);
            Metadata metadata = new Metadata();
            MediaType mediaType = detector.detect(stream, metadata);
            return mediaType.toString();
        } catch (Throwable t) {
            log.debug("Throwable", t);
            return null;
        }
    }

    public synchronized static void sendEmailToAdmin(String subject, String text) {
        try {
            CustomProxySelector.setNoProxy(true);
            log.debug("Send email to admin...");
            log.debug("Connect to mailbox: " + Settings.get().SMTP_EMAIL_LOGIN);

            Properties properties = System.getProperties();
            properties.setProperty("mail.smtp.host", Settings.get().SMTP_HOST);
            properties.setProperty("mail.smtp.port", String.valueOf(Settings.get().SMTP_PORT));
            properties.setProperty("mail.smtp.auth", "true");

            Session session = Session.getInstance(properties, new javax.mail.Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(Settings.get().SMTP_EMAIL_LOGIN, Settings.get().SMTP_EMAIL_PASSWORD);
                }
            });
            MimeMessage mimeMessage = new MimeMessage(session);
            mimeMessage.setFrom(new InternetAddress(Settings.get().SMTP_EMAIL_LOGIN, Settings.get().SENDER_NAME));
            mimeMessage.addRecipient(Message.RecipientType.TO, new InternetAddress(Settings.get().ADMIN_EMAIL));
            mimeMessage.setSubject(subject, "UTF-8");

            Multipart multipart = new MimeMultipart();
            {
                MimeBodyPart messageBodyPart = new MimeBodyPart();
                messageBodyPart.setText(text, "UTF-8", "plain");
                multipart.addBodyPart(messageBodyPart);
            }
            mimeMessage.setContent(multipart);

            Transport.send(mimeMessage);
            log.debug("Mail sended");
        } catch (Throwable t) {
            log.debug("Throwable", t);
        } finally {
            CustomProxySelector.setNoProxy(false);
        }
    }

    public static String getStackTrace(Throwable t) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        t.printStackTrace(printWriter);
        return stringWriter.toString();
    }

    public static Proxy parseProxy(String proxyString) {
        Proxy proxy = null;
        String[] chunks = proxyString.split(":");
        if (chunks.length == 2) {
            proxy = new Proxy(chunks[0], Integer.valueOf(chunks[1]), null, null, System.currentTimeMillis());
        } else {
            log.debug("Cannot parse proxy from string: " + proxyString);
        }
        return proxy;
    }

    public static void incProxyErrorCount(Proxy proxy) throws SQLException {
        if (proxy != null) {
            if (proxy.incErrorCount() < Settings.get().PROXY_MAX_ERROR_COUNT) {
                Database.get().getProxyDao().update(proxy);
                log.debug("Proxy error counter increased: " + proxy);
            } else {
                deleteProxy(proxy);
            }
        }
        SocksManager.get().resetCurrentProxy();
    }

    public static void deleteProxy(Proxy proxy) throws SQLException {
        if (Database.get().getProxyDao().delete(proxy) == 1) {
            log.debug("Proxy " + proxy + " deleted");
            Stats.get().incUsedProxyToday();
        } else {
            log.debug("Error on proxy delete: " + proxy);
        }
    }

    public static boolean workTime() {
        Calendar calendar = Calendar.getInstance();
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        Pair<Long, Long> pair = null;
        switch (dayOfWeek) {
            case Calendar.MONDAY:
                pair = Settings.get().MONDAY_CLOCK;
                break;
            case Calendar.TUESDAY:
                pair = Settings.get().TUESDAY_CLOCK;
                break;
            case Calendar.WEDNESDAY:
                pair = Settings.get().WEDNESDAY_CLOCK;
                break;
            case Calendar.THURSDAY:
                pair = Settings.get().THURSDAY_CLOCK;
                break;
            case Calendar.FRIDAY:
                pair = Settings.get().FRIDAY_CLOCK;
                break;
            case Calendar.SATURDAY:
                pair = Settings.get().SATURDAY_CLOCK;
                break;
            case Calendar.SUNDAY:
                pair = Settings.get().SUNDAY_CLOCK;
                break;
            default:
                throw new RuntimeException("Неизвестный день недели!");
        }
        if (pair != null) {
            String s = DateFormatUtils.format(calendar, "HH:mm");
            long l = parseTime(s);
            return l >= pair.getValue0() && l <= pair.getValue1();
        } else {
            return true;
        }
    }

    public static long parseTime(String s) {
        String[] split = StringUtils.split(s, ":");
        int h = Integer.parseInt(split[0]);
        int m = Integer.parseInt(split[1]);
        return (h * 60 + m) * DateUtils.MILLIS_PER_MINUTE;
    }

    public static String formatTime(long value) {
        value /= DateUtils.MILLIS_PER_MINUTE;
        long h = value / 60;
        long m = value % 60;
        return String.format("%02d", h) + ":" + String.format("%02d", m);
    }

    public static void adjustSpeed() throws InterruptedException {
        if (Settings.get().SPEED > 0) {
            log.debug("Adjust speed, pause: " + Settings.get().SPEED + " ms");
            Thread.sleep(Settings.get().SPEED);
        }
    }

    public static Date parseDate(String string) {
        Date date = null;
        try {
            return DateUtils.parseDate(string, new String[]{"dd.MM.yyyy HH:mm", "yyyy-MM-dd HH:mm:ss"});
        } catch (Throwable t) {
            log.debug("Throwable", t);
        }
        return date;
    }

    public static Locale findLocaleByName(String localeName) {
        Locale[] availableLocales = Locale.getAvailableLocales();
        for (Locale locale : availableLocales) {
            if (StringUtils.equalsIgnoreCase(locale.getDisplayName(Locale.forLanguageTag("RU")), localeName)) {
                return locale;
            }
        }
        return availableLocales[0];
    }

    public static void updateLocalePreview(JLabel localePreviewLabel) {
        Date date = new Date();
        String dateFormatted = DateFormat.getDateInstance(DateFormat.LONG).format(date);
        String timeFormatted = DateFormat.getTimeInstance(DateFormat.DEFAULT).format(date);
        localePreviewLabel.setText(dateFormatted + " " + timeFormatted);
    }

    public static List<Attachment> getAttachments(Message message) throws IOException, MessagingException {
        List<Attachment> attachments = new ArrayList<>();
        if (message.getContent() instanceof Multipart) {
            Multipart multipart = (Multipart) message.getContent();
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart bodyPart = multipart.getBodyPart(i);
                if (!Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition()) &&
                        !StringUtils.isNotBlank(bodyPart.getFileName())) {
                    continue; // dealing with attachments only
                }
                {
                    boolean f = false;
                    for (String ext: Settings.get().EMAIL_FOR_RESPONSE_FINDER_EXTENSIONS) {
                        if (StringUtils.endsWithIgnoreCase(bodyPart.getFileName(), ext)) {
                            f = true;
                            break;
                        }
                    }
                    if (!f) {
                        log.debug("Skip unknown filename extension in " + bodyPart.getFileName());
                        continue;
                    }
                }
                InputStream is = bodyPart.getInputStream();
                Attachment attachment = new Attachment();
                attachment.setFilename(MimeUtility.decodeText(bodyPart.getFileName()));
                attachment.setData(IOUtils.toByteArray(is));
                attachments.add(attachment);
            }
        }
        return attachments;
    }

    public static ResumeData getResumeData(Message message) {
        ResumeData resumeData = new ResumeData();
        try {
            for (Attachment attachment: getAttachments(message)) {
                if (attachment.getFilename() != null &&
                        StringUtils.endsWithIgnoreCase(attachment.getFilename(), "pdf")) {
                    log.debug("Search resume data in attachment: " + attachment.getFilename());
                    PDFParser parser = new PDFParser(new RandomAccessBuffer(attachment.getData()));
                    parser.parse();
                    String author = parser.getPDDocument().getDocumentInformation().getAuthor();
                    String creator = parser.getPDDocument().getDocumentInformation().getCreator();
                    String producer = parser.getPDDocument().getDocumentInformation().getProducer();
                    String title = parser.getPDDocument().getDocumentInformation().getTitle();
                    String keywords = parser.getPDDocument().getDocumentInformation().getKeywords();
                    if (StringUtils.containsIgnoreCase(author, "Indeed") ||
                            StringUtils.containsIgnoreCase(creator, "Indeed") ||
                            StringUtils.containsIgnoreCase(producer, "Indeed") ||
                            StringUtils.containsIgnoreCase(title, "Indeed") ||
                            StringUtils.containsIgnoreCase(keywords, "Indeed")) {
                        log.debug("Process " + attachment.getFilename() + " Indeed resume...");
                        COSDocument cosDocument = parser.getDocument();
                        PDFTextStripper pdfTextStripper = new PDFTextStripper();
                        PDDocument pdDocument = new PDDocument(cosDocument);
                        String parsedText = pdfTextStripper.getText(pdDocument);
                        String[] ss = StringUtils.split(parsedText, "\n\r");
                        if (ss == null || ss.length == 0) {
                            continue;
                        }
                        {
                            String t0 = ss[0];
                            String[] tt = StringUtils.split(t0);
                            if (tt != null && tt.length > 0) {
                                resumeData.setFirstName(tt[0]);
                            }
                            if (tt != null && tt.length > 1) {
                                resumeData.setLastName(tt[1]);
                            }
                        }
                        int m = Math.min(ss.length, 10);
                        for (int i = 1; i < m; i++) {
                            String s = ss[i];
                            ListOrderedSet<String> emails = new EmailExtractor().getEmails(s);
                            if (emails.size() > 0) {
                                String loc = ss[i-1];
                                loc = StringUtils.substringBefore(loc, ", ");
                                if (cityNames.contains(StringUtils.lowerCase(loc))) {
                                    resumeData.setLocation(loc);
                                }
                                resumeData.setEmail(emails.get(0));
                                for (String email : emails) {
                                    s = StringUtils.replaceOnce(s, email, "");
                                    s = StringUtils.strip(s, "- ");
                                }
                                if (StringUtils.isNotBlank(s)) {
                                    resumeData.setPhone(s);
                                }
                                if (StringUtils.isBlank(resumeData.getPhone()) && i < m - 1) {
                                    String t = ss[i + 1];
                                    if (t.matches("[0-9-\\(\\) ]+")) {
                                        resumeData.setPhone(StringUtils.trim(t));
                                    }
                                }
                                break;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Exception", e);
        }
        if (StringUtils.isBlank(resumeData.getEmail())) {
            log.debug("Failed to extract address from message");
        }
        log.debug("Found usefull resume data: " + resumeData);
        return resumeData;
    }
}
