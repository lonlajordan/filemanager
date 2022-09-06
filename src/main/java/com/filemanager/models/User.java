package com.filemanager.models;

public class User {
    private String username = "";
    private String password = "";
    private String roles = "";

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

    public boolean hasRole(String role){
        return this.roles.contains(role);
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
