package com.jumkid.vault.service;

import com.jumkid.share.security.AccessScope;
import com.jumkid.vault.exception.FileNotAvailableException;
import com.jumkid.vault.exception.FileNotFoundException;
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
import java.util.Optional;

@Slf4j
@Service("securityService")
public class MediaFileSecurityServiceImpl implements MediaFileSecurityService{

    private final MetadataStorage metadataStorage;

    @Autowired
    public MediaFileSecurityServiceImpl(MetadataStorage metadataStorage) {
        this.metadataStorage = metadataStorage;
    }

    @Override
    public boolean isPublic(String mediaFileId) {
        MediaFileMetadata metadata = getMetadata(mediaFileId);
        return AccessScope.PUBLIC.equals(metadata.getAccessScope());
    }

    @Override
    public boolean isOwner(Authentication authentication, String mediaFileId) {
        MediaFileMetadata metadata = getMetadata(mediaFileId);

        if (Boolean.FALSE.equals(metadata.getActivated())) throw new FileNotAvailableException();

        String currentUserId;
        if (authentication == null) {
            currentUserId = getCurrentUserId();
        } else {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            currentUserId = userDetails.getPassword();
        }

        String createdBy = metadata.getCreatedBy();
        if (createdBy == null) { return true; }
        else { return metadata.getCreatedBy().equals(currentUserId); }
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
    public String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof AnonymousAuthenticationToken)) {
            UserDetails userDetails = (UserDetails)authentication.getPrincipal();
            return userDetails.getPassword();
        } else {
            return null;
        }
    }

    @Override
    public List<String> getCurrentUserRoles() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof AnonymousAuthenticationToken)) {
            return authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
        } else {
            return List.of(GUEST_ROLE);
        }
    }

    private MediaFileMetadata getMetadata(String mediaFileId){
        Optional<MediaFileMetadata> optional = metadataStorage.getMetadata(mediaFileId);

        if (optional.isEmpty()) throw new FileNotFoundException(mediaFileId);
        return optional.get();
    }

}
