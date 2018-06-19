package ru.mailhandler.model;

import org.apache.commons.lang3.StringUtils;
import ru.mailhandler.Helpers;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * Created with IntelliJ IDEA.
 * User: Stas
 * Date: 20.12.16
 * Time: 21:48
 */
@Entity(name = "response_time")
public class ResponseTime {

    @Id
    @Column
    private String email;

    @Column
    private long time;

    public ResponseTime() {
    }

    public ResponseTime(String email, long time) {
        this.email = StringUtils.lowerCase(email);
        this.time = time;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    @Override
    public String toString() {
        return "ResponseTime{" +
                "email='" + email + '\'' +
                ", time='" + Helpers.formatDate(time) + '\'' +
                '}';
    }
}
