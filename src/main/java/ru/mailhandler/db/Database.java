package ru.mailhandler.db;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.mailhandler.model.ResponseTime;
import ru.mailhandler.model.ScheduledMessage;
import ru.mailhandler.model.SeenMessage;
import ru.misterparser.common.configuration.ConfigurationUtils;

import java.sql.*;

/**
 * Created with IntelliJ IDEA.
 * User: Stas
 * Date: 18.12.16
 * Time: 11:11
 */
public class Database {

    private static final Logger log = LogManager.getLogger(Database.class);

    private static final Database database = new Database();

    public static Database get() {
        return database;
    }

    private ConnectionSource connectionSource;
    private ProxyDao proxyDao;
    private Dao<ScheduledMessage, Long> scheduledMessageDao;
    private Dao<ResponseTime, Long> responseTimeDao;
    private SentMessageDao sentMessageDao;
    private Dao<SeenMessage, Long> seenMessageDao;

    private Database() {
        try {
            initDatabase();
            String dbFileName = ConfigurationUtils.getCurrentDirectory() + "mailhandler.sqlite3";
            connectionSource = new JdbcConnectionSource("jdbc:sqlite:" + dbFileName);
            proxyDao = new ProxyDao(connectionSource);
            scheduledMessageDao = DaoManager.createDao(connectionSource, ScheduledMessage.class);
            responseTimeDao = DaoManager.createDao(connectionSource, ResponseTime.class);
            sentMessageDao = new SentMessageDao(connectionSource);
            seenMessageDao = DaoManager.createDao(connectionSource, SeenMessage.class);
            connectionSource.close();
        } catch (Throwable t) {
            log.debug("Throwable", t);
        }
    }

    public void initDatabase() {

        String dbFileName = ConfigurationUtils.getCurrentDirectory() + "mailhandler.sqlite3";
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + dbFileName)) {
            {
                String initSql = IOUtils.toString(getClass().getClassLoader().getResourceAsStream("mailhandler.sql"), "UTF-8");
                updateFromFile(connection, initSql);
            }
            {
                int version = getVersion(connection);
                log.debug("Current database version: " + version);
                if (version < 2) {
                    String sql = IOUtils.toString(getClass().getClassLoader().getResourceAsStream("update_to_v2.sql"), "UTF-8");
                    updateFromFile(connection, sql);
                    setVersion(connection, 2);
                    log.debug("Set database version to " + getVersion(connection));
                }
            }
        } catch (Throwable t) {
            log.debug("Throwable", t);
        }
    }

    private void updateFromFile(Connection connection, String initSql) throws SQLException {
        for (String s : StringUtils.splitByWholeSeparator(initSql, "$$$")) {
            PreparedStatement preparedStatement = connection.prepareStatement(s);
            preparedStatement.executeUpdate();
        }
    }

    public int getVersion(Connection connection) {
        try (ResultSet rs = connection.prepareStatement("PRAGMA USER_VERSION").executeQuery()) {
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void setVersion(Connection connection, int version) {
        try {
            Statement statement = connection.createStatement();
            statement.execute("PRAGMA USER_VERSION = " + version);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public ProxyDao getProxyDao() {
        return proxyDao;
    }

    public Dao<ScheduledMessage, Long> getScheduledMessageDao() {
        return scheduledMessageDao;
    }

    public Dao<ResponseTime, Long> getResponseTimeDao() {
        return responseTimeDao;
    }

    public SentMessageDao getSentMessageDao() {
        return sentMessageDao;
    }

    public Dao<SeenMessage, Long> getSeenMessageDao() {
        return seenMessageDao;
    }

    public void closeQuietly() {
        //connectionSource.closeQuietly();
    }
}
