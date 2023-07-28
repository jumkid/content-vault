package com.jumkid.vault.exception;

public class GalleryNotEmptyException extends RuntimeException{

    private static final String ERROR = "Target media gallery is not empty. id: ";

    public GalleryNotEmptyException(String toGalleryId) { super(ERROR + toGalleryId); }

}
