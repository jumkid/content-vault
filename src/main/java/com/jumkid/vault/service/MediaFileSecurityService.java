package com.jumkid.vault.service;

import com.jumkid.vault.exception.FileNotAvailableException;
import com.jumkid.vault.exception.FileNotFoundException;
import com.jumkid.vault.exception.FileStoreServiceException;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface MediaFileSecurityService {

    boolean isPublic(String mediaFileId) throws FileStoreServiceException, FileNotFoundException;

    boolean isOwner(Authentication authentication, String mediaFileId) throws FileNotAvailableException, FileStoreServiceException, FileNotFoundException;

    String getCurrentUserName();

    String getCurrentUserId();

    List<String> getCurrentUserRoles();
}
