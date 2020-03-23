package com.jumkid.vault.enums;

public enum ThumbnailInfo {
    SMALL_SUFFIX("_thmb"),
    LARGE_SUFFIX("_thmb_l"),
    EXTENSION(".png");

    String value;

    ThumbnailInfo(String value) { this.value = value; }

    public String value() { return value; }
}
