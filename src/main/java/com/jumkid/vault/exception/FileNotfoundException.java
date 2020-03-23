package com.jumkid.vault.exception;

public class FileNotfoundException extends RuntimeException {

    private static final String ERROR = "Can not find media file with Id: ";

    public FileNotfoundException(String id) { super(ERROR + id); }

}
