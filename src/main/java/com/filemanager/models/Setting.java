package com.filemanager.models;

import javax.persistence.*;

@Entity
public class Setting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Integer id;
    @Column(nullable = false)
    private String code = "";
    @Column(nullable = false)
    private String value = "";
}
