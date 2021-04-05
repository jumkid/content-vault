package com.jumkid.vault.config;

import com.jumkid.vault.enums.ThumbnailNamespace;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new StringToThumbnailNamespaceConverter());
    }

    private static class StringToThumbnailNamespaceConverter implements Converter<String, ThumbnailNamespace> {
        @Override
        public ThumbnailNamespace convert(String source) {
            return ThumbnailNamespace.valueOf(source.toUpperCase());
        }
    }

}
