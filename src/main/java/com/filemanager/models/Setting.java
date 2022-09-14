package com.filemanager.models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class Setting {
    @Id
    @Column(nullable = false)
    private String id;
    @Column(nullable = false)
    private String label = "";
    @Column(nullable = false)
    private String value = "";

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Setting() {
    }

    public Setting(String id, String label) {
        this.id = id;
        this.label = label;
    }
}
