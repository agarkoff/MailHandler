package ru.mailhandler.model;

/**
 * Created with IntelliJ IDEA.
 * User: Stas
 * Date: 25.12.16
 * Time: 19:03
 */
public class CategoryRow implements Comparable<CategoryRow> {

    private String folderName;
    private long countByHour;
    private long countByToday;

    public CategoryRow(String folderName, long countByHour, long countByToday) {
        this.folderName = folderName;
        this.countByHour = countByHour;
        this.countByToday = countByToday;
    }

    public String getFolderName() {
        return folderName;
    }

    public void setFolderName(String folderName) {
        this.folderName = folderName;
    }

    public long getCountByHour() {
        return countByHour;
    }

    public void setCountByHour(long countByHour) {
        this.countByHour = countByHour;
    }

    public long getCountByToday() {
        return countByToday;
    }

    public void setCountByToday(long countByToday) {
        this.countByToday = countByToday;
    }

    @Override
    public String toString() {
        return "CategoryRow{" +
                "folderName='" + folderName + '\'' +
                ", countByHour=" + countByHour +
                ", countByToday=" + countByToday +
                '}';
    }

    @Override
    public int compareTo(CategoryRow o) {
        return folderName.compareTo(o.getFolderName());
    }
}
