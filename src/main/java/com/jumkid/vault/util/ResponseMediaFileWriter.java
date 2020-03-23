package com.jumkid.vault.util;

/*
 * This software is written by Jumkid and subject
 * to a contract between Jumkid and its customer.
 *
 * This software stays property of Jumkid unless differing
 * arrangements between Jumkid and its customer apply.
 *
 *
 * (c)2013 Jumkid All rights reserved.
 *
 * VERSION   | DATE      | DEVELOPER  | DESC
 * -----------------------------------------------------------------
 * 3.0        Dec2014      chooli      creation
 *
 *
 */
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.jumkid.vault.controller.dto.MediaFile;
import com.jumkid.vault.model.MediaFileMetadata;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ResponseMediaFileWriter {

    private static final long DEFAULT_EXPIRE_TIME = 604800000L; // ..ms = 1 week.

    private static final String MULTIPART_BOUNDARY = "MULTIPART_BYTERANGES";

    private static final int DEFAULT_BUFFER_SIZE = Constants.DEFAULT_1K;

    public String readSmallTextfile(FileChannel fc) throws IOException{
        byte[] buffer = new byte[(int)fc.size()];
        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);

        try {
            fc.read(byteBuffer);
            byteBuffer.rewind();
            byteBuffer.flip();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            fc.close();
        }
        return new String(buffer, Constants.DEFAULT_ENCODING);
    }

    public String readTextfile(FileChannel fc) throws IOException{
        StringBuffer sb = new StringBuffer();
        ByteBuffer byteBuffer = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE);
        try {
            while(fc.read(byteBuffer)!=-1){
                byteBuffer.flip();
                while (byteBuffer.hasRemaining()){
                    sb.append(Charset.forName(Constants.DEFAULT_ENCODING).decode(byteBuffer));
                }
                byteBuffer.clear();
            }
        } catch (Exception e) {
            log.error("failed to read media file {}", e.getMessage());
        } finally {
            fc.close();
        }
        return sb.toString();
    }

    /**
     * Write mediaFile to response with file channel (nio)
     *
     * @param mediaFile
     * @param fc
     * @param response
     * @return
     */
    public HttpServletResponse write(MediaFile mediaFile, FileChannel fc,
                                     HttpServletResponse response){
        try{
            setResponseParams(mediaFile, (int)fc.size(), response);
            _write(fc, response);
        } catch (IOException ioe) {
            log.error("failed to read media file {}", ioe.getMessage());
        }

        return response;
    }

    /**
     * Write mediaFile to response with file channel (nio)
     *
     * @param mediaFile
     * @param bytes
     * @param response
     * @return
     */
    public HttpServletResponse write(MediaFile mediaFile, byte[] bytes,
                                     HttpServletResponse response){
        try{
            setResponseParams(mediaFile, bytes.length, response);
            _write(bytes, response);
        } catch (IOException ioe) {
            log.error("failed to read media file {}", ioe.getMessage());
        }

        return response;
    }

    private void setResponseParams(MediaFile mediaFile, int fileSize, HttpServletResponse response) {
        response.setContentType(mediaFile.getMimeType());

        if (mediaFile.getMimeType().startsWith("text")) {
            response.setCharacterEncoding("UTF-8");
        } else {
            String fileName = (mediaFile.getFilename()==null ? mediaFile.getUuid() : mediaFile.getFilename());
            response.setHeader("Content-Disposition", "inline;filename=\"" + fileName + "\"");
            response.setContentLength(fileSize);
        }
    }

    /**
     * Write mediaFile to response with file channel (nio)
     *
     * @param mediaFileMetadata
     * @param fc
     * @param response
     * @return
     * @throws IOException
     */
    public HttpServletResponse writeForDownload(MediaFileMetadata mediaFileMetadata, FileChannel fc,
                                                HttpServletResponse response) throws IOException{

        String fileName = (mediaFileMetadata.getFilename()==null ? mediaFileMetadata.getId() : mediaFileMetadata.getFilename());

        response.setHeader("Content-Disposition", "attachment;filename=\"" +fileName+ "\"");
        response.setContentType(mediaFileMetadata.getMimeType());

        _write(fc, response);
        return response;

    }

    /**
     * Write source file bytes to response
     *
     * @param mediaFileMetadata
     * @param bytes
     * @param response
     * @throws IOException
     */
    public void writeForDownload(MediaFileMetadata mediaFileMetadata, byte[] bytes,
                                 HttpServletResponse response) throws IOException{
        String fileName = (mediaFileMetadata.getFilename()==null ? mediaFileMetadata.getId() : mediaFileMetadata.getFilename());

        response.setHeader("Content-Disposition", "attachment;filename=\"" +fileName+ "\"");
        response.setContentType(mediaFileMetadata.getMimeType());

        response.getOutputStream().write(bytes);
        response.getOutputStream().flush();
        response.getOutputStream().close();
    }

    public HttpServletResponse stream(MediaFile mediaFile, FileChannel fc,
                                      HttpServletRequest request, HttpServletResponse response) throws IOException{

        // Prepare some variables. The ETag is an unique identifier of the file.
        String fileName = mediaFile.getFilename();
        long length = mediaFile.getSize();
        long lastModified = Timestamp.valueOf(mediaFile.getCreationDate()).getTime();
        String eTag = fileName + "_" + length + "_" + lastModified;
        long expires = System.currentTimeMillis() + DEFAULT_EXPIRE_TIME;

        // Prepare some variables. The full Range represents the complete file.
        Range full = new Range(0, fc.size() - 1, fc.size());
        List<Range> ranges = new ArrayList<Range>();

        // Validate and process Range and If-Range headers.
        String range = request.getHeader("Range");
        if (range != null) {

            // Range header should match format "bytes=n-n,n-n,n-n...". If not, then return 416.
            if (!range.matches("^bytes=\\d*-\\d*(,\\d*-\\d*)*$")) {
                response.setHeader("Content-Range", "bytes */" + fc.size()); // Required in 416.
                response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                return response;
            }

            // If-Range header should either match ETag or be greater then LastModified. If not,
            // then return full file.
            String ifRange = request.getHeader("If-Range");
            if (ifRange != null && !ifRange.equals(eTag)) {
                try {
                    long ifRangeTime = request.getDateHeader("If-Range"); // Throws IAE if invalid.
                    if (ifRangeTime != -1 && ifRangeTime + 1000 < lastModified) {
                        ranges.add(full);
                    }
                } catch (IllegalArgumentException ignore) {
                    ranges.add(full);
                }
            }

            // If any valid If-Range header, then process each part of byte range.
            if (ranges.isEmpty()) {
                for (String part : range.substring(6).split(",")) {
                    // Assuming a file with length of 100, the following examples returns bytes at:
                    // 50-80 (50 to 80), 40- (40 to length=100), -20 (length-20=80 to length=100).
                    long start = sublong(part, 0, part.indexOf("-"));
                    long end = sublong(part, part.indexOf("-") + 1, part.length());

                    if (start == -1) {
                        start = length - end;
                        end = length - 1;
                    } else if (end == -1 || end > length - 1) {
                        end = length - 1;
                    }

                    // Check if Range is syntactically valid. If not, then return 416.
                    if (start > end) {
                        response.setHeader("Content-Range", "bytes */" + length); // Required in 416.
                        response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                        return response;
                    }

                    // Add range.
                    ranges.add(new Range(start, end, length));
                }
            }

        }


        // Prepare and initialize response --------------------------------------------------------

        // Get content type by file name and set default GZIP support and content disposition.
        String contentType = mediaFile.getMimeType();
        boolean acceptsGzip = false;
        String disposition = "inline";

        // If content type is unknown, then set the default value.
        // For all content types, see: http://www.w3schools.com/media/media_mimeref.asp
        // To add new content types, add new mime-mapping entry in web.xml.
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        // If content type is text, then determine whether GZIP content encoding is supported by
        // the browser and expand content type with the one and right character encoding.
        if (contentType.startsWith("text")) {
            String acceptEncoding = request.getHeader("Accept-Encoding");
            acceptsGzip = acceptEncoding != null && accepts(acceptEncoding, "gzip");
            contentType += ";charset=UTF-8";
        }

        // Else, expect for images, determine content disposition. If content type is supported by
        // the browser, then set to inline, else attachment which will pop a 'save as' dialogue.
        else if (!contentType.startsWith("image")) {
            String accept = request.getHeader("Accept");
            disposition = accept != null && accepts(accept, contentType) ? "inline" : "attachment";
        }

        // Initialize response.
        response.reset();
        response.setBufferSize(DEFAULT_BUFFER_SIZE);
        response.setHeader("Content-Disposition", disposition + ";filename=\"" + fileName + "\"");
        response.setHeader("Accept-Ranges", "bytes");
        response.setHeader("ETag", eTag);
        response.setDateHeader("Last-Modified", lastModified);
        response.setDateHeader("Expires", expires);


        // Send requested file (part(s)) to client ------------------------------------------------

        // Prepare streams.
        OutputStream output = null;

        try {
            // Open streams.
            output = response.getOutputStream();

            if (ranges.isEmpty() || ranges.get(0) == full) {

                // Return full file.
                Range r = full;
                response.setContentType(contentType);
                response.setHeader("Content-Range", "bytes " + r.start + "-" + r.end + "/" + r.total);

                if (acceptsGzip) {
                    // The browser accepts GZIP, so GZIP the content.
                    response.setHeader("Content-Encoding", "gzip");
                    output = new GZIPOutputStream(output, DEFAULT_BUFFER_SIZE);
                } else {
                    // Content length is not directly predictable in case of GZIP.
                    // So only add it if there is no means of GZIP, else browser will hang.
                    response.setHeader("Content-Length", String.valueOf(r.length));
                }

                // Copy full range.
                copy(fc, output, r.start, r.length);

            } else if (ranges.size() == 1) {

                // Return single part of file.
                Range r = ranges.get(0);
                response.setContentType(contentType);
                response.setHeader("Content-Range", "bytes " + r.start + "-" + r.end + "/" + r.total);
                response.setHeader("Content-Length", String.valueOf(r.length));
                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT); // 206.

                // Copy single part range.
                copy(fc, output, r.start, r.length);

            } else {

                // Return multiple parts of file.
                response.setContentType("multipart/byteranges; boundary=" + MULTIPART_BOUNDARY);
                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT); // 206.

                // Cast back to ServletOutputStream to get the easy println methods.
                ServletOutputStream sos = (ServletOutputStream) output;

                // Copy multi part range.
                for (Range r : ranges) {
                    // Add multipart boundary and header fields for every range.
                    sos.println();
                    sos.println("--" + MULTIPART_BOUNDARY);
                    sos.println("Content-Type: " + contentType);
                    sos.println("Content-Range: bytes " + r.start + "-" + r.end + "/" + r.total);

                    // Copy single part range of multi part range.
                    copy(fc, output, r.start, r.length);
                }

                // End with multipart boundary.
                sos.println();
                sos.println("--" + MULTIPART_BOUNDARY + "--");
            }
        } finally {
            // Gently close streams.
            fc.close();
        }

        return response;
    }

    /**
     * Read file with file channel and write to buffered output
     *
     * @param bytes
     * @param response
     * @return
     * @throws IOException
     */
    private HttpServletResponse _write(byte[] bytes, HttpServletResponse response) throws IOException{
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            baos.writeBytes(bytes);
            ServletOutputStream out = response.getOutputStream();
            baos.writeTo(out);
            out.flush();
        } catch (Exception e) {
            log.error("failed to read media file {}", e.getMessage());
        }

        return response;
    }

    /**
     * Read file with file channel and write to buffered output
     *
     * @param fc
     * @param response
     * @return
     * @throws IOException
     */
    private HttpServletResponse _write(FileChannel fc,
                                       HttpServletResponse response) throws IOException{

        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
        try{
            while (true) {
                final int len = fc.read(byteBuffer);
                if (len <= 0) {
                    break;
                }
                response.getOutputStream().write(buffer, 0, len);
                byteBuffer.clear();
            }
        }catch(Exception e){
            log.error("failed to read media file {}", e.getMessage());
        }finally{
            fc.close();
        }

        return response;
    }


    /**
     * Returns true if the given accept header accepts the given value.
     * @param acceptHeader The accept header.
     * @param toAccept The value to be accepted.
     * @return True if the given accept header accepts the given value.
     */
    private static boolean accepts(String acceptHeader, String toAccept) {
        String[] acceptValues = acceptHeader.split("\\s*(,|;)\\s*");
        Arrays.sort(acceptValues);
        return Arrays.binarySearch(acceptValues, toAccept) > -1
                || Arrays.binarySearch(acceptValues, toAccept.replaceAll("/.*$", "/*")) > -1
                || Arrays.binarySearch(acceptValues, "*/*") > -1;
    }

    /**
     * Copy the given byte range of the given input to the given output.
     *
     * @param fc
     * @param output
     * @param start
     * @param length
     * @throws IOException
     */
    private void copy(FileChannel fc, OutputStream output, long start, long length)
            throws IOException
    {
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
        int read;

        if (fc.size() == length) {

            while (true) {
                final int len = fc.read(byteBuffer);
                if (len <= 0) {
                    break;
                }
                output.write(buffer, 0, len);
                byteBuffer.clear();
            }

        } else {
            // Write partial range.
            fc.position(start);
            long toRead = length;

            while ((read = fc.read(byteBuffer)) > 0) {
                if ((toRead -= read) > 0) {
                    output.write(buffer, 0, read);
                } else {
                    output.write(buffer, 0, (int) toRead + read);
                    break;
                }
                byteBuffer.clear();
            }
        }
    }

    /**
     * Returns a substring of the given string value from the given begin index to the given end
     * index as a long. If the substring is empty, then -1 will be returned
     * @param value The string value to return a substring as long for.
     * @param beginIndex The begin index of the substring to be returned as long.
     * @param endIndex The end index of the substring to be returned as long.
     * @return A substring of the given string value as long or -1 if substring is empty.
     */
    private static long sublong(String value, int beginIndex, int endIndex) {
        String substring = value.substring(beginIndex, endIndex);
        return (substring.length() > 0) ? Long.parseLong(substring) : -1;
    }

    /**
     * This class represents a byte range.
     */
    protected class Range {
        long start;
        long end;
        long length;
        long total;

        /**
         * Construct a byte range.
         * @param start Start of the byte range.
         * @param end End of the byte range.
         * @param total Total length of the byte source.
         */
        Range(long start, long end, long total) {
            this.start = start;
            this.end = end;
            this.length = end - start + 1;
            this.total = total;
        }

    }

}
