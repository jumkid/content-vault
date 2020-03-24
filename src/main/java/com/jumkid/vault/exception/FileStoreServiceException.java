package com.jumkid.vault.exception;

import com.jumkid.vault.controller.dto.MediaFile;
import lombok.Getter;

@Getter
public class FileStoreServiceException extends RuntimeException {

    private final MediaFile mediaFile;

    public FileStoreServiceException(String errorMsg) {
        super(errorMsg);
        this.mediaFile = null;
    }

    public FileStoreServiceException(String errorMsg, MediaFile mediaFile){
        super(errorMsg);
        this.mediaFile = mediaFile;
    }
	
}
