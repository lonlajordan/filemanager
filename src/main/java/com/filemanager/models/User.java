package com.filemanager.models;

import com.filemanager.enums.Institution;
import com.filemanager.enums.Role;

import javax.persistence.*;
import java.util.Date;

@Entity
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Integer id;
    @Column(nullable = false, unique = true)
    private String username = "";
    @Transient
    private String password = "";
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.ROLE_GIE;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Institution institution = Institution.GIE;
    private Date lastLogin = new Date();
    private boolean enabled = true;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public Date getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(Date lastLogin) {
        this.lastLogin = lastLogin;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean hasRole(String role){
        return this.role.name().equals(role);
    }

    public boolean hasAnyRole(String... roles){
        for(String role: roles) if(this.role.name().equals(role)) return true;
        return false;
    }

    public User(String username, String password, String role) {
        this.username = username;
        this.password = password;
        this.role = Role.valueOf(role);
    }

    public void normalize(){
        if(this.username != null) this.username = this.username.trim().toLowerCase();
    }
}
