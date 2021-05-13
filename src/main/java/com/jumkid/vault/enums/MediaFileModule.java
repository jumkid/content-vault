package com.jumkid.vault.enums;

public enum MediaFileModule {

    FILE("file"), TEXT("text"), HTML("html"), GALLERY("gallery"), REFERENCE("reference");

    private final String value;

    MediaFileModule(String value) { this.value = value; }

    public String value() { return this.value; }

}
