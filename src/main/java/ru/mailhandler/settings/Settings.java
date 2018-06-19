package ru.mailhandler.settings;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javatuples.Pair;
import ru.misterparser.common.configuration.ConfigurationUtils;
import ru.misterparser.common.gui.GuiUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: Stas
 * Date: 17.12.16
 * Time: 18:40
 */
public class Settings {

    private static final Logger log = LogManager.getLogger(Settings.class);

    private static Settings settings = new Settings();

    public static Settings get() {
        return settings;
    }

    public static void setSettings(Settings settings) {
        Settings.settings = settings;
    }

    private Settings() {
    }

    public long FETCH_EMAIL_INTERVAL = 2 * DateUtils.MILLIS_PER_MINUTE;
    public String POP3_EMAIL_LOGIN;
    public String POP3_EMAIL_PASSWORD;
    public String POP3_HOST;
    public int POP3_PORT;
    public long POP3_MAILBOX_CONNECTION_TIMEOUT = 10 * DateUtils.MILLIS_PER_SECOND;

    public String SMTP_EMAIL_LOGIN;
    public String SMTP_EMAIL_PASSWORD;
    public String SMTP_HOST;
    public int SMTP_PORT;
    public boolean SMTP_SSL;
    public long SMTP_MAILBOX_CONNECTION_TIMEOUT = 10 * DateUtils.MILLIS_PER_SECOND;
    public String SENDER_FROM;

    public boolean RESPONSE_TIME_FILTER = true;
    public long MIN_RESPONSE_INTERVAL = 15 * DateUtils.MILLIS_PER_SECOND;
    public int MIN_RESPONSE_INTERVAL_FIRST = 8;
    public int MIN_RESPONSE_INTERVAL_LAST = 6;
    public boolean DELETE_BY_SUBJECT_AND_FROM_FILTER = true;
    public List<String> DELETE_BY_SUBJECT_AND_FROM_FILTER_TAGS = new ArrayList<>();
    public boolean ANTI_DELETE_BY_SUBJECT_AND_FROM_FILTER = true;
    public List<String> ANTI_DELETE_BY_SUBJECT_AND_FROM_FILTER_TAGS = new ArrayList<>();
    public Set<Folder> FOLDERS = new LinkedHashSet<>();
    public long SENDING_DELAY = 1 * DateUtils.MILLIS_PER_MINUTE;
    public List<String> EMAIL_FOR_RESPONSE_FINDER_FILENAME_TAGS = new ArrayList<>();
    public List<String> EMAIL_FOR_RESPONSE_FINDER_EXTENSIONS;
    public long PROXY_TTL = 2 * DateUtils.MILLIS_PER_HOUR;
    public String PROXY_PANEL_USER;
    public String PROXY_PANEL_PASS;
    public List<String> PROXY_PANEL_URLS = new ArrayList<>();
    public long MAX_COUNT_PROXY_TODAY = 15;
    public String ADMIN_EMAIL;
    public String MESSAGE_TEMPLATE = "";
    public int PROXY_MAX_ERROR_COUNT = 3;
    public long TIMEOUT_BETWEEN_MESSAGE_SEND = 15 * DateUtils.MILLIS_PER_SECOND;
    public long ONCE_MESSAGE_COUNT = 20;
    public boolean DISABLE_EMAIL_SEARCH_IN_FROM;
    public boolean DISABLE_EMAIL_SEARCH_IN_BODY;
    public boolean DISABLE_EMAIL_SEARCH_IN_ATTACHMENTS;
    public Pair<Long, Long> MONDAY_CLOCK;
    public Pair<Long, Long> TUESDAY_CLOCK;
    public Pair<Long, Long> WEDNESDAY_CLOCK;
    public Pair<Long, Long> THURSDAY_CLOCK;
    public Pair<Long, Long> FRIDAY_CLOCK;
    public Pair<Long, Long> SATURDAY_CLOCK;
    public Pair<Long, Long> SUNDAY_CLOCK;
    public long TODAY_CLOCK;
    public int SOCKS_THRESHOLD;
    public int SPEED;
    public GuiUtils.DirectoryHolder DIRECTORY_HOLDER;
    public String REPEATED_SUBJECT;
    public String LOG_FONT_NAME;
    public String SENDER_NAME;
    public String LOCALE;
    public String REPLY_TO;

    public transient boolean NO_PROXY = false;

    public void save() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String s = gson.toJson(Settings.get());
        try {
            FileUtils.write(new File(ConfigurationUtils.getCurrentDirectory() + "settings.json"), s, "UTF-8");
        } catch (Throwable t) {
            log.debug("Throwable", t);
        }
    }
}
