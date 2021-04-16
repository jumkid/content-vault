package com.jumkid.vault.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ThumbnailNamespace {

    SMALL("small"),
    MEDIUM("medium"),
    LARGE("large"),
    SMALL_SUFFIX("_thmb"),
    MEDIUM_SUFFIX("_thmb_m"),
    LARGE_SUFFIX("_thmb_l");

    @JsonValue
    private final String value;

    ThumbnailNamespace(String value) { this.value = value; }

    public String value() { return value; }
}
