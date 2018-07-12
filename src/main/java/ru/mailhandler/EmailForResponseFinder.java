package ru.mailhandler;

import org.apache.commons.collections4.set.ListOrderedSet;
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
import ru.mailhandler.model.Attachment;
import ru.mailhandler.settings.Settings;
import ru.misterparser.common.mail.EmailUtils;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.swing.text.Document;
import javax.swing.text.rtf.RTFEditorKit;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
                    attachments.addAll(Helpers.getAttachments(message));
                }
                if (!Settings.get().DISABLE_EMAIL_SEARCH_IN_BODY) {
                    attachments.add(new Attachment(null, EmailUtils.getTextFromMessage(message, false).getBytes()));
                }
                //log.debug("Сортировка вложений для поиска адреса для ответа...");
                Collections.sort(attachments, new AttachmentComparator());
                //log.debug("Результат сортировки вложений: " + attachments);
                for (Attachment attachment : attachments) {
                    if (attachment.getFilename() != null) {
                        log.debug("Search in attachment: " + attachment.getFilename());
                    } else {
                        log.debug("Search in message body...");
                    }
                    email = getEmailFromAttachment(attachment);
                    // если нашли, либо дошли до текста письма, то заканчиваем
                    if (StringUtils.isNotBlank(email) || attachment.getFilename() == null) {
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
        if (attachment.getFilename() == null) {
            emails = emailExtractor.getEmails(new String(attachment.getData()));
            log.debug("In message body found emails: " + emails);
        } else if (StringUtils.endsWithIgnoreCase(attachment.getFilename(), "pdf")) {
            try {
                PDFParser parser = new PDFParser(new RandomAccessBuffer(attachment.getData()));
                parser.parse();
                COSDocument cosDocument = parser.getDocument();
                PDFTextStripper pdfTextStripper = new PDFTextStripper();
                PDDocument pdDocument = new PDDocument(cosDocument);
                String parsedText = pdfTextStripper.getText(pdDocument);
                pdDocument.close();
                cosDocument.close();
                emails = emailExtractor.getEmails(parsedText);
                log.debug("In file " + attachment.getFilename() + " found emails: " + emails);
            } catch (Exception e) {
                log.debug("Exception", e);
            }
        } else if (StringUtils.endsWithIgnoreCase(attachment.getFilename(), "txt") ||
                StringUtils.endsWithIgnoreCase(attachment.getFilename(), "html") ||
                StringUtils.endsWithIgnoreCase(attachment.getFilename(), "htm")) {
            emails = emailExtractor.getEmails(new String(attachment.getData()));
            log.debug("In file " + attachment.getFilename() + " found emails: " + emails);
        } else if (StringUtils.endsWithIgnoreCase(attachment.getFilename(), "doc")) {
            try {
                BodyContentHandler handler = new BodyContentHandler();
                Metadata metadata = new Metadata();
                TikaInputStream stream = TikaInputStream.get(attachment.getData());
                ParseContext parseContext = new ParseContext();
                OfficeParser parser = new OfficeParser();
                parser.parse(stream, handler, metadata, parseContext);
                String text = handler.toString();
                emails = emailExtractor.getEmails(text);
                log.debug("In file " + attachment.getFilename() + " found emails: " + emails);
            } catch (Exception e) {
                log.debug("Exception", e);
            }
        } else if (StringUtils.endsWithIgnoreCase(attachment.getFilename(), "docx")) {
            try {
                BodyContentHandler handler = new BodyContentHandler();
                Metadata metadata = new Metadata();
                TikaInputStream stream = TikaInputStream.get(attachment.getData());
                ParseContext parseContext = new ParseContext();
                OOXMLParser parser = new OOXMLParser();
                parser.parse(stream, handler, metadata, parseContext);
                String text = handler.toString();
                emails = emailExtractor.getEmails(text);
                log.debug("In file " + attachment.getFilename() + " found emails: " + emails);
            } catch (Exception e) {
                log.debug("Exception", e);
            }
        } else if (StringUtils.endsWithIgnoreCase(attachment.getFilename(), "xls")) {
            try {
                BodyContentHandler handler = new BodyContentHandler();
                Metadata metadata = new Metadata();
                TikaInputStream stream = TikaInputStream.get(attachment.getData());
                ParseContext parseContext = new ParseContext();
                OfficeParser parser = new OfficeParser();
                parser.parse(stream, handler, metadata, parseContext);
                String text = handler.toString();
                emails = emailExtractor.getEmails(text);
                log.debug("In file " + attachment.getFilename() + " found emails: " + emails);
            } catch (Exception e) {
                log.debug("Exception", e);
            }
        } else if (StringUtils.endsWithIgnoreCase(attachment.getFilename(), "xlsx")) {
            try {
                BodyContentHandler handler = new BodyContentHandler();
                Metadata metadata = new Metadata();
                TikaInputStream stream = TikaInputStream.get(attachment.getData());
                ParseContext parseContext = new ParseContext();
                OOXMLParser parser = new OOXMLParser();
                parser.parse(stream, handler, metadata, parseContext);
                String text = handler.toString();
                emails = emailExtractor.getEmails(text);
                log.debug("In file " + attachment.getFilename() + " found emails: " + emails);
            } catch (Exception e) {
                log.debug("Exception", e);
            }
        } else if (StringUtils.endsWithIgnoreCase(attachment.getFilename(), "rtf")) {
            try {
                RTFEditorKit rtfParser = new RTFEditorKit();
                Document document = rtfParser.createDefaultDocument();
                rtfParser.read(new ByteArrayInputStream(attachment.getData()), document, 0);
                String text = document.getText(0, document.getLength());
                emails = emailExtractor.getEmails(text);
                log.debug("In file " + attachment.getFilename() + " found emails: " + emails);
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
