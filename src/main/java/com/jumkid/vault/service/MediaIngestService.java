package com.jumkid.vault.service;

import com.jumkid.vault.controller.dto.MediaFile;
import com.jumkid.vault.enums.MediaFileModule;
import it.grabz.grabzit.GrabzItClient;
import it.grabz.grabzit.GrabzItFile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MediaIngestService {

    @Value("${vault.storage.mode}")
    private String storageMode;

    @Value("${grabzit.application.key}")
    private String grabzitAppKey;

    @Value("${grabzit.application.secret}")
    private String grabzitAppSecret;

    private final MediaFileService fileService;

    @Autowired
    public MediaIngestService(MediaFileService fileService) {
        this.fileService = fileService;
    }

    public MediaFile ingestUrlToImage(String title, String url) {
        try {
            GrabzItClient client = new GrabzItClient(grabzitAppKey, grabzitAppSecret);

            log.debug("convert url {} to image", url);

            client.URLToImage(url);
            GrabzItFile gFile = client.SaveTo();
            if (gFile != null) {
                MediaFile mediaFile = MediaFile.builder()
                        .title(title != null ? title : url) //TODO extract domain name only
                        .size(gFile.getBytes().length)
                        .mimeType("image/jpeg")
                        .build();

                //TODO set user info

                return fileService.addMediaFile(mediaFile, gFile.getBytes(), MediaFileModule.FILE);
            }

        } catch (Exception e) {
            log.error("grabzit failed to convert url to image {}", e.getMessage());
        }

        return null;
    }

}
