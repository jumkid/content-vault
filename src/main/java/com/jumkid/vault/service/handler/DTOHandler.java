package com.jumkid.vault.service.handler;

import com.jumkid.share.service.dto.GenericDTOHandler;
import com.jumkid.share.user.UserProfile;
import com.jumkid.share.user.UserProfileManager;
import com.jumkid.vault.controller.dto.MediaFile;
import com.jumkid.vault.model.MediaFileMetadata;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component("dtoHandler")
public class DTOHandler extends GenericDTOHandler {

    public DTOHandler(UserProfileManager userProfileManager) {
        super(userProfileManager);
    }

    public void normalize(String uuid, MediaFile dto, MediaFileMetadata oldMetadata) {
        if (uuid != null) {
            dto.setUuid(uuid);
        }

        UserProfile userProfile = userProfileManager.fetchUserProfile();
        String userId = userProfile.getId();
        LocalDateTime now = LocalDateTime.now();
        dto.setModifiedOn(now);

        if (oldMetadata != null) {
            dto.setModifiedBy(userId);
            dto.setCreatedBy(oldMetadata.getCreatedBy());
            dto.setCreatedOn(oldMetadata.getCreatedOn());
        } else {
            dto.setCreatedBy(userId);
            dto.setCreatedOn(now);
        }

        if (dto.getActivated() == null) {
            dto.setActivated(true);
        }
    }
}
