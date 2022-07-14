package com.jumkid.vault.service;

import org.springframework.security.core.Authentication;

import java.util.List;

public interface MediaFileSecurityService {

    boolean isOwner(Authentication authentication, String mediaFileId);

    String getCurrentUserName();

    String getCurrentUserId();

    List<String> getCurrentUserRoles();
}
