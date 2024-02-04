package com.jumkid.vault.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jumkid.share.event.ContentEvent;
import com.jumkid.vault.service.MediaFileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class KafkaConsumer {

    public final ObjectMapper objectMapper;

    public final MediaFileService mediaFileService;

    public KafkaConsumer(MediaFileService mediaFileService) {
        this.mediaFileService = mediaFileService;
        this.objectMapper = new ObjectMapper();
    }

    @KafkaListener(topics = "${com.jumkid.events.content.content-delete}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory")
    public void listenContentDelete(@Payload String message) {
        try {
            ContentEvent contentEvent = objectMapper.readValue(message, ContentEvent.class);
            log.debug("Received kafka topic vehicle.delete with payload : {}", contentEvent);

            if (contentEvent != null && contentEvent.getContentId() != null) {
                mediaFileService.trashMediaFile(contentEvent.getContentId());
            }
        } catch (JsonMappingException jme) {
            jme.printStackTrace();
            log.error("failed to map message to json object: {}", jme.getMessage());
        } catch (JsonProcessingException jpe) {
            jpe.printStackTrace();
            log.error("failed to map process json message: {}", jpe.getMessage());
        }

    }

}
