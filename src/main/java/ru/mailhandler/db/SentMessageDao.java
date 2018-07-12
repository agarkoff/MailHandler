package ru.mailhandler.db;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.support.ConnectionSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.mailhandler.Helpers;
import ru.mailhandler.model.SentMessage;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Stas
 * Date: 29.12.16
 * Time: 12:50
 */
public class SentMessageDao {

    private static final Logger log = LogManager.getLogger(SentMessageDao.class);

    private Dao<SentMessage, Long> sentMessageDao;

    public SentMessageDao(ConnectionSource connectionSource) throws SQLException {
        sentMessageDao = DaoManager.createDao(connectionSource, SentMessage.class);
    }

    public List<SentMessage> getSentMessages(Date date1, Date date2) throws SQLException {
        List<SentMessage> sentMessages = sentMessageDao.queryBuilder().where().ge("sent_time", date1.getTime()).and().le("sent_time", date2.getTime()).query();
        log.debug("Found sent messages from " + Helpers.formatDate(date1.getTime()) + " to " + Helpers.formatDate(date2.getTime()) + ": " + sentMessages.size());
        return sentMessages;
    }

    public int create(SentMessage sentMessage) throws SQLException {
        return sentMessageDao.create(sentMessage);
    }
}
