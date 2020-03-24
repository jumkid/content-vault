package com.jumkid.vault.enums;

public enum SystemDirectoryName {
    PUBLIC("public"),
    PRIVATE("private"),
    TRASH("trash");

    private String value;

    SystemDirectoryName(String value) { this.value = value; }

    public String value() { return this.value; }

}
