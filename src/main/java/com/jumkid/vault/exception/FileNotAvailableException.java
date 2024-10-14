package com.jumkid.vault.exception;

public class FileNotAvailableException extends Exception{

    private static final String ERROR = "The media file is not available ";

    public FileNotAvailableException() { super(ERROR); }

    public FileNotAvailableException(String errorMessage) { super(errorMessage == null ? ERROR : errorMessage); }
}
