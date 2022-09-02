package com.filemanager.services;

import java.io.File;
import java.nio.file.attribute.PosixFilePermission;
import java.text.DecimalFormat;
import java.util.Set;

public class FileUtils {
    private static final int OWNER_READ_FILEMODE = 0400;
    private static final int OWNER_WRITE_FILEMODE = 0200;
    private static final int OWNER_EXEC_FILEMODE = 0100;
    private static final int GROUP_READ_FILEMODE = 0040;
    private static final int GROUP_WRITE_FILEMODE = 0020;
    private static final int GROUP_EXEC_FILEMODE = 0010;
    private static final int OTHERS_READ_FILEMODE = 0004;
    private static final int OTHERS_WRITE_FILEMODE = 0002;
    private static final int OTHERS_EXEC_FILEMODE = 0001;

    /**
     * Converts a set of {@link PosixFilePermission} to chmod-style octal file mode.
     */
    public static int toOctalFileMode(Set<PosixFilePermission> permissions) {
        int result = 0;
        for (PosixFilePermission permissionBit : permissions) {
            switch (permissionBit) {
                case OWNER_READ:
                    result |= OWNER_READ_FILEMODE;
                    break;
                case OWNER_WRITE:
                    result |= OWNER_WRITE_FILEMODE;
                    break;
                case OWNER_EXECUTE:
                    result |= OWNER_EXEC_FILEMODE;
                    break;
                case GROUP_READ:
                    result |= GROUP_READ_FILEMODE;
                    break;
                case GROUP_WRITE:
                    result |= GROUP_WRITE_FILEMODE;
                    break;
                case GROUP_EXECUTE:
                    result |= GROUP_EXEC_FILEMODE;
                    break;
                case OTHERS_READ:
                    result |= OTHERS_READ_FILEMODE;
                    break;
                case OTHERS_WRITE:
                    result |= OTHERS_WRITE_FILEMODE;
                    break;
                case OTHERS_EXECUTE:
                    result |= OTHERS_EXEC_FILEMODE;
                    break;
            }
        }
        return result;
    }

    /**
     * Converts a set of {@link PosixFilePermission} to chmod-style octal file mode.
     */
    public static int toOctalFileMode(File file) {
        int result = 0;
        if(file.canRead()) result |= OWNER_READ_FILEMODE;
        if(file.canWrite()) result |= OWNER_WRITE_FILEMODE;
        if(file.canExecute()) result |= OWNER_EXEC_FILEMODE;
        /*for (PosixFilePermission permissionBit : permissions) {
            switch (permissionBit) {
                case OWNER_READ:
                    result |= OWNER_READ_FILEMODE;
                    break;
                case OWNER_WRITE:
                    result |= OWNER_WRITE_FILEMODE;
                    break;
                case OWNER_EXECUTE:
                    result |= OWNER_EXEC_FILEMODE;
                    break;
                case GROUP_READ:
                    result |= GROUP_READ_FILEMODE;
                    break;
                case GROUP_WRITE:
                    result |= GROUP_WRITE_FILEMODE;
                    break;
                case GROUP_EXECUTE:
                    result |= GROUP_EXEC_FILEMODE;
                    break;
                case OTHERS_READ:
                    result |= OTHERS_READ_FILEMODE;
                    break;
                case OTHERS_WRITE:
                    result |= OTHERS_WRITE_FILEMODE;
                    break;
                case OTHERS_EXECUTE:
                    result |= OTHERS_EXEC_FILEMODE;
                    break;
            }
        }*/
        return result;
    }

    public static String readableFileSize(long size) {
        if(size <= 0) return "0";
        final String[] units = new String[] { "o", "Ko", "Mo", "Go", "To" };
        int digitGroups = (int) (Math.log10(size)/Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size/Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }
    
    public static String icon(String extension){
        String icon = "";
        switch (extension) {
            case "ico":
            case "gif":
            case "jpg":
            case "jpeg":
            case "jpc":
            case "jp2":
            case "jpx":
            case "xbm":
            case "wbmp":
            case "png":
            case "bmp":
            case "tif":
            case "tiff":
            case "svg":
                icon = "fa fa-picture-o";
                break;
            case "passwd":
            case "ftpquota":
            case "sql":
            case "js":
            case "json":
            case "sh":
            case "config":
            case "twig":
            case "tpl":
            case "md":
            case "gitignore":
            case "c":
            case "cpp":
            case "cs":
            case "py":
            case "map":
            case "lock":
            case "dtd":
                icon = "fa fa-file-code-o";
                break;
            case "txt":
            case "ini":
            case "conf":
            case "log":
            case "csv":
            case "htaccess":
                icon = "fa fa-file-text-o";
                break;
            case "css":
            case "less":
            case "sass":
            case "scss":
                icon = "fa fa-css3";
                break;
            case "zip":
            case "rar":
            case "gz":
            case "tar":
            case "7z":
                icon = "fa fa-file-archive-o";
                break;
            case "php":
            case "php4":
            case "php5":
            case "phps":
            case "phtml":
                icon = "fa fa-code";
                break;
            case "htm":
            case "html":
            case "shtml":
            case "xhtml":
                icon = "fa fa-html5";
                break;
            case "xml":
            case "xsl":
            case "xls":
            case "xlsx":
            case "ods":
                icon = "fa fa-file-excel-o";
                break;
            case "wav":
            case "mp3":
            case "mp2":
            case "m4a":
            case "aac":
            case "ogg":
            case "oga":
            case "wma":
            case "mka":
            case "flac":
            case "ac3":
            case "tds":
                icon = "fa fa-music";
                break;
            case "m3u":
            case "m3u8":
            case "pls":
            case "cue":
                icon = "fa fa-headphones";
                break;
            case "avi":
            case "mpg":
            case "mpeg":
            case "mp4":
            case "m4v":
            case "flv":
            case "f4v":
            case "ogm":
            case "ogv":
            case "mov":
            case "mkv":
            case "3gp":
            case "asf":
            case "wmv":
                icon = "fa fa-file-video-o";
                break;
            case "eml":
            case "msg":
                icon = "fa fa-envelope-o";
                break;
            case "bak":
                icon = "fa fa-clipboard";
                break;
            case "doc":
            case "docx":
            case "odt":
                icon = "fa fa-file-word-o";
                break;
            case "ppt":
            case "pptx":
                icon = "fa fa-file-powerpoint-o";
                break;
            case "ttf":
            case "ttc":
            case "otf":
            case "woff":
            case "woff2":
            case "eot":
            case "fon":
                icon = "fa fa-font";
                break;
            case "pdf":
                icon = "fa fa-file-pdf-o";
                break;
            case "psd":
            case "ai":
            case "eps":
            case "fla":
            case "swf":
                icon = "fa fa-file-image-o";
                break;
            case "exe":
            case "msi":
                icon = "fa fa-file-o";
                break;
            case "bat":
                icon = "fa fa-terminal";
                break;
            default:
                icon = "fa fa-info-circle";
        }

        return icon;
    }
}
