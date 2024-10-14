package com.jumkid.vault.exception;

public class FileNotFoundException extends Exception {

    private static final String ERROR = "Can not find media file with Id: ";

    public FileNotFoundException(String mediaFileId) { super(ERROR + mediaFileId); }

}
