package com.jumkid.vault.util;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Optional;

@Slf4j
public class FileUtils {

    private static final String ERROR_MSG = "Failed to read file channel to byte array. {}";

    public static Optional<byte[]> fileChannelToBytes(FileChannel fc) {
        if (fc != null) {
            try {
                ByteBuffer buffer = ByteBuffer.allocate((int) fc.size());
                fc.read(buffer);
                buffer.flip();

                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);
                return Optional.of(bytes);
            } catch (IOException ioe) {
                log.error(ERROR_MSG, ioe.getMessage());
            } finally {
                try {
                    fc.close();
                } catch (IOException ioe) {
                    log.error(ERROR_MSG, ioe.getMessage());
                }

            }
        }
        return Optional.empty();
    }

}
