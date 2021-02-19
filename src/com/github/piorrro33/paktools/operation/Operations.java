package com.github.piorrro33.paktools.operation;

import java.nio.file.Path;

public class Operations {
    public static boolean perform(OperationMode mode, Path pakPath, Path folderPath) {
        try {
            switch (mode) {
                case EXTRACT -> {
                    return Extraction.perform(pakPath, folderPath);
                }
                case REBUILD -> {
                    return Reconstruction.perform(pakPath, folderPath);
                }
                default -> {
                    System.err.println("Warning: no handler for OperationMode" + mode.toString());
                    return false;
                }
            }
        } catch (Exception e) {
            System.err.println("An uncaught exception has been thrown while performing the requested operation.");
            e.printStackTrace();
            return false;
        }
    }
}
