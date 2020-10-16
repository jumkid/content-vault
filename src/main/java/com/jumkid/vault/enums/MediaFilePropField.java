package com.jumkid.vault.enums;

public enum MediaFilePropField {
    NAME("name"),
    VALUE("value"),
    DATATYPE("dataType");

    private final String value;

    MediaFilePropField(String value) { this.value = value; }

    public String value() { return this.value; }

}
