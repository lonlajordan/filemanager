package com.filemanager.enums;

public enum Role {
    ROLE_ADMIN("ADMINISTRATEUR"),
    ROLE_GIE("GIE"),
    ROLE_BANK_MONET("BANK_MONET"),
    ROLE_BANK_INFO("BANK_INFO");

    private final String displayValue;

    Role(String displayValue) {
        this.displayValue = displayValue;
    }

    public String getDisplayValue() {
        return displayValue;
    }
}
