package com.filemanager.enums;

public enum Role {
    ROLE_ADMIN("ADMINISTRATEUR"),
    ROLE_GIE(""),
    ROLE_CBC_MONET(""),
    ROLE_CBC_INFO(""),
    ROLE_CBT_MONET(""),
    ROLE_CBT_INFO("");

    private final String displayValue;

    Role(String displayValue) {
        this.displayValue = displayValue;
    }

    public String getDisplayValue() {
        return displayValue;
    }
}
