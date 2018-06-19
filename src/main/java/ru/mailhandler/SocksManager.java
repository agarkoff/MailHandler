package ru.mailhandler;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlTableCell;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import ru.mailhandler.db.Database;
import ru.mailhandler.stat.Stats;
import ru.misterparser.common.AuthUtils;
import ru.misterparser.common.HtmlCleanerUtils;
import ru.misterparser.common.PoolingHttpClient;
import ru.misterparser.common.Utils;
import ru.misterparser.common.configuration.ConfigurationUtils;
import ru.mailhandler.model.Proxy;
import ru.mailhandler.settings.Settings;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: Stas
 * Date: 17.12.16
 * Time: 18:54
 */
public class SocksManager {

    private static final Logger log = LogManager.getLogger(SocksManager.class);

    private static final SocksManager socksManager = new SocksManager();

    public static SocksManager get() {
        return socksManager;
    }

    private SocksManager() {
    }

    private Random random = new Random();
    private DefaultHttpClient httpClient = PoolingHttpClient.getHttpClient(20000);
    private HtmlCleaner htmlCleaner = new HtmlCleaner();
    private WebClient webClient = new WebClient(BrowserVersion.FIREFOX_45);
    private Proxy currentProxy = null;
    private int proxyIndex = 0;

    public synchronized Proxy getCurrentProxy() throws SQLException, InterruptedException {
        if (currentProxy == null) {
            nextProxy();
        }
        return currentProxy;
    }

    public synchronized void nextProxy() throws InterruptedException, SQLException {
        if (Settings.get().NO_PROXY) {
            currentProxy = null;
            return;
        }
        List<Proxy> proxies = Database.get().getProxyDao().queryForAll();
        if (proxies.size() == 0) {
            fillProxies(1);
            proxies = Database.get().getProxyDao().queryForAll();
        }
        if (proxyIndex >= proxies.size()) {
            proxyIndex = 0;
        }
        currentProxy = proxies.get(proxyIndex++);
        log.debug("Set current proxy: " + currentProxy);
    }

    public synchronized void fillProxies(long max) throws InterruptedException {
        if (Settings.get().NO_PROXY) {
            return;
        }
        log.debug("Need receive " + max + " proxy");
        int c = 0;
        while (c < max) {
            try {
                try {
                    authProxyPanel();
                    checkUnused();
                } catch (IOException e) {
                    log.debug("Auth error");
                    Helpers.sendEmailToAdmin("Ошибка авторизации на admin.5socks.net", Helpers.getStackTrace(e));
                    timeoutProxyPanel();
                    continue;
                }
                for (String currentUrl : Settings.get().PROXY_PANEL_URLS) {
                    randomTimeout(5, 10);
                    c += addProxies(currentUrl, max - c);
                    if (c >= max) {
                        break;
                    }
                }
                break;
            } catch (InterruptedException e) {
                throw e;
            } catch (Throwable t) {
                log.debug("Throwable", t);
                Helpers.sendEmailToAdmin("Ошибка при работе с admin.5socks.net", Helpers.getStackTrace(t));
                timeoutProxyPanel();
            } finally {
                log.debug("Proxy panel logout...");
                Utils.fetch(httpClient, "http://admin.5socks.net/cgi-bin/login.cgi?action=login", "iso-8859-1");
            }
        }
    }

    private synchronized void checkUnused() throws InterruptedException {
        try {
            String page = Utils.fetch(httpClient, "http://admin.5socks.net/cgi-bin/login.cgi?action=settings", "iso-8859-1");
            TagNode rootNode = htmlCleaner.clean(page);
            boolean f = false;
            for (TagNode tr : HtmlCleanerUtils.evaluateXPath(rootNode, "body/font[2]/table/tbody/tr/td/table/tbody/tr")) {
                String t = HtmlCleanerUtils.getText(tr);
                if (StringUtils.startsWithIgnoreCase(t, "Unused proxies")) {
                    f = true;
                    TagNode td2 = Utils.getFirstTagNode(tr, "./td[2]");
                    String s = HtmlCleanerUtils.getText(td2);
                    int c = Integer.parseInt(s);
                    log.debug("Remain not use proxy: " + c);
                    if (c < Settings.get().SOCKS_THRESHOLD) {
                        Helpers.sendEmailToAdmin("Предупреждение с admin.5socks.net", Utils.squeezeText(t));
                    }
                }
            }
            if (!f) {
                Helpers.sendEmailToAdmin("Предупреждение с admin.5socks.net", "Не найдено количество оставшихся прокси");
            }
        } catch (Throwable t) {
            log.debug("Throwable", t);
        }
    }

    private synchronized void authProxyPanel() throws InterruptedException, IOException {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("user", Settings.get().PROXY_PANEL_USER);
        map.put("pass", Settings.get().PROXY_PANEL_PASS);
        AuthUtils.authorize(httpClient, "http://admin.5socks.net/", ".//form[@action='/cgi-bin/login.cgi']", new AuthUtils.AuthResultChecker() {
            @Override
            public boolean check(String result) {
                try {
                    String loginPage = Utils.fetch(httpClient, "http://admin.5socks.net/cgi-bin/login.cgi", "iso-8859-1");
                    return StringUtils.containsIgnoreCase(loginPage, "Hello " + Settings.get().PROXY_PANEL_USER);
                } catch (Throwable t) {
                    log.debug("Throwable", t);
                    return false;
                }
            }
        }, map, "iso-8859-1");
        webClient.getCookieManager().clearCookies();
        for (Cookie cookie : httpClient.getCookieStore().getCookies()) {
            webClient.getCookieManager().addCookie(new com.gargoylesoftware.htmlunit.util.Cookie(cookie.getDomain(), cookie.getName(), cookie.getValue(), cookie.getPath(), cookie.getExpiryDate(), false, false));
        }
    }

