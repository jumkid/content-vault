package com.jumkid.vault;

import com.jumkid.vault.model.MediaFileMetadata;
import com.jumkid.vault.model.MediaFileProp;

import java.util.List;

class APITestsSetup {

    static String DUMMY_ID = "dummy-id";

    MediaFileMetadata buildMetadata() {
        return MediaFileMetadata.builder()
                .id(DUMMY_ID).title("test.title").filename("upload-test.html")
                .content("<p>test.content</p>")
                .props(List.of(MediaFileProp.builder()
                        .name("author").textValue("Mr nobody")
                        .build()))
                .build();
    }

}
