package ru.mailhandler.thread;

import org.apache.commons.lang3.time.DateUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.mailhandler.Helpers;
import ru.mailhandler.MailBoxLocker;
import ru.mailhandler.SocksManager;
import ru.mailhandler.db.Database;
import ru.mailhandler.settings.Settings;
import ru.misterparser.common.ControlledRunnable;
import ru.mailhandler.model.Proxy;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Stas
 * Date: 20.12.16
 * Time: 20:20
 */
public class ProxyCleaner extends ControlledRunnable {

    private static final Logger log = LogManager.getLogger(ProxyCleaner.class);

    @Override
    public void run() {
        while (true) {
            try {
                synchronized (MailBoxLocker.class) {
                    //log.debug("Поток очистки старых прокси запущен");
                    int c = 0;
                    List<Proxy> proxies = Database.get().getProxyDao().queryForAll();
                    for (Proxy proxy : proxies) {
                        if (System.currentTimeMillis() - proxy.getDownloadTime() > Settings.get().PROXY_TTL) {
                            log.debug("Proxy " + proxy + " delete by life time");
                            if (SocksManager.get().getCurrentProxy().equals(proxy)) {
                                SocksManager.get().resetCurrentProxy();
                            }
                            Helpers.deleteProxy(proxy);
                            c++;
                        }
                    }
                    log.debug("Removing the old proxy: " + c);
                }
                Thread.sleep(5 * DateUtils.MILLIS_PER_MINUTE);
                checkForWait();
            } catch (InterruptedException e) {
                log.debug("Stopping ProxyCleaner");
                return;
            } catch (Throwable t) {
                log.debug("Throwable", t);
            }
        }
    }
}
