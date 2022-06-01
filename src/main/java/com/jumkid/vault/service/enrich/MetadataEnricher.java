package com.jumkid.vault.service.enrich;

import com.jumkid.vault.model.MediaFileMetadata;
import com.jumkid.vault.util.DateTimeUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.tika.Tika;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.HttpHeaders;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeParseException;

@Slf4j
@Component
public class MetadataEnricher {

    private static final String WHITESPACE = "";

    private final Tika tika;
    final Parser parser;

    @Autowired
    public MetadataEnricher() {
        this.tika = new Tika();
        this.parser = new AutoDetectParser();
    }

    public void enrichProps(MediaFileMetadata mediaFileMetadata, byte[] bytes) {

        final BodyContentHandler handler = new BodyContentHandler();
        final Metadata metadata = new Metadata();
        final ParseContext context = new ParseContext();

        try (TikaInputStream tikaStream = TikaInputStream.get(bytes)) {
            String mimeType = tika.detect(tikaStream);
            metadata.set(HttpHeaders.CONTENT_TYPE, mimeType);

            parser.parse(tikaStream, handler, metadata, context);

            for (String metaName : metadata.names()) {

                String metaValue = metadata.get(metaName);
                if (metaValue == null || metaValue.isBlank()) continue;

                if (metaName.toLowerCase().contains("date") || metaName.toLowerCase().contains("modified")) {
                    addDatetimeProp(mediaFileMetadata, metaValue, metaName);
                } else {
                    if (NumberUtils.isParsable(metaValue)) mediaFileMetadata.addProp(metaName, NumberUtils.createNumber(metaValue));
                    else mediaFileMetadata.addProp(metaName, metaValue);
                }

            }

        } catch (Exception e) {
            log.error("Metadata parsing exception {}", e.getMessage());
        }
    }

    private void addDatetimeProp(MediaFileMetadata mediaFileMetadata, String metaValue, String metaName) {
        try {
            mediaFileMetadata.addProp(metaName, DateTimeUtils.stringToLocalDatetime(metaValue));
        } catch (DateTimeParseException ex) {
            log.error("meta={}  |  {}", metaName, ex.getMessage());
            //mediaFileMetadata.addProp(metaName, metaValue + WHITESPACE); //add whitespace here to escape date type in ES
            mediaFileMetadata.addProp(metaName, metaValue);
        }
    }

}
