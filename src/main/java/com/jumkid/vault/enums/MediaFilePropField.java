package com.jumkid.vault.enums;

public enum MediaFilePropField {
    NAME("name"),
    TEXT_VALUE("textValue"),
    DATE_VALUE("dateValue"),
    NUMBER_VALUE("numberValue");

    private final String value;

    MediaFilePropField(String value) { this.value = value; }

    public String value() { return this.value; }

}
