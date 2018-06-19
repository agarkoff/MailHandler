package ru.mailhandler.db;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.support.ConnectionSource;
import ru.mailhandler.MainFrame;
import ru.mailhandler.model.Proxy;

import java.sql.SQLException;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Stas
 * Date: 25.12.16
 * Time: 17:56
 */
public class ProxyDao {

    private Dao<Proxy, Long> proxyDao;

    public ProxyDao(ConnectionSource connectionSource) throws SQLException {
        proxyDao = DaoManager.createDao(connectionSource, Proxy.class);
    }

    public Proxy getProxyByHostAndPort(String host, int port) throws SQLException {
        return proxyDao.queryBuilder().where().eq("host", host).and().eq("port", port).queryForFirst();
    }

    public List<Proxy> queryForAll() throws SQLException {
        return proxyDao.queryForAll();
    }

    public void create(Proxy proxy) throws SQLException {
        proxyDao.create(proxy);
        MainFrame.get().refreshSocksCombobox();
    }

    public void update(Proxy proxy) throws SQLException {
        proxyDao.update(proxy);
        MainFrame.get().refreshSocksCombobox();
    }

    public int delete(Proxy proxy) throws SQLException {
        int r = proxyDao.delete(proxy);
        MainFrame.get().refreshSocksCombobox();
        return r;
    }

    public long countOf() throws SQLException {
        return proxyDao.countOf();
    }
}
