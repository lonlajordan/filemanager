package com.filemanager.controllers;

import com.filemanager.models.Log;
import com.filemanager.repositories.LogRepository;
import org.apache.commons.lang3.exception.ExceptionUtils;
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
import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.Principal;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/download")
public class DownloadController {

    private static final Logger logger = LoggerFactory.getLogger(DownloadController.class);

    private final LogRepository logRepository;

    public DownloadController(LogRepository logRepository) {
        this.logRepository = logRepository;
    }

    @GetMapping("file")
    public void downloadFile(@RequestParam String path, HttpServletResponse response, Principal principal) {
        Path filePath = null;
        try {
            filePath = Paths.get(URLDecoder.decode(path, String.valueOf(StandardCharsets.UTF_8))).toAbsolutePath().normalize();
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
                logRepository.save(Log.info("Le fichier <b>" + filePath + "</b> a été téléchargé par <b>" + principal.getName() + "</b>"));
            }else{
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                logRepository.save(Log.warn("Le fichier <b>" + filePath + "</b> est introuvable"));
            }
        } catch (Exception e) {
            logRepository.save(Log.error("Erreur de téléchargement du fichier <b>" + filePath + "</b> par <b>" + principal.getName() + "</b>", ExceptionUtils.getStackTrace(e)));
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
        }
    }

    @GetMapping(path = "folder")
    public ResponseEntity<StreamingResponseBody> downloadFolderAsZip(@RequestParam String path, HttpServletResponse response, Principal principal) {
        Path filePath = null;
        try {
            filePath = Paths.get(URLDecoder.decode(path, String.valueOf(StandardCharsets.UTF_8))).toAbsolutePath().normalize();
            File folder = filePath.toFile();
            if(!folder.isDirectory()) throw new Exception("excepting folder instead of file");
            StreamingResponseBody streamResponseBody = out -> {
                final ZipOutputStream zipOutputStream = new ZipOutputStream(response.getOutputStream());
                zipFile(folder, zipOutputStream, logRepository);
            };
            response.setHeader("Content-Transfer-Encoding", "binary");
            response.setHeader("Content-Disposition", "attachment; filename=" + folder.getName() + ".zip");
            logRepository.save(Log.info("Le dossier <b>" + filePath + "</b> a été téléchargé par <b>" + principal.getName() + "</b>"));
            return ResponseEntity.ok(streamResponseBody);
        } catch (Exception e) {
            logRepository.save(Log.error("Erreur de téléchargement du dossier <b>" + filePath + "</b> par <b>" + principal.getName() + "</b>", ExceptionUtils.getStackTrace(e)));
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping(path = "files")
    public ResponseEntity<StreamingResponseBody> downloadFilesAsZip(@RequestParam(name = "path") String[] paths, HttpServletResponse response, Principal principal) {
        StreamingResponseBody streamResponseBody = out -> {
            final ZipOutputStream zipOutputStream = new ZipOutputStream(response.getOutputStream());
            final List<Path> pathList = Stream.of(paths).map(path -> {
                try {
                    return Paths.get(URLDecoder.decode(path, String.valueOf(StandardCharsets.UTF_8))).toAbsolutePath().normalize();
                } catch (UnsupportedEncodingException ignored) { }
                return null;
            }).filter(Objects::nonNull).collect(Collectors.toList());
            zipFiles(pathList, zipOutputStream);
            new Thread(() -> pathList.forEach(path -> {
                logRepository.save(Log.info("Téléchargement de <b>" + path + "</b> par <b>" + principal.getName() + "</b>"));
            })).start();
        };

        response.setHeader("Content-Transfer-Encoding", "binary");
        response.setHeader("Content-Disposition", "attachment; filename=archive.zip");
        return ResponseEntity.ok(streamResponseBody);
    }

    private static void zipFile(File fileToZip, ZipOutputStream zipOut, LogRepository logRepository) {
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
                        logRepository.save(Log.error("Erreur lors de la compression de <b>" + file + "</b>", ExceptionUtils.getStackTrace(e)));
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
            zipOut.close();
        } catch (IOException e) {
            logRepository.save(Log.error("Erreur lors de la compression de <b>" + sourceDir + "</b>", ExceptionUtils.getStackTrace(e)));
        }
    }

    private static void zipFiles(List<Path> paths, ZipOutputStream zipOut) {
        if(paths.isEmpty()){
            try {
                zipOut.close();
            } catch (IOException e) {
                logger.error("ZipOutputStream closing error", e);
            }
            return;
        }
        final Path sourceDir = paths.get(0).getParent();
        try {
            Files.walkFileTree(sourceDir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
                    try {
                        if(!file.getParent().equals(sourceDir) || paths.contains(file)){
                            Path targetFile = sourceDir.relativize(file);
                            zipOut.putNextEntry(new ZipEntry(targetFile.toString()));
                            byte[] bytes = Files.readAllBytes(file);
                            zipOut.write(bytes, 0, bytes.length);
                            zipOut.closeEntry();
                        }
                    } catch (IOException e) {
                        logger.error("zip entry error", e);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
            zipOut.close();
        } catch (IOException e) {
            logger.error("multiple files compression error", e);
        }
    }
}
