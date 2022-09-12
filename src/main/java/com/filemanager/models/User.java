package com.filemanager.models;

import com.filemanager.enums.Institution;
import com.filemanager.enums.Role;

import javax.persistence.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Entity
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Integer id;
    @Column(nullable = false)
    private String username = "";
    @Column(nullable = false)
    private String email = "";
    @Transient
    private String password = "";
    @Column(nullable = false)
    private String roles = "";
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Institution institution = Institution.GIE;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public User() {
    }

    public String getRoles() {
        return roles;
    }

    public void setRoles(String roles) {
        this.roles = roles;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public boolean hasRole(String role){
        return this.roles.contains(role);
    }

    public boolean hasAnyRole(String... roles){
        List<String> values = Stream.of(Role.values()).map(Enum::name).collect(Collectors.toList());
        for(String role: roles){
            if(values.contains(role)) return true;
        }
        return false;
    }

    public User(String username, String password, String roles) {
        this.username = username;
        this.password = password;
        this.roles = roles;
    }

    public void normalize(){
        if(this.username != null) this.username = this.username.trim().toLowerCase();
    }
}
