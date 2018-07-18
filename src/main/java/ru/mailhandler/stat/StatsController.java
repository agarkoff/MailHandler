package ru.mailhandler.stat;

import org.apache.commons.lang3.time.DateUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sqlite.date.DateFormatUtils;
import ru.mailhandler.Helpers;
import ru.mailhandler.settings.Settings;

import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created with IntelliJ IDEA.
 * User: Stas
 * Date: 26.12.16
 * Time: 17:51
 */
public class StatsController {

    private static final Logger log = LogManager.getLogger(StatsController.class);

    private static final StatsController statsController = new StatsController();

    public static StatsController get() {
        return statsController;
    }

    private Timer timer;
    private TimerTask todayCleaner;

    public StatsController() {
        initHourStatsCleaner();
        restartTodayCleaner();
    }

    private void initHourStatsCleaner() {
        int m = Calendar.getInstance().get(Calendar.MINUTE);
        int s = Calendar.getInstance().get(Calendar.SECOND);
        int k = 3600 - m * 60 + s;
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                try {
                    log.debug("Reset hourly stats");
                    Stats.get().resetCountReceivedEmailByHour();
                    Stats.get().resetCountSentEmailByHour();
                    for (String folderName : Stats.get().getCategoryStats().keySet()) {
                        Stats.get().resetCategoryStatsHour(folderName);
                    }
                    Stats.get().updateLastTimeResetHourStats();
                } catch (Throwable t) {
                    log.debug("Throwable", t);
                }
            }
        };
        if (new Date().getTime() - Stats.get().getLastTimeResetHourStats() > DateUtils.MILLIS_PER_HOUR) {
            timerTask.run();
        }
        new Timer().schedule(timerTask, k * DateUtils.MILLIS_PER_SECOND, DateUtils.MILLIS_PER_HOUR);
    }

    public void restartTodayCleaner() {
        if (todayCleaner != null) {
            todayCleaner.cancel();
        }
        if (timer != null) {
            timer.cancel();
            timer.purge();
        }
        timer = new Timer();
        todayCleaner = new TimerTask() {
            @Override
            public void run() {
                try {
                    log.debug("Reset TODAY stats");
                    Stats.get().resetCountReceivedEmailToday();
                    Stats.get().resetCountSentEmailToday();
                    Stats.get().resetUndeliveredToday();
                    Stats.get().resetUsedProxyToday();
                    Stats.get().resetCountDownloadedProxyToday();
                    for (String folderName : Stats.get().getCategoryStats().keySet()) {
                        Stats.get().resetCategoryStatsToday(folderName);
                    }
                } catch (Throwable t) {
                    log.debug("Throwable", t);
                }
            }
        };
        long current = Helpers.parseTime(DateFormatUtils.format(new Date(), "HH:mm"));
        long delay = Settings.get().TODAY_CLOCK - current;
        if (delay < 0) {
            delay += DateUtils.MILLIS_PER_DAY;
        }
        log.debug("Next reset today stats in ~" + delay / DateUtils.MILLIS_PER_HOUR + " ч " + delay % DateUtils.MILLIS_PER_HOUR / DateUtils.MILLIS_PER_MINUTE + " м");
        timer.schedule(todayCleaner, delay, DateUtils.MILLIS_PER_DAY);
    }
}
