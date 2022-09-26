package com.filemanager.models;

import com.filemanager.enums.Level;

import javax.persistence.*;
import java.util.Date;

@Entity
public class Log {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;
    @Enumerated(EnumType.STRING)
    private Level level = Level.INFO;
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String message = "";
    @Column(columnDefinition = "LONGTEXT")
    private String details = "";
    @Column(nullable = false)
    private Date date = new Date();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Level getLevel() {
        return level;
    }

    public void setLevel(Level level) {
        this.level = level;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Log() {
    }

    public Log(String message) {
        this.message = message;
    }

    public Log(Level level, String message) {
        this.level = level;
        this.message = message;
    }

    public Log(Level level, String message, String details) {
        this.level = level;
        this.message = message;
        this.details = details;
    }

    public static Log info(String message){
        return new Log(Level.INFO, message);
    }

    public static Log error(String message, String details){
        return new Log(Level.ERROR, message, details);
    }

    public static Log warn(String message){
        return new Log(Level.WARN, message);
    }
}
