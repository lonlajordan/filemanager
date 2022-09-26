package com.filemanager.enums;

public enum Role {
    ROLE_ADMIN("ADMINISTRATEUR"),
    ROLE_GIE("OPERATEUR GIE"),
    ROLE_BANK_MONET("MONETIQUE"),
    ROLE_BANK_INFO("INFORMATIQUE");

    private final String displayValue;

    Role(String displayValue) {
        this.displayValue = displayValue;
    }

    public String getDisplayValue() {
        return displayValue;
    }
}
