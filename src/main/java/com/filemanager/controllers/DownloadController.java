package com.filemanager.controllers;

import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/download")
public class DownloadController {

    private final Logger logger = LoggerFactory.getLogger(DownloadController.class);

    @GetMapping("file")
    public void downloadFile(@RequestParam String path, HttpServletResponse response) {
        try {
            Path filePath = Paths.get(URLDecoder.decode(path, String.valueOf(StandardCharsets.UTF_8))).toAbsolutePath().normalize();
            File file = filePath.toFile();
            if(!file.isDirectory()){
                InputStream inputStream = new InputStreamResource(new FileInputStream(file)).getInputStream();
                response.setContentType(String.valueOf(MediaType.APPLICATION_OCTET_STREAM));
                response.setHeader("Content-Transfer-Encoding", "binary");
                response.setHeader("Content-Disposition", "attachment; filename=" + file.getName());
                IOUtils.copy(inputStream, response.getOutputStream());
                inputStream.close();
                response.flushBuffer();
                response.setStatus(HttpServletResponse.SC_OK);
            }else{
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
        }
    }

    @GetMapping(path = "folder")
    public ResponseEntity<StreamingResponseBody> downloadFolderAsZip(@RequestParam String path, HttpServletResponse response) {
        try {
            Path filePath = Paths.get(URLDecoder.decode(path, String.valueOf(StandardCharsets.UTF_8))).toAbsolutePath().normalize();
            File folder = filePath.toFile();
            if(!folder.isDirectory()) throw new Exception("excepting folder instead of file");
            StreamingResponseBody streamResponseBody = out -> {
                final ZipOutputStream zipOutputStream = new ZipOutputStream(response.getOutputStream());
                zipFile(folder, zipOutputStream);
            };
            response.setHeader("Content-Transfer-Encoding", "binary");
            response.setHeader("Content-Disposition", "attachment; filename=" + folder.getName() + ".zip");
            return ResponseEntity.ok(streamResponseBody);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping(path = "files")
    public ResponseEntity<StreamingResponseBody> downloadFilesAsZip(@RequestParam String[] paths, HttpServletResponse response) {
        StreamingResponseBody streamResponseBody = out -> {

            final ZipOutputStream zipOutputStream = new ZipOutputStream(response.getOutputStream());
            ZipEntry zipEntry;
            InputStream inputStream = null;

            try {
                for (String path : paths) {
                    Path filePath = Paths.get(URLDecoder.decode(path, String.valueOf(StandardCharsets.UTF_8))).toAbsolutePath().normalize();
                    File file = filePath.toFile();
                    zipEntry = new ZipEntry(filePath.toString());
                    inputStream = new FileInputStream(file);
                    zipOutputStream.putNextEntry(zipEntry);
                    byte[] bytes = Files.readAllBytes(file.toPath());
                    zipOutputStream.write(bytes, 0, bytes.length);
                    zipOutputStream.closeEntry();
                }
            } catch (IOException e) {
                logger.error("Exception while reading and streaming data", e);
            } finally {
                if (inputStream != null) {
                    inputStream.close();
                }
                zipOutputStream.close();
            }

        };

        response.setHeader("Content-Transfer-Encoding", "binary");
        response.setHeader("Content-Disposition", "attachment; filename=archive.zip");
        return ResponseEntity.ok(streamResponseBody);
    }

    private static void zipFile(File fileToZip, ZipOutputStream zipOut) {
        final Path sourceDir = Paths.get(fileToZip.toURI());
        try {
            Files.walkFileTree(sourceDir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
                    try {
                        Path targetFile = sourceDir.relativize(file);
                        zipOut.putNextEntry(new ZipEntry(targetFile.toString()));
                        byte[] bytes = Files.readAllBytes(file);
                        zipOut.write(bytes, 0, bytes.length);
                        zipOut.closeEntry();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
            zipOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
