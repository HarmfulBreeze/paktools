package com.github.piorrro33.paktools;

import com.github.piorrro33.paktools.operation.OperationMode;
import com.github.piorrro33.paktools.operation.Operations;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

public class Main {
    public static final String APPLICATION_VERSION = "0.1";

    public static void usage() {
        // extract/rebuild datfile hd6file sourceDir/destDir
        System.out.println("Usage:");
        System.out.println("./paktools <mode> <pakfile> [folder]");
        System.out.println("<mode>: extract, rebuild");
        System.out.println("<pakfile>: path to your package file. Also supports other packages from Level-5 games.");
        System.out.println("[folder]: source/destination folder. Optional.");
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            usage();
            System.exit(1);
        }

        boolean isSuccessful;
        OperationMode mode;
        Path pakPath, folderPath;
        try {
            // Get mode and check path validity
            if (args.length == 3) {
                mode = OperationMode.valueOf(args[0].toUpperCase(Locale.ROOT));
                pakPath = Paths.get(args[1]);
                folderPath = Paths.get(args[2]);
            } else {
                mode = OperationMode.valueOf(args[0].toUpperCase(Locale.ROOT));
                pakPath = Paths.get(args[1]);
                String pakFileNameStr = pakPath.getFileName().toString();
                if (pakFileNameStr.contains(".")) {
                    String pakFileNameStrNoExt = pakFileNameStr.substring(0, pakFileNameStr.lastIndexOf('.'));
                    folderPath = pakPath.toAbsolutePath().getParent().resolve(pakFileNameStrNoExt);
                } else {
                    throw new NotInferrableFolderPathException();
                }
            }
            isSuccessful = Operations.perform(mode, pakPath, folderPath);
        } catch (InvalidPathException e) {
            System.err.println("Invalid path: " + e.getInput());
            isSuccessful = false;
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid operation mode: " + args[0]);
            usage();
            isSuccessful = false;
        } catch (NotInferrableFolderPathException e) {
            System.err.println("Folder path could not be inferred from the given package file.");
            System.err.println("Please specify a folder path when calling the program.");
            isSuccessful = false;
        }
        if (isSuccessful) {
            System.out.println("Operation completed.");
        } else {
            System.err.println("Operation failed.");
        }
    }
}
