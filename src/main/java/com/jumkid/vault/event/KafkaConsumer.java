package com.jumkid.vault.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jumkid.vault.controller.dto.external.Vehicle;
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

    @KafkaListener(topics = "${spring.kafka.topic.name.vehicle.create}", groupId = "${spring.kafka.consumer.group-id}",
                containerFactory = "kafkaListenerContainerFactory")
    public void listenVehicleCreate(@Payload String message) {
        try {
            Vehicle vehicle = objectMapper.readValue(message, Vehicle.class);
            log.debug("Received kafka topic vehicle.create with payload : {}", vehicle);
        } catch (JsonMappingException jme) {
            jme.printStackTrace();
            log.error("failed to map message to json object: {}", jme.getMessage());
        } catch (JsonProcessingException jpe) {
            jpe.printStackTrace();
            log.error("failed to map process json message: {}", jpe.getMessage());
        }

    }

    @KafkaListener(topics = "${spring.kafka.topic.name.vehicle.delete}", groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory")
    public void listenVehicleDelete(@Payload String message) {
        try {
            Vehicle vehicle = objectMapper.readValue(message, Vehicle.class);
            log.debug("Received kafka topic vehicle.delete with payload : {}", vehicle);

            if (vehicle != null && vehicle.getMediaGalleryId() != null) {
                mediaFileService.trashMediaFile(vehicle.getMediaGalleryId());
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
