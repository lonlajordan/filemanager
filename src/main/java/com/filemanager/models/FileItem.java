package com.filemanager.models;

import com.filemanager.services.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileOwnerAttributeView;
import java.nio.file.attribute.UserPrincipal;
import java.util.Date;

public class FileItem {
    private File file;
    private Date lastModifiedDate;
    private String fileSize = "";
    private String owner = "";
    private String permissions = "";
    private String absolutePath = "";
    private long size;

    public FileItem(File file) throws IOException {
        this.file = file;
        this.absolutePath = URLEncoder.encode(file.getAbsolutePath(), String.valueOf(StandardCharsets.UTF_8));
        Path path = Paths.get(file.toURI());
        BasicFileAttributes attr = Files.readAttributes(path, BasicFileAttributes.class);
        this.lastModifiedDate = Date.from(attr.lastModifiedTime().toInstant());
        this.size = attr.size();
        this.fileSize = this.file.isDirectory() ? "-" : FileUtils.readableFileSize(this.size);
        FileOwnerAttributeView view = Files.getFileAttributeView(path, FileOwnerAttributeView.class);
        UserPrincipal user = view.getOwner();
        this.owner = user.getName();
        try {
            this.permissions = FileUtils.toOctalFileMode(Files.getPosixFilePermissions(file.toPath())) + "";
        }catch (Exception ignored){}
    }

    public FileItem() {
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public Date getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(Date lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getFileSize() {
        return fileSize;
    }

    public void setFileSize(String fileSize) {
        this.fileSize = fileSize;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getPermissions() {
        return permissions;
    }

    public void setPermissions(String permissions) {
        this.permissions = permissions;
    }

    public String getAbsolutePath() {
        return absolutePath;
    }

    public void setAbsolutePath(String absolutePath) {
        this.absolutePath = absolutePath;
    }
}
