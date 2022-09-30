package com.filemanager.enums;

public enum Role {
    ROLE_ADMIN("ADMINISTRATEUR"),
    ROLE_GIE("OPÉRATEUR GIE"),
    ROLE_BANK_MONET("MONÉTIQUE"),
    ROLE_BANK_INFO("INFORMATIQUE");

    private final String displayValue;

    Role(String displayValue) {
        this.displayValue = displayValue;
    }

    public String getDisplayValue() {
        return displayValue;
    }
}
