package ru.mailhandler.filter;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.mailhandler.EmailForResponseFinder;
import ru.mailhandler.Helpers;
import ru.mailhandler.db.Database;
import ru.mailhandler.settings.Settings;
import ru.mailhandler.model.ResponseTime;

import javax.mail.Message;
import javax.mail.MessagingException;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Stas
 * Date: 18.12.16
 * Time: 11:34
 */
public class ResponseTimeFilter {

    private static final Logger log = LogManager.getLogger(ResponseTimeFilter.class);

    public void filter(List<Message> messages) {
        if (Settings.get().RESPONSE_TIME_FILTER) {
            log.debug("Response time filter");
            log.debug("Message count before: " + messages.size());
            for (Iterator<Message> iterator = messages.iterator(); iterator.hasNext(); ) {
                try {
                    Message message = iterator.next();
                    String email = EmailForResponseFinder.get().getEmail(message);
                    //email = "nishatansaribrown22_abc@indeedemail.com";
                    if (email != null) {
                        long minResponseTime = System.currentTimeMillis() - Settings.get().MIN_RESPONSE_INTERVAL;
                        List<ResponseTime> responseTimes = Database.get().getResponseTimeDao().queryBuilder().where().gt("time", minResponseTime).query();
                        log.debug("Total recent emails: " + responseTimes.size());
                        filterByFirstAndLast(responseTimes, email);
                        if (responseTimes.size() > 0) {
                            for (ResponseTime responseTime : responseTimes) {
                                log.debug("Email: " + email + ", sent last time: " + Helpers.formatDate(responseTime.getTime()));
                            }
                            log.debug("Message decline by sent time and similar address");
                            iterator.remove();
                        } else {
                            log.debug("On email " + email + " yet not sent");
                        }
                    } else {
                        log.debug("Delete message without email for response, message subject: " + message.getSubject());
                    }
                } catch (SQLException e) {
                    log.debug("SQLException", e);
                } catch (MessagingException e) {
                    log.debug("MessagingException", e);
                }
            }
            log.debug("Message count after: " + messages.size());
        } else {
            log.debug("Disable Response time filter");
        }
    }

    private void filterByFirstAndLast(List<ResponseTime> responseTimes, String email) {
        log.debug("Checking recent responses to match the address: " + email);
        log.debug("Old recent responses: " + responseTimes.size());
        responseTimes.removeIf(responseTime -> !compareEmail(responseTime.getEmail(), email));
        log.debug("New recent responses: " + responseTimes.size());
    }

    private static boolean compareEmail(String email1, String email2) {
        String s1 = StringUtils.substringBefore(email1, "@");
        String s2 = StringUtils.substringBefore(email2, "@");
        String s11 = StringUtils.substring(s1, 0, Settings.get().MIN_RESPONSE_INTERVAL_FIRST);
        String s21 = StringUtils.substring(s2, 0, Settings.get().MIN_RESPONSE_INTERVAL_FIRST);
        if (StringUtils.equalsIgnoreCase(s11, s21)) {
            String s111 = StringUtils.substring(s1, 0, -Settings.get().MIN_RESPONSE_INTERVAL_LAST);
            String s211 = StringUtils.substring(s2, 0, -Settings.get().MIN_RESPONSE_INTERVAL_LAST);
            return StringUtils.equalsIgnoreCase(s111, s211);
        }
        return false;
    }

    public static void main(String[] args) {
        System.out.println(compareEmail("nishatansaribrown55_znr@indeedemail.com", "nishatansaribrown22_abc@indeedemail.com"));
        System.out.println(compareEmail("nishatansa5ribrown55_znr@indeedemail.com", "nishatansaribrown22_abc@indeedemail.com"));
    }
}
