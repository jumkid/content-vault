package com.jumkid.vault.exception;

public class FileNotFoundException extends RuntimeException {

    private static final String ERROR = "Can not find media file with Id: ";

    public FileNotFoundException(String id) { super(ERROR + id); }

}
