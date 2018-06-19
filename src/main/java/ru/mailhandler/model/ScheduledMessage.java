package ru.mailhandler.model;

import ru.mailhandler.Helpers;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * Created with IntelliJ IDEA.
 * User: Stas
 * Date: 20.12.16
 * Time: 20:02
 */
@Entity(name = "scheduled_message")
public class ScheduledMessage {

    @Id
    @GeneratedValue
    private long id;

    @Column
    private String name;

    @Column
    private String to;

    @Column
    private String subject;

    @Column
    private String body;

    @Column(name = "attachment_filenames")
    private String attachmentFilenames;

    @Column(name = "folder_name")
    private String folderName;

    @Column(name = "sending_time")
    private long sendingTime;

    @Column
    private int priority;

    public ScheduledMessage() {
    }

    public ScheduledMessage(String to, String name, String subject, String body, String attachmentFilenames, String folderName, long sendingTime, int priority) {
        this.to = to;
        this.name = name;
        this.subject = subject;
        this.body = body;
        this.attachmentFilenames = attachmentFilenames;
        this.folderName = folderName;
        this.sendingTime = sendingTime;
        this.priority = priority;
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

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getAttachmentFilenames() {
        return attachmentFilenames;
    }

    public void setAttachmentFilenames(String attachmentFilenames) {
        this.attachmentFilenames = attachmentFilenames;
    }

    public String getFolderName() {
        return folderName;
    }

    public void setFolderName(String folderName) {
        this.folderName = folderName;
    }

    public long getSendingTime() {
        return sendingTime;
    }

    public void setSendingTime(long sendingTime) {
        this.sendingTime = sendingTime;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    @Override
    public String toString() {
        return "ScheduledMessage{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", to='" + to + '\'' +
                ", subject='" + subject + '\'' +
                ", attachmentFilenames='" + attachmentFilenames + '\'' +
                ", folderName='" + folderName + '\'' +
                ", sendingTime=" + Helpers.formatDate(sendingTime) +
                ", priority='" + priority + '\'' +
                '}';
    }
}
