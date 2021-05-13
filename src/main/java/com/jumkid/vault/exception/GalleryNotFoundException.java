package com.jumkid.vault.exception;

public class GalleryNotFoundException extends RuntimeException{

    private static final String ERROR = "Can not find media gallery with Id: ";

    public GalleryNotFoundException(String mediaFileId) { super(ERROR + mediaFileId); }
}
