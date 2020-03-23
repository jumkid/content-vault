package com.jumkid.vault.enums;

public enum MediaFileField {
    ID("id"),
    TITLE("title"),
    FILENAME("filename"),
    MIMETYPE("mimeType"),
    SIZE("size"),
    MODULE("module"),
    CREATION_DATE("creationDate"),
    CREATED_BY("createdBy"),
    MODIFICATION_DATE("modificationDate"),
    MODIFIED_BY("modifiedBy"),
    CONTENT("content"),
    ACTIVATED("activated"),
    BLOB("blob"),
    LOGICALPATH("logicalPath");

    private final String value;

    MediaFileField(String value) { this.value = value; }

    public String value() { return this.value; }
}
