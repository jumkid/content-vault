package com.jumkid.vault.enums;

public enum StorageMode {

    LOCAL("local"), HADOOP("hadoop");

    private String value;

    private StorageMode(String value) { this.value = value; }

    public String value() { return this.value; }

}
