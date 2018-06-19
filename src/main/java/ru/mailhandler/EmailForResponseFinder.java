package ru.mailhandler;

import org.apache.commons.collections4.set.ListOrderedSet;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.kahadb.util.LRUCache;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.io.RandomAccessBuffer;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.microsoft.OfficeParser;
import org.apache.tika.parser.microsoft.ooxml.OOXMLParser;
import org.apache.tika.sax.BodyContentHandler;
import ru.misterparser.common.mail.EmailUtils;
import ru.mailhandler.settings.Settings;

import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.MimeUtility;
import javax.swing.text.Document;
import javax.swing.text.rtf.RTFEditorKit;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Stas
 * Date: 18.12.16
 * Time: 11:07
 */
public class EmailForResponseFinder {

    private static final Logger log = LogManager.getLogger(EmailForResponseFinder.class);

    private static final EmailForResponseFinder emailForResponseFinder = new EmailForResponseFinder();

    public static EmailForResponseFinder get() {
        return emailForResponseFinder;
    }

    private EmailForResponseFinder() {
    }

    private LRUCache<Message, String> cache = new LRUCache<>(100);

    public String getEmail(Message message) {
        try {
            log.debug("Search email for message with subject: " + message.getSubject());
            String email = cache.get(message);
            if (email == null) {
                List<Attachment> attachments = new ArrayList<>();
                if (!Settings.get().DISABLE_EMAIL_SEARCH_IN_ATTACHMENTS) {
                    attachments.addAll(getAttachments(message));
                }
                if (!Settings.get().DISABLE_EMAIL_SEARCH_IN_BODY) {
                    attachments.add(new Attachment(null, EmailUtils.getTextFromMessage(message, false).getBytes()));
                }
                //log.debug("Сортировка вложений для поиска адреса для ответа...");
                Collections.sort(attachments, new AttachmentComparator());
                //log.debug("Результат сортировки вложений: " + attachments);
                for (Attachment attachment : attachments) {
                    if (attachment.filename != null) {
                        log.debug("Search in attachment: " + attachment.filename);
                    } else {
                        log.debug("Search in message body...");
                    }
                    email = getEmailFromAttachment(attachment);
                    // если нашли, либо дошли до текста письма, то заканчиваем
                    if (StringUtils.isNotBlank(email) || attachment.filename == null) {
                        break;
                    }
                }
                if (StringUtils.isBlank(email) && !Settings.get().DISABLE_EMAIL_SEARCH_IN_FROM) {
                    log.debug("Take message From header as email...");
                    Address[] address = message.getFrom();
                    if (address.length > 0) {
                        ListOrderedSet<String> emails = new EmailExtractor().getEmails(address[0].toString());
                        if (emails.size() > 0) {
                            email = emails.get(0);
                        }
                    }
                }

                if (StringUtils.isNotBlank(email)) {
                    log.debug("From message extracted email for response: " + email);
                    cache.put(message, email);
                } else {
                    log.debug("Not found email in message");
                }
            } else {
                log.debug("Email take from cache: " + email);
            }
            return email;
        } catch (MessagingException e) {
            log.debug("MessagingException", e);
        } catch (IOException e) {
            log.debug("IOException", e);
        }
        return null;
    }

    private String getEmailFromAttachment(Attachment attachment) {
        EmailExtractor emailExtractor = new EmailExtractor();
        ListOrderedSet<String> emails = new ListOrderedSet<>();
        if (attachment.filename == null) {
            emails = emailExtractor.getEmails(new String(attachment.data));
            log.debug("In message body found emails: " + emails);
        } else if (StringUtils.endsWithIgnoreCase(attachment.filename, "pdf")) {
            try {
                PDFParser parser = new PDFParser(new RandomAccessBuffer(attachment.data));
                parser.parse();
                COSDocument cosDocument = parser.getDocument();
                PDFTextStripper pdfTextStripper = new PDFTextStripper();
                PDDocument pdDocument = new PDDocument(cosDocument);
                String parsedText = pdfTextStripper.getText(pdDocument);
                pdDocument.close();
                cosDocument.close();
                emails = emailExtractor.getEmails(parsedText);
                log.debug("In file " + attachment.filename + " found emails: " + emails);
            } catch (Exception e) {
                log.debug("Exception", e);
            }
        } else if (StringUtils.endsWithIgnoreCase(attachment.filename, "txt") ||
                StringUtils.endsWithIgnoreCase(attachment.filename, "html") ||
                StringUtils.endsWithIgnoreCase(attachment.filename, "htm")) {
            emails = emailExtractor.getEmails(new String(attachment.data));
            log.debug("In file " + attachment.filename + " found emails: " + emails);
        } else if (StringUtils.endsWithIgnoreCase(attachment.filename, "doc")) {
            try {
                BodyContentHandler handler = new BodyContentHandler();
                Metadata metadata = new Metadata();
                TikaInputStream stream = TikaInputStream.get(attachment.data);
                ParseContext parseContext = new ParseContext();
                OfficeParser parser = new OfficeParser();
                parser.parse(stream, handler, metadata, parseContext);
                String text = handler.toString();
                emails = emailExtractor.getEmails(text);
                log.debug("In file " + attachment.filename + " found emails: " + emails);
            } catch (Exception e) {
                log.debug("Exception", e);
            }
        } else if (StringUtils.endsWithIgnoreCase(attachment.filename, "docx")) {
            try {
                BodyContentHandler handler = new BodyContentHandler();
                Metadata metadata = new Metadata();
                TikaInputStream stream = TikaInputStream.get(attachment.data);
                ParseContext parseContext = new ParseContext();
                OOXMLParser parser = new OOXMLParser();
                parser.parse(stream, handler, metadata, parseContext);
                String text = handler.toString();
                emails = emailExtractor.getEmails(text);
                log.debug("In file " + attachment.filename + " found emails: " + emails);
            } catch (Exception e) {
                log.debug("Exception", e);
            }
        } else if (StringUtils.endsWithIgnoreCase(attachment.filename, "xls")) {
            try {
                BodyContentHandler handler = new BodyContentHandler();
                Metadata metadata = new Metadata();
                TikaInputStream stream = TikaInputStream.get(attachment.data);
                ParseContext parseContext = new ParseContext();
                OfficeParser parser = new OfficeParser();
                parser.parse(stream, handler, metadata, parseContext);
                String text = handler.toString();
                emails = emailExtractor.getEmails(text);
                log.debug("In file " + attachment.filename + " found emails: " + emails);
            } catch (Exception e) {
                log.debug("Exception", e);
            }
        } else if (StringUtils.endsWithIgnoreCase(attachment.filename, "xlsx")) {
            try {
                BodyContentHandler handler = new BodyContentHandler();
                Metadata metadata = new Metadata();
                TikaInputStream stream = TikaInputStream.get(attachment.data);
                ParseContext parseContext = new ParseContext();
                OOXMLParser parser = new OOXMLParser();
                parser.parse(stream, handler, metadata, parseContext);
                String text = handler.toString();
                emails = emailExtractor.getEmails(text);
                log.debug("In file " + attachment.filename + " found emails: " + emails);
            } catch (Exception e) {
                log.debug("Exception", e);
            }
        } else if (StringUtils.endsWithIgnoreCase(attachment.filename, "rtf")) {
            try {
                RTFEditorKit rtfParser = new RTFEditorKit();
                Document document = rtfParser.createDefaultDocument();
                rtfParser.read(new ByteArrayInputStream(attachment.data), document, 0);
                String text = document.getText(0, document.getLength());
                emails = emailExtractor.getEmails(text);
                log.debug("In file " + attachment.filename + " found emails: " + emails);
            } catch (Exception e) {
                log.debug("Exception", e);
            }
        }
        if (emails.size() > 0) {
            for (String email : emails) {
                if (!StringUtils.equalsIgnoreCase(email, Settings.get().POP3_EMAIL_LOGIN)) {
                    return emails.get(0);
                }
            }
            return null;
        } else {
            return null;
        }
    }

