package com.jumkid.vault.repository.thumbnail;

import com.jumkid.vault.enums.MediaFileModule;
import com.jumkid.vault.enums.ThumbnailNamespace;
import com.jumkid.vault.model.MediaFileMetadata;
import com.jumkid.vault.model.MediaFilePropMetadata;
import com.jumkid.vault.repository.FileMetadata;
import com.jumkid.vault.repository.FilePathManager;
import com.jumkid.vault.util.FileUtils;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.jumkid.vault.util.Constants.PROP_FEATURED_ID;

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

    private final FileMetadata<MediaFileMetadata> metadataStorage;

    @Autowired
    public ThumbnailFileManager(FilePathManager filePathManager, FileMetadata<MediaFileMetadata> metadataStorage) {
        this.filePathManager = filePathManager;
        this.metadataStorage = metadataStorage;
    }

    public Optional<byte[]> getThumbnail(MediaFileMetadata mediaFileMetadata, ThumbnailNamespace thumbnailNamespace) {
        String filePath;

        if (mediaFileMetadata.getModule().equals(MediaFileModule.GALLERY)) {
            filePath = getThumbnailFilePathForGallery(mediaFileMetadata, thumbnailNamespace);
        } else if (mediaFileMetadata.getMimeType().startsWith("image")) {
            filePath = getThumbnailFilePathForMediaFile(mediaFileMetadata, thumbnailNamespace);
        } else {
            filePath = getIconFilePath(mediaFileMetadata.getMimeType());
        }

        File file = new File(filePath);
        if (!file.exists()) {
            return Optional.empty();
        }

        try (FileInputStream fin = new FileInputStream(file)) {
            return FileUtils.fileChannelToBytes(fin.getChannel());
        } catch(Exception e) {
            log.error("Failed to get file on {}", filePath);
            return Optional.empty();
        }

    }

    public Optional<MediaFileMetadata> getThumbnailFileForGallery(MediaFileMetadata galleryMetadata) {
        Optional<MediaFilePropMetadata> optional = galleryMetadata.getProps().stream()
                .filter(prop -> prop.getName().equals(PROP_FEATURED_ID))
                .findFirst();

        if (optional.isPresent()) {   //get featured image as thumbnail
            String featuredId = optional.get().getTextValue();
            return metadataStorage.getMetadata(featuredId);
        } else {
            return Optional.empty();
        }
    }

    private String getThumbnailFilePathForGallery(MediaFileMetadata galleryMetadata, ThumbnailNamespace thumbnailNamespace) {
        if (galleryMetadata.getProps() != null && !galleryMetadata.getProps().isEmpty()) {
            Optional<MediaFileMetadata> featuredOptional = getThumbnailFileForGallery(galleryMetadata);

            if (featuredOptional.isPresent()) {
                return getThumbnailFilePathForMediaFile(featuredOptional.get(), thumbnailNamespace);
            }
        }

        return getIconFilePath(MediaFileModule.GALLERY.value());
    }

    private String getThumbnailFilePathForMediaFile(MediaFileMetadata mediaFileMetadata, ThumbnailNamespace thumbnailNamespace) {
        Path thumbnailFilePath = getThumbnailFilePath(mediaFileMetadata, thumbnailNamespace);

        if (!Files.exists(thumbnailFilePath)) {
            // fetch other thumbnail file if the target one does not exist
            for (ThumbnailNamespace ns : ThumbnailNamespace.getMainSet().toArray(new ThumbnailNamespace[0])){
                if (!thumbnailNamespace.equals(ns)) {
                    thumbnailFilePath = getThumbnailFilePath(mediaFileMetadata, ns);
                    if (Files.exists(thumbnailFilePath)) return thumbnailFilePath.toString();
                }
            }

        }

        return thumbnailFilePath.toString();
    }

    private Path getThumbnailFilePath(MediaFileMetadata mediaFileMetadata, ThumbnailNamespace thumbnailNamespace) {
        Path rootPath = Paths.get(filePathManager.getDataHomePath(), mediaFileMetadata.getLogicalPath());
        String thumbnailFileName = String.format("%s%s.%s", mediaFileMetadata.getId(),
                this.getThumbnailSuffix(thumbnailNamespace).value(), ThumbnailFileManager.THUMBNAIL_FILE_EXTEND);

        return Paths.get(rootPath.toString(), thumbnailFileName);
    }

    private String getIconFilePath(String mimeType) {
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
        return switch (thumbnailNamespace) {
            case SMALL -> ThumbnailNamespace.SMALL_SUFFIX;
            case LARGE -> ThumbnailNamespace.LARGE_SUFFIX;
            default -> ThumbnailNamespace.MEDIUM_SUFFIX;
        };
    }

    public void generateThumbnail(Path filePath) {
        String path = filePath.toString();

        try {
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
        } catch (IOException ioe) {
            ioe.printStackTrace();
            log.info("unable to generate thumbnails for file due to {}", ioe.getMessage());
        }

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
