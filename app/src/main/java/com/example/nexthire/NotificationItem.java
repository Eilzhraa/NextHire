package com.example.nexthire;

public class NotificationItem {
    private String title;
    private String body;
    private String timestamp;
    private String read;

    private String key;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public NotificationItem() {
    }

    public NotificationItem(String title, String body, String timestamp, String read) {
        this.title = title;
        this.body = body;
        this.timestamp = timestamp;
        this.read = read;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getRead() {
        return read;
    }

    public void setRead(String read) {
        this.read = read;
    }
}