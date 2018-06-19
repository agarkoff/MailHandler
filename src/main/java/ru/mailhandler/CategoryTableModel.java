package ru.mailhandler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.mailhandler.model.CategoryRow;

import javax.swing.table.DefaultTableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: Stas
 * Date: 25.12.16
 * Time: 18:58
 */
public class CategoryTableModel extends DefaultTableModel {

    private static final Logger log = LogManager.getLogger(CategoryTableModel.class);

    private Map<String, CategoryRow> categoryStats;

    public CategoryTableModel(Map<String, CategoryRow> categoryStats) {
        super(new String[]{"Категория", "Получено за час", "Получено за сегодня"}, 0);
        this.categoryStats = categoryStats;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return String.class;
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }

    @Override
    public int getRowCount() {
        return categoryStats != null ? categoryStats.size() : 0;
    }

    @Override
    public int getColumnCount() {
        return columnIdentifiers.size();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        List<CategoryRow> values = new ArrayList<>(categoryStats.values());
        CategoryRow categoryRow = values.get(rowIndex);
        switch (columnIndex) {
            case 0:
                return categoryRow.getFolderName();
            case 1:
                return categoryRow.getCountByHour();
            case 2:
                return categoryRow.getCountByToday();
            default:
                throw new RuntimeException("Ошибка форматирования таблицы!");
        }
    }

//    public Item getItem(int row) {
//        return items.get(row);
//    }
//
//    public void clear() {
//        items.clear();
//        fireTableDataChanged();
//    }
//

//
//    public List<Item> getItems() {
//        return items;
//    }
}
