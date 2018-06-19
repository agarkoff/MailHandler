package ru.mailhandler.stat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.collections4.bidimap.TreeBidiMap;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.misterparser.common.configuration.ConfigurationUtils;
import ru.mailhandler.MainFrame;
import ru.mailhandler.model.CategoryRow;

import java.io.File;
import java.util.Date;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: Stas
 * Date: 20.12.16
 * Time: 10:56
 */
public class Stats {

    private static final Logger log = LogManager.getLogger(Stats.class);

    private static Stats stats = new Stats();

    public static Stats get() {
        return stats;
    }

    public static void setStats(Stats stats) {
        Stats.stats = stats;
    }

    public Map<String, CategoryRow> getCategoryStats() {
        return categoryStats;
    }

    private long countReceivedEmailByHour;
    private long countSentEmailByHour;
    private long countReceivedEmailToday;
    private long countSentEmailToday;
    private long undeliveredToday;
    private long usedProxyToday;
    private long countDownloadedProxyToday;
    private TreeBidiMap<String, CategoryRow> categoryStats = new TreeBidiMap<>();
    private long lastTimeResetHourStats = 0;

    public void resetCountReceivedEmailByHour() {
        countReceivedEmailByHour = 0;
        saveAndDisplayStats();
    }

    private void incCountReceivedEmailByHour() {
        countReceivedEmailByHour++;
        saveAndDisplayStats();
    }

    public long getCountReceivedEmailByHour() {
        return countReceivedEmailByHour;
    }


    public void resetCountSentEmailByHour() {
        countSentEmailByHour = 0;
        saveAndDisplayStats();
    }

    private void incCountSentEmailByHour() {
        countSentEmailByHour++;
        saveAndDisplayStats();
    }

    public long getCountSentEmailByHour() {
        return countSentEmailByHour;
    }


    public void resetCountReceivedEmailToday() {
        countReceivedEmailToday = 0;
        saveAndDisplayStats();
    }

    private void incCountReceivedEmailToday() {
        countReceivedEmailToday++;
        saveAndDisplayStats();
    }

    public long getCountReceivedEmailToday() {
        return countReceivedEmailToday;
    }


    public void resetCountSentEmailToday() {
        countSentEmailToday = 0;
        saveAndDisplayStats();
    }

    private void incCountSentEmailToday() {
        countSentEmailToday++;
        saveAndDisplayStats();
    }

    public long getCountSentEmailToday() {
        return countSentEmailToday;
    }


    public void incCountSentEmail() {
        incCountSentEmailByHour();
        incCountSentEmailToday();
    }

    public void incCountReceivedEmail() {
        incCountReceivedEmailByHour();
        incCountReceivedEmailToday();
    }


    public void resetUndeliveredToday() {
        undeliveredToday = 0;
        saveAndDisplayStats();
    }

    public void incUndeliveredToday() {
        undeliveredToday++;
        saveAndDisplayStats();
    }

    public long getUndeliveredToday() {
        return undeliveredToday;
    }


    public void resetUsedProxyToday() {
        usedProxyToday = 0;
        saveAndDisplayStats();
    }

    public void incUsedProxyToday() {
        usedProxyToday++;
        saveAndDisplayStats();
    }

    public long getUsedProxyToday() {
        return usedProxyToday;
    }


    public void resetCountDownloadedProxyToday() {
        countDownloadedProxyToday = 0;
        saveAndDisplayStats();
    }

    public void incCountDownloadedProxyToday() {
        countDownloadedProxyToday++;
        saveAndDisplayStats();
    }

    public long getCountDownloadedProxyToday() {
        return countDownloadedProxyToday;
    }


    public void resetCategoryStatsHour(String folderName) {
        CategoryRow categoryRow = categoryStats.get(folderName);
        if (categoryRow != null) {
            categoryRow.setCountByHour(0);
            saveAndDisplayStats();
        }
    }

    public void resetCategoryStatsToday(String folderName) {
        CategoryRow categoryRow = categoryStats.get(folderName);
        if (categoryRow != null) {
            categoryRow.setCountByToday(0);
            saveAndDisplayStats();
        }
    }

    private void incCategoryStatsHour(String folderName) {
        CategoryRow categoryRow = categoryStats.get(folderName);
        if (categoryRow != null) {
            categoryRow.setCountByHour(categoryRow.getCountByHour() + 1);
            saveAndDisplayStats();
        }
    }

    private void incCategoryStatsToday(String folderName) {
        CategoryRow categoryRow = categoryStats.get(folderName);
        if (categoryRow != null) {
            categoryRow.setCountByToday(categoryRow.getCountByToday() + 1);
            saveAndDisplayStats();
        }
    }

    public void incCategoryStats(String folderName) {
        incCategoryStatsHour(folderName);
        incCategoryStatsToday(folderName);
    }

    public void addOrChangeCategoryStats(String oldFolderName, String newFolderName) {
        if (oldFolderName == null) {
            categoryStats.put(newFolderName, new CategoryRow(newFolderName, 0, 0));
        } else if (newFolderName == null) {
            categoryStats.remove(oldFolderName);
        } else {
            CategoryRow categoryRow = categoryStats.get(oldFolderName);
            if (categoryRow == null) {
                categoryRow = new CategoryRow(newFolderName, 0, 0);
                categoryStats.put(newFolderName, categoryRow);
            }
            categoryRow.setFolderName(newFolderName);
        }
        saveAndDisplayStats();
    }


    public void saveAndDisplayStats() {
        MainFrame.get().getCountReceivedEmailByHourTextField().setText(String.valueOf(countReceivedEmailByHour));
        MainFrame.get().getCountSentEmailByHourTextField().setText(String.valueOf(countSentEmailByHour));
        MainFrame.get().getCountReceivedEmailTodayTextField().setText(String.valueOf(countReceivedEmailToday));
        MainFrame.get().getCountSentEmailTodayTextField().setText(String.valueOf(countSentEmailToday));
        MainFrame.get().getUndeliveredTodayTextField().setText(String.valueOf(undeliveredToday));
        MainFrame.get().getUsedProxyTodayTextField().setText(String.valueOf(usedProxyToday));
        MainFrame.get().getCountDownloadedProxyTodayTextField().setText(String.valueOf(countDownloadedProxyToday));
        MainFrame.get().getCategoryTableModel().fireTableDataChanged();
        save();
    }

    public synchronized void save() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String s = gson.toJson(Stats.get());
        try {
            FileUtils.write(new File(ConfigurationUtils.getCurrentDirectory() + "stats.json"), s, "UTF-8");
        } catch (Throwable t) {
            log.debug("Throwable", t);
        }
    }

    public void updateLastTimeResetHourStats() {
        lastTimeResetHourStats = new Date().getTime();
        save();
    }

    public long getLastTimeResetHourStats() {
        return lastTimeResetHourStats;
    }
}
