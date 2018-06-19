package ru.mailhandler.model;

import javax.persistence.Column;
import javax.persistence.Entity;

/**
 * Created with IntelliJ IDEA.
 * User: Stas
 * Date: 28.12.16
 * Time: 21:20
 */
@Entity(name = "seen_message")
public class SeenMessage {

    @Column(unique = true)
    private String uid;

    @Column
    private long time;

    public SeenMessage() {
    }

    public SeenMessage(String uid, long time) {
        this.uid = uid;
        this.time = time;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    @Override
    public String toString() {
        return "SeenMessage{" +
                "uid='" + uid + '\'' +
                '}';
    }
}
