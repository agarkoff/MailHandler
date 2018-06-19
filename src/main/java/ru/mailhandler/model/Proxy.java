package ru.mailhandler.model;

import ru.mailhandler.Helpers;
import ru.misterparser.common.model.NamedElement;
import ru.misterparser.common.proxy.ProxyInfo;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * Created with IntelliJ IDEA.
 * User: Stas
 * Date: 20.12.16
 * Time: 10:16
 */
public class Proxy extends ProxyInfo implements NamedElement {

    @Id
    @GeneratedValue
    private long id;

    @Column(name = "download_time")
    private long downloadTime;

    @Column(name = "error_count")
    private long errorCount;

    public Proxy() {
    }

    public Proxy(String host, int port, String userName, String password, long downloadTime) {
        super(host, port, userName, password);
        this.downloadTime = downloadTime;
    }

    public long getDownloadTime() {
        return downloadTime;
    }

    public void setDownloadTime(long downloadTime) {
        this.downloadTime = downloadTime;
    }

    public long incErrorCount() {
        return this.errorCount++;
    }

    public long getErrorCount() {
        return errorCount;
    }

    @Override
    public String getName() {
        return toString();
    }

    @Override
    public String toString() {
        return super.toString() + ", errorCount=" + errorCount + ", date: " + Helpers.formatDate(downloadTime);
    }
}
