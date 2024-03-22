package com.jumkid.vault.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import static com.jumkid.share.util.Constants.YYYYMMDDTHHMMSS3S;

@Builder @Data @NoArgsConstructor @AllArgsConstructor
public class MediaFilePropMetadata {

    private String name;

    private String textValue;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = YYYYMMDDTHHMMSS3S)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime dateValue;

    private Number numberValue;

}
