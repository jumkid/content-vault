package com.jumkid.vault.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ThumbnailNamespace {

    SMALL("small"),
    LARGE("large"),
    SMALL_SUFFIX("_thmb"),
    LARGE_SUFFIX("_thmb_l");

    @JsonValue
    private final String value;

    ThumbnailNamespace(String value) { this.value = value; }

    public String value() { return value; }
}
