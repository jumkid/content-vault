package com.jumkid.vault.service;

import com.jumkid.vault.model.MediaFileMetadata;
import com.jumkid.vault.repository.MetadataStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import static com.jumkid.share.util.Constants.*;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service("securityService")
public class MediaFileSecurityServiceImpl implements MediaFileSecurityService{

    private final MetadataStorage metadataStorage;

    @Autowired
    public MediaFileSecurityServiceImpl(MetadataStorage metadataStorage) {
        this.metadataStorage = metadataStorage;
    }

    @Override
    public boolean isOwner(Authentication authentication, String mediaFileId) {
        MediaFileMetadata metadata = metadataStorage.getMetadata(mediaFileId);
        String currentUsername;
        if (authentication == null) {
            currentUsername = getCurrentUserName();
        } else {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            currentUsername = userDetails.getUsername();
        }

        String createdBy = metadata.getCreatedBy();
        if (createdBy == null) { return true; }
        else { return metadata.getCreatedBy().equals(currentUsername); }
    }

    @Override
    public String getCurrentUserName() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof AnonymousAuthenticationToken)) {
            return authentication.getName();
        } else {
            return ANONYMOUS_USER;
        }
    }

    @Override
    public List<String> getCurrentUserRoles() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof AnonymousAuthenticationToken)) {
            return authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList());
        } else {
            return List.of(GUEST_ROLE);
        }
    }

}