package ru.mailhandler.stat;

import java.util.Date;
import java.util.TreeMap;

/**
 * Created with IntelliJ IDEA.
 * User: Stas
 * Date: 28.12.16
 * Time: 23:37
 */
public class FetchEmailIntervalStats {

    private Date date1;
    private Date date2;
    private long countReceived;
    private long countCanceled;
    private TreeMap<String, Long> sentByCategories = new TreeMap<>();

    public FetchEmailIntervalStats() {
        date1 = new Date();
    }

    public Date getDate1() {
        return date1;
    }

    public void setDate1(Date date1) {
        this.date1 = date1;
    }

    public Date getDate2() {
        return date2;
    }

    public void setDate2(Date date2) {
        this.date2 = date2;
    }

    public long getCountReceived() {
        return countReceived;
    }

    public void incCountReceived() {
        this.countReceived++;
    }

    public long getCountCanceled() {
        return countCanceled;
    }

    public void incCountCanceled() {
        this.countCanceled++;
    }

    public void incSentByCategories(String folderName) {
        Long l = sentByCategories.get(folderName);
        if (l == null) {
            l = 0L;
        }
        l++;
        sentByCategories.put(folderName, l);
    }

    public TreeMap<String, Long> getSentByCategories() {
        return sentByCategories;
    }

    @Override
    public String toString() {
        return "FetchEmailIntervalStats{" +
                "date1=" + date1 +
                ", date2=" + date2 +
                ", countReceived=" + countReceived +
                ", countCanceled=" + countCanceled +
                ", sentByCategories=" + sentByCategories +
                '}';
    }
}
