package ru.mailhandler.model;

import ru.mailhandler.Helpers;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * Created with IntelliJ IDEA.
 * User: Stas
 * Date: 28.12.16
 * Time: 19:47
 */
@Entity(name = "sent_message")
public class SentMessage {

    @Id
    @GeneratedValue
    private long id;

    @Column
    private String name;

    @Column
    private String email;

    @Column
    private String subject;

    @Column
    private String proxy;

    @Column(name = "sent_time")
    private long sentTime;

    public SentMessage() {
    }

    public SentMessage(String name, String email, String subject, String proxy, long sentTime) {
        this.name = name;
        this.email = email;
        this.subject = subject;
        this.proxy = proxy;
        this.sentTime = sentTime;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getProxy() {
        return proxy;
    }

    public void setProxy(String proxy) {
        this.proxy = proxy;
    }

    public long getSentTime() {
        return sentTime;
    }

    public void setSentTime(long sentTime) {
        this.sentTime = sentTime;
    }

    public String getFormattedSentTime() {
        return Helpers.formatDate(sentTime);
    }

    @Override
    public String toString() {
        return "SentMessage{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", subject='" + subject + '\'' +
                ", proxy='" + proxy + '\'' +
                ", sentTime=" + Helpers.formatDate(sentTime) +
                '}';
    }
}
