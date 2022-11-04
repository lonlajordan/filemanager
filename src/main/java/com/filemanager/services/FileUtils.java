package com.filemanager.services;

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

    public static String readableFileSize(long size) {
        if(size <= 0) return "0";
        final String[] units = new String[] { "o", "Ko", "Mo", "Go", "To" };
        int digitGroups = (int) (Math.log10(size)/Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size/Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }
}
