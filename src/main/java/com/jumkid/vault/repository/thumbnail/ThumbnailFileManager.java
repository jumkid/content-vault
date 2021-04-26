package com.jumkid.vault.repository.thumbnail;

import com.jumkid.vault.enums.ThumbnailNamespace;
import com.jumkid.vault.model.MediaFileMetadata;
import com.jumkid.vault.repository.FilePathManager;
import com.jumkid.vault.util.FileUtils;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class ThumbnailFileManager {

    @Value("${vault.thumbnail.small}")
    private int thumbnailSmall;

    @Value("${vault.thumbnail.medium}")
    private int thumbnailMedium;

    @Value("${vault.thumbnail.large}")
    private int thumbnailLarge;

    @Value("#{${vault.thumbnail.icon-mappings}}")
    private Map<String, String> iconMappings;

    public static final String THUMBNAIL_FILE_EXTEND = "PNG";
    private static final String MISC_PATH = "misc";
    private static final String PATH_DELIMITER = "/";

    private final FilePathManager filePathManager;

    public ThumbnailFileManager(FilePathManager filePathManager) {
        this.filePathManager = filePathManager;
    }

    public Optional<byte[]> getThumbnail(MediaFileMetadata mediaFileMetadata, ThumbnailNamespace thumbnailNamespace) {

        String dataHomePath = filePathManager.getDataHomePath();
        String logicalPath = mediaFileMetadata.getLogicalPath();

        String filePath;
        if(mediaFileMetadata.getMimeType().startsWith("image")) {
            filePath = String.format("%s%s/%s%s.%s", dataHomePath, logicalPath, mediaFileMetadata.getId(),
                    this.getThumbnailSuffix(thumbnailNamespace).value(), ThumbnailFileManager.THUMBNAIL_FILE_EXTEND);
        } else {
            filePath = getThumbnailFilePath(mediaFileMetadata.getMimeType());
        }

        File file = new File(filePath);
        if(!file.exists()) {
            log.info("file in {} is not found.", filePath);
            return Optional.empty();
        }

        try (FileInputStream fin = new FileInputStream(file)) {
            return FileUtils.fileChannelToBytes(fin.getChannel());
        } catch(Exception e) {
            log.error("Failed to get file on {}", filePath);
            return Optional.empty();
        }

    }

    private String getThumbnailFilePath(String mimeType) {
        String dataHomePath = filePathManager.getDataHomePath();

        String filePath = String.join(PATH_DELIMITER, dataHomePath, MISC_PATH, "icon_file.png"); //default icon

        for (Map.Entry<String, String> mapping : iconMappings.entrySet()) {
            Pattern pattern = Pattern.compile(mapping.getKey(), Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(mimeType);
            if (matcher.find()) {
                filePath = String.join(PATH_DELIMITER, dataHomePath, MISC_PATH, mapping.getValue());
                break;
            }
        }

        return filePath;
    }

    public ThumbnailNamespace getThumbnailSuffix(ThumbnailNamespace thumbnailNamespace) {
        switch (thumbnailNamespace) {
            case SMALL: return ThumbnailNamespace.SMALL_SUFFIX;
            case LARGE: return ThumbnailNamespace.LARGE_SUFFIX;
            default: return ThumbnailNamespace.MEDIUM_SUFFIX;
        }
    }

    public void generateThumbnail(Path filePath) throws IOException {
        String path = filePath.toString();

        Thumbnails.of(new File(path))
                .size(thumbnailSmall, thumbnailSmall)
                .outputFormat(THUMBNAIL_FILE_EXTEND)
                .toFile(new File(path + ThumbnailNamespace.SMALL_SUFFIX.value()));

        Thumbnails.of(new File(path))
                .size(thumbnailMedium, thumbnailMedium)
                .outputFormat(THUMBNAIL_FILE_EXTEND)
                .toFile(new File(path + ThumbnailNamespace.MEDIUM_SUFFIX.value()));

        Thumbnails.of(new File(path))
                .size(thumbnailLarge, thumbnailLarge)
                .outputFormat(THUMBNAIL_FILE_EXTEND)
                .toFile(new File(path + ThumbnailNamespace.LARGE_SUFFIX.value()));

    }

    public void deleteThumbnail(MediaFileMetadata mediaFile) {

        if(mediaFile.getMimeType().startsWith("image")){
            Path pathS = getThumbnailPath(mediaFile, ThumbnailNamespace.SMALL_SUFFIX.value() + "." + THUMBNAIL_FILE_EXTEND);
            Path pathM = getThumbnailPath(mediaFile, ThumbnailNamespace.MEDIUM_SUFFIX.value() + "." + THUMBNAIL_FILE_EXTEND);
            Path pathL = getThumbnailPath(mediaFile, ThumbnailNamespace.LARGE_SUFFIX.value() + "." + THUMBNAIL_FILE_EXTEND);

            try {
                Files.deleteIfExists(pathS);
                Files.deleteIfExists(pathM);
                Files.deleteIfExists(pathL);
            } catch (IOException e) {
                log.warn("Failed to remove thumbnail files {}", e.getMessage());
            }

        }

    }

    private Path getThumbnailPath(MediaFileMetadata mediaFile, String suffix){
        return Paths.get(filePathManager.getDataHomePath(), mediaFile.getLogicalPath(), mediaFile.getId() + suffix);
    }

}
