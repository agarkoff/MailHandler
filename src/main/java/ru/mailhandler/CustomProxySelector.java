package ru.mailhandler;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.mailhandler.db.Database;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Stas
 * Date: 22.12.16
 * Time: 22:14
 */
public class CustomProxySelector extends ProxySelector {

    private static final Logger log = LogManager.getLogger(CustomProxySelector.class);

    private static boolean noProxy = false;

    @Override
    public List<Proxy> select(URI uri) {
        if (noProxy) {
            return new ArrayList<>(Arrays.asList(java.net.Proxy.NO_PROXY));
        } else {
            if (StringUtils.contains(uri.toASCIIString(), "admin.5socks.net")) {
                log.debug("For access to admin.5socks.net do not use proxy");
                return new ArrayList<>(Arrays.asList(java.net.Proxy.NO_PROXY));
            } else {
                try {
                    ru.mailhandler.model.Proxy currentProxy = SocksManager.get().getCurrentProxy();
                    log.debug("Use proxy: " + currentProxy);
                    return new ArrayList<>(Arrays.asList(new java.net.Proxy(Proxy.Type.SOCKS, new InetSocketAddress(currentProxy.getHost(), currentProxy.getPort()))));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                } catch (Throwable t) {
                    log.debug("Throwable", t);
                    return new ArrayList<>(Arrays.asList(java.net.Proxy.NO_PROXY));
                }
            }
        }
    }

    @Override
    public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
        log.debug("IOException", ioe);
        try {
            InetSocketAddress isa = (InetSocketAddress) sa;
            ru.mailhandler.model.Proxy proxy = Database.get().getProxyDao().getProxyByHostAndPort(isa.getHostName(), isa.getPort());
            Helpers.incProxyErrorCount(proxy);
            SocksManager.get().resetCurrentProxy();
        } catch (Throwable t) {
            log.debug("Throwable", t);
        }
    }

    public static void setNoProxy(boolean noProxy) {
        CustomProxySelector.noProxy = noProxy;
    }
}
