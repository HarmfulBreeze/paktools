package com.github.piorrro33.paktools.operation;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;
import java.util.stream.Stream;

import static java.nio.ByteOrder.LITTLE_ENDIAN;

class Reconstruction {
    private static final Charset CS_SHIFT_JIS = Charset.forName("Shift_JIS");

    public static boolean perform(Path pakPath, Path sourceFolderPath) {
        // Check if source folder exists and if PAK file does not exist
        if (Files.notExists(sourceFolderPath)) {
            System.err.println("Source folder could not be found!");
            return false;
        }
        if (Files.exists(pakPath)) {
            System.out.println("Warning! The destination package file already exists.\n" +
                    "Do you want to proceed (yes or no)?");
            Scanner sc = new Scanner(System.in);
            final String userAnswer = sc.nextLine();
            if (!userAnswer.equalsIgnoreCase("yes") && !userAnswer.equalsIgnoreCase("y")) {
                // User did not answer yes
                System.out.println("Aborting.");
                return false;
            }
        }

        // Create parent dirs for PAK
        if (pakPath.getParent() != null && Files.notExists(pakPath.getParent())) {
            System.out.println("Creating parent folders for package file...");
            try {
                Files.createDirectories(pakPath.getParent());
            } catch (IOException e) {
                System.err.println("Could not create parent folders for package file! " + e.getLocalizedMessage());
                return false;
            }
        }

        // Make a list holding paths to all the files in the source folder.
        System.out.println("Browsing source folder...");

        // Open stream to pak file
        OutputStream pakStream;
        try {
            pakStream = new BufferedOutputStream(Files.newOutputStream(pakPath));
        } catch (IOException e) {
            System.err.println("Could not open package file! " + e.getLocalizedMessage());
            return false;
        }

        // Get all files at the root of the source folder
        try (Stream<Path> walk = Files.walk(sourceFolderPath, 1)) {
            System.out.println("Writing package...");

            // Create ByteBuffers
            ByteBuffer bb_name = ByteBuffer.allocate(64).order(LITTLE_ENDIAN);
            ByteBuffer bb_headerSize = ByteBuffer.allocate(4).order(LITTLE_ENDIAN);
            ByteBuffer bb_fileSize = ByteBuffer.allocate(4).order(LITTLE_ENDIAN);
            ByteBuffer bb_nextHeaderOffset = ByteBuffer.allocate(4).order(LITTLE_ENDIAN);
            ByteBuffer bb_unk = ByteBuffer.allocate(4).order(LITTLE_ENDIAN);

            // Write all file entries except final dummy
            walk.filter(Files::isRegularFile).forEach(inFilePath -> {
                try {
                    // Populate header ByteBuffers
                    int fileSize = (int) Files.size(inFilePath);
                    int alignmentSize = (0x10 - fileSize % 0x10) % 0x10;
                    bb_name.put(CS_SHIFT_JIS.encode(inFilePath.getFileName().toString() + "\0"));
                    bb_headerSize.putInt(0x50);
                    bb_fileSize.putInt(fileSize);
                    bb_nextHeaderOffset.putInt(0x50 + fileSize + alignmentSize);
                    bb_unk.putInt(0x43424140);

                    // Read input file data
                    ByteBuffer bb_fileData = ByteBuffer.allocate(fileSize).order(LITTLE_ENDIAN);
                    byte[] alignment = new byte[alignmentSize];
                    try (InputStream inFileIs = new BufferedInputStream(Files.newInputStream(inFilePath))) {
                        inFileIs.read(bb_fileData.array());
                    } catch (IOException e) {
                        String s = "An error has occurred while reading from file " +
                                "\"" + inFilePath + "\": " +
                                e.getLocalizedMessage();
                        System.err.println(s);
                    }

                    // Write everything to package
                    pakStream.write(bb_name.array());
                    pakStream.write(bb_headerSize.array());
                    pakStream.write(bb_fileSize.array());
                    pakStream.write(bb_nextHeaderOffset.array());
                    pakStream.write(bb_unk.array());
                    pakStream.write(bb_fileData.array());
                    pakStream.write(alignment);

                    // Rewind ByteBuffers
                    bb_name.rewind();
                    bb_headerSize.rewind();
                    bb_fileSize.rewind();
                    bb_nextHeaderOffset.rewind();
                    bb_unk.rewind();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            // Write final dummy: set name to \0, set fileSize and nextHeaderOffset to -1, reuse the rest
            bb_name.put((byte) 0x00);
            bb_fileSize.putInt(-1);
            bb_nextHeaderOffset.putInt(-1);
            pakStream.write(bb_name.array());
            pakStream.write(bb_headerSize.array());
            pakStream.write(bb_fileSize.array());
            pakStream.write(bb_nextHeaderOffset.array());
            pakStream.write(bb_unk.array());
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                pakStream.close();
            } catch (IOException e) {
                System.err.println("Could not close package file stream! " + e.getLocalizedMessage());
            }
        }
        return true;
    }
}
