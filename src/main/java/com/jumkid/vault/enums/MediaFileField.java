package com.jumkid.vault.enums;

public enum MediaFileField {
    ID("id"),
    TITLE("title"),
    FILENAME("filename"),
    MIME_TYPE("mimeType"),
    SIZE("size"),
    MODULE("module"),
    CREATED_ON("createdOn"),
    CREATED_BY("createdBy"),
    MODIFIED_ON("modifiedOn"),
    MODIFIED_BY("modifiedBy"),
    CONTENT("content"),
    ACCESS_SCOPE("accessScope"),
    ACTIVATED("activated"),
    BLOB("blob"),
    LOGICAL_PATH("logicalPath"),
    PROPS("props"),
    TAGS("tags"),
    CHILDREN("children");

    private final String value;

    MediaFileField(String value) { this.value = value; }

    public String value() { return this.value; }
}
