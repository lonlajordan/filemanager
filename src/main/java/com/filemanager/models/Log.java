package com.filemanager.models;

import com.filemanager.enums.Level;

import javax.persistence.*;
import java.util.Date;

@Entity
public class Log {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Integer id;
    @Enumerated(EnumType.STRING)
    private Level level = Level.INFO;
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String message = "";
    @Column(nullable = false)
    private Date date = new Date();

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
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

    public static Log info(String message){
        return new Log(Level.INFO, message);
    }

    public static Log error(String message){
        return new Log(Level.ERROR, message);
    }
}
