package com.filemanager.enums;

public enum Level {
    WARN("Avertissement"),
    ERROR("Erreur"),
    INFO("Information");

    private final String displayValue;

    Level(String displayValue) {
        this.displayValue = displayValue;
    }

    public String getDisplayValue() {
        return displayValue;
    }
}
