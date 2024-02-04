package com.jumkid.vault.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum MediaFileModule {

    FILE("FILE"), TEXT("TEXT"), HTML("HTML"), GALLERY("GALLERY"), REFERENCE("REFERENCE");

    @JsonValue
    private final String value;

    MediaFileModule(String value) { this.value = value; }

    public String value() { return this.value; }

}
