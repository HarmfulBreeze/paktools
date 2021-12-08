package com.github.piorrro33.paktools.operation;

import java.nio.file.Path;

public class Operations {
    public static boolean perform(OperationMode mode, Path input, Path output) {
        try {
            switch (mode) {
                case EXTRACT -> {
                    return Extraction.perform(input, output);
                }
                case REBUILD -> {
                    return Reconstruction.perform(output, input);
                }
                default -> {
                    System.err.println("Warning: no handler for OperationMode" + mode);
                    return false;
                }
            }
        } catch (Exception e) {
            System.err.println("An uncaught exception has been thrown while performing the requested operation.");
            e.printStackTrace();
            return false;
        }
    }

    public enum OperationMode {
        EXTRACT, REBUILD
    }
}