    private List<Attachment> getAttachments(Message message) throws IOException, MessagingException {
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
                    for (String ext : Settings.get().EMAIL_FOR_RESPONSE_FINDER_EXTENSIONS) {
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
                attachment.filename = MimeUtility.decodeText(bodyPart.getFileName());
                attachment.data = IOUtils.toByteArray(is);
                attachments.add(attachment);
            }
        }
        return attachments;
    }

    private static class Attachment {

        private String filename;
        private byte[] data;

        private Attachment() {
        }

        private Attachment(String filename, byte[] data) {
            this.filename = filename;
            this.data = data;
        }

        @Override
        public String toString() {
            return "Attachment{" +
                    "filename='" + filename + '\'' +
                    '}';
        }
    }

    private static class AttachmentComparator implements Comparator<Attachment> {
        @Override
        public int compare(Attachment o1, Attachment o2) {
            int i1 = Integer.MAX_VALUE;
            int i2 = Integer.MAX_VALUE;
            for (int i = 0; i < Settings.get().EMAIL_FOR_RESPONSE_FINDER_FILENAME_TAGS.size(); i++) {
                String tag = Settings.get().EMAIL_FOR_RESPONSE_FINDER_FILENAME_TAGS.get(i);
                if (StringUtils.containsIgnoreCase(o1.filename, tag)) {
                    i1 = i + 1;
                }
                if (StringUtils.containsIgnoreCase(o2.filename, tag)) {
                    i2 = i + 1;
                }
            }
            for (int i = 0; i < Settings.get().EMAIL_FOR_RESPONSE_FINDER_EXTENSIONS.size(); i++) {
                String ext = Settings.get().EMAIL_FOR_RESPONSE_FINDER_EXTENSIONS.get(i);
                if (i1 == Integer.MAX_VALUE && StringUtils.endsWithIgnoreCase(o1.filename, ext)) {
                    i1 = 100 * (i + 1);
                }
                if (i2 == Integer.MAX_VALUE && StringUtils.endsWithIgnoreCase(o2.filename, ext)) {
                    i2 = 100 * (i + 1);
                }
            }
            if (i1 == Integer.MAX_VALUE && o1.filename == null) {
                i1 = 1001;
            }
            if (i2 == Integer.MAX_VALUE && o2.filename == null) {
                i2 = 1001;
            }
            //System.out.println(o1.filename + "=" + i1 + "\t\t\t" + o2.filename + "=" + i2);
            return i1 - i2;
        }
    }

    public static void main(String[] args) {
        List<Attachment> attachments = new ArrayList<>();
        Settings.get().EMAIL_FOR_RESPONSE_FINDER_EXTENSIONS = Arrays.asList("pdf, xlsx");
        Settings.get().EMAIL_FOR_RESPONSE_FINDER_FILENAME_TAGS = Arrays.asList("cover", "resume");
        System.out.println(Settings.get().EMAIL_FOR_RESPONSE_FINDER_EXTENSIONS);
        attachments.add(new Attachment("resume.xlsx", null));
        attachments.add(new Attachment("test.pdf", null));
        attachments.add(new Attachment("hello.cpp", null));
        attachments.add(new Attachment(null, null));
        attachments.add(new Attachment("cover.doc", null));
        Collections.sort(attachments, new AttachmentComparator());
        System.out.println(attachments);
    }
}
