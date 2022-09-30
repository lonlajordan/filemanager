package com.filemanager.models;

import com.filemanager.enums.Institution;
import com.filemanager.enums.Role;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    @Column(nullable = false)
    private String roles = Role.ROLE_GIE.name();
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Institution institution = Institution.GIE;
    private Date lastLogin;
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

    public List<Role> getRoleList() {
        Role[] values = Role.values();
        List<Role> privileges = new ArrayList<>();
        for(Role value: values){
            if(roles.contains(value.name())) privileges.add(value);
        }
        return privileges;
    }

    public boolean hasRole(String role){
        return this.getRoleList().contains(Role.valueOf(role));
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