    private synchronized int addProxies(String currentUrl, long max) throws InterruptedException, IOException {
        if (max <= 0) {
            return 0;
        }
        int c = 0;
        log.debug("Process page: " + currentUrl);
        List<TagNode> trs = new ArrayList<>();
        while (trs.size() == 0) {
            String page = Utils.fetch(httpClient, currentUrl, "iso-8859-1");
            TagNode rootNode = htmlCleaner.clean(page);
            trs = HtmlCleanerUtils.evaluateXPath(rootNode, ".//form[@name='selecter']/table/tbody/tr/td/table/tbody/tr");
            if (trs.size() == 0) {
                if (StringUtils.containsIgnoreCase(page, "Account suspended, you have spent all proxies, please make a payment")) {
                    Helpers.sendEmailToAdmin("Account on admin.5socks.net suspended", "Account suspended, you have spent all proxies, please make a payment");
                } else {
                    String filename = Utils.debugSave(ConfigurationUtils.getCurrentDirectory(), page, "iso-8859-1");
                    log.debug("Error in proxy panel, page dump saved in file: " + filename);
                }
                timeoutProxyPanel();
            }
        }
        Collections.reverse(trs);
        for (TagNode tr : trs) {
            TagNode a = Utils.getFirstTagNode(tr, "./td[2]/a", false);
            if (a != null) {
                String href = a.getAttributeByName("href");
                if (StringUtils.startsWithIgnoreCase(href, "javascript:showinfo")) {
                    while (Stats.get().getCountDownloadedProxyToday() >= Settings.get().MAX_COUNT_PROXY_TODAY) {
                        log.debug("Daily proxy limit exceeded");
                        Thread.sleep(1000);
                    }
                    String proxyUrl = StringUtils.substringAfter(href, "javascript:showinfo");
                    proxyUrl = StringUtils.substringAfter(proxyUrl, "'");
                    proxyUrl = StringUtils.substringBefore(proxyUrl, "'");
                    proxyUrl = Utils.normalizeUrl(proxyUrl, "http://admin.5socks.net/");
                    randomTimeout(5, 10);
                    log.debug("Process page: " + proxyUrl);
                    HtmlPage proxyHtmlPage = webClient.getPage(proxyUrl);
                    HtmlTableCell tdProxy = proxyHtmlPage.getFirstByXPath(".//td[@id='ipport']");
                    String t = tdProxy.asText();
                    if (StringUtils.equalsIgnoreCase(t, "click here to view")) {
                        HtmlAnchor aProxy = tdProxy.getFirstByXPath("./a");
                        if (aProxy == null) {
                            log.debug("Not found link in block 'click here to view'");
                            continue;
                        }
                        String proxyCheckUrl = aProxy.getHrefAttribute();
                        proxyCheckUrl = Utils.normalizeUrl(proxyCheckUrl, "http://admin.5socks.net/");
                        randomTimeout(5, 10);
                        log.debug("Process page: " + proxyCheckUrl);
                        HtmlPage htmlPage = webClient.getPage(proxyCheckUrl);
                        String proxyCheckPage = htmlPage.asXml();
                        if (StringUtils.containsIgnoreCase(proxyCheckPage, "Checking proxy....done")) {
                            HtmlInput input = htmlPage.getFirstByXPath(".//input");
                            if (input != null) {
                                Stats.get().incCountDownloadedProxyToday();
                                String proxyString = input.getValueAttribute();
                                Proxy proxy = Helpers.parseProxy(proxyString);
                                if (proxy != null) {
                                    try {
                                        Database.get().getProxyDao().create(proxy);
                                        c++;
                                        log.debug("Proxy saved: " + proxy);
                                    } catch (SQLException e) {
                                        log.debug("SQLException", e);
                                    }
                                    if (c == max) {
                                        log.debug("Received the required number of the proxy");
                                        break;
                                    }
                                }
                            }
                        } else {
                            String filename = Utils.debugSave(ConfigurationUtils.getCurrentDirectory(), proxyCheckPage, "iso-8859-1");
                            log.debug("Proxy check result not contains string Checking proxy....done, результат сохранен в файл " + filename);
                        }
                    } else {
                        log.debug("String contains already use proxy " + t);
                    }
                }
            }
        }
        return c;
    }

    private synchronized void randomTimeout(int t1, int t2) throws InterruptedException {
        long timeout = random.nextInt((int) ((t2 - t1) * DateUtils.MILLIS_PER_SECOND)) + t1 * DateUtils.MILLIS_PER_SECOND;
        log.debug("Random timeout from " + t1 + " to " + t2 + " s: " + timeout / 1000);
        Thread.sleep(timeout);
    }

    private synchronized void timeoutProxyPanel() throws InterruptedException {
        // пауза от 10 до 30 минут
        randomTimeout(600, 1800);
    }

    public synchronized void resetCurrentProxy() {
        log.debug("Reset current proxy");
        currentProxy = null;
    }
}
