package com.filemanager.enums;

public enum Institution {
    GIE("GIE-GCB"),
    CBC("CBC"),
    CBT("CBT");

    private final String displayValue;

    Institution(String displayValue) {
        this.displayValue = displayValue;
    }

    public String getDisplayValue() {
        return displayValue;
    }
}
