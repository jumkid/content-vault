package com.jumkid.vault.exception;

public class InvalidFieldException extends RuntimeException {

    private static final String ERROR = "MediaFile prop type is invalid: ";

    public InvalidFieldException(String value) { super(ERROR + value); }

}
