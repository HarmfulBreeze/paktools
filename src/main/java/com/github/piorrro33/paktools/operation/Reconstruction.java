package com.github.piorrro33.paktools.operation;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;
import java.util.stream.Stream;

import static com.github.piorrro33.paktools.Constants.*;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static java.nio.file.StandardOpenOption.*;

public class Reconstruction {
    private static final Charset CS_SHIFT_JIS = Charset.forName("Shift_JIS");

    public static boolean perform(Path pakPath, Path sourceFolderPath) {
        // Create parent dirs for PAK if needed
        if (pakPath.getParent() != null && Files.notExists(pakPath.getParent())) {
            System.out.println("Creating parent folders for package file...");
            try {
                Files.createDirectories(pakPath.getParent());
            } catch (IOException e) {
                System.err.println("Could not create parent folders for package file! " + e.getLocalizedMessage());
                return false;
            }
        } else if (Files.exists(pakPath)) {
            System.out.printf("Warning! The destination package file (%s) already exists.%n" +
                              "Do you want to overwrite it (yes or no)? ", pakPath.getFileName());
            final String userAnswer = new Scanner(System.in).nextLine();
            if (!userAnswer.equalsIgnoreCase("yes") && !userAnswer.equalsIgnoreCase("y")) {
                // User did not answer yes
                System.out.println("Aborting.");
                return false;
            }
        }

        ByteBuffer pakBuf = ByteBuffer.allocate(BUFSIZE).order(LITTLE_ENDIAN);

        try (ByteChannel pakChan = Files.newByteChannel(pakPath, WRITE, CREATE, TRUNCATE_EXISTING)) {
            // Get all files at the root of the source folder
            try (Stream<Path> walk = Files.walk(sourceFolderPath, 1)) {
                // Write all file entries except final dummy
                walk.filter(Files::isRegularFile).forEach(inFilePath -> {
                    try (ByteChannel inChan = Files.newByteChannel(inFilePath)) {
                        String filename = inFilePath.getFileName().toString();
                        int fileSize = (int) Files.size(inFilePath);
                        int alignmentSize = (0x10 - fileSize & 0xF) & 0xF;
                        byte[] alignment = new byte[alignmentSize];

                        System.out.println("Adding file \"" + filename + "\"...");

                        // Populate pakBuf with the header
                        pakBuf.put(CS_SHIFT_JIS.encode(filename + "\0")); // filename
                        pakBuf.position(FILENAME_BUFSIZE);
                        pakBuf.putInt(HEADER_SIZE); // headerSize
                        pakBuf.putInt(fileSize); // fileSize
                        pakBuf.putInt(HEADER_SIZE + fileSize + alignmentSize); // nextHeaderOffset
                        pakBuf.putInt(UNK_HEADER_CONST); // unk
                        pakBuf.flip();

                        // Write header into pakChan
                        pakChan.write(pakBuf);
                        pakBuf.clear();

                        // Copy file data into pakChan
                        while (inChan.read(pakBuf) > 0) {
                            pakBuf.flip();
                            pakChan.write(pakBuf);
                            pakBuf.clear();
                        }

                        // Write alignment into pakChan
                        pakBuf.put(alignment);
                        pakBuf.flip();
                        pakChan.write(pakBuf);
                        pakBuf.clear();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

                // Write final dummy: set name to \0, set fileSize and nextHeaderOffset to -1, reuse the rest
                System.out.println("Adding final dummy file...");
                pakBuf.put((byte) 0x00); // filename
                pakBuf.position(FILENAME_BUFSIZE);
                pakBuf.putInt(HEADER_SIZE); // headerSize
                pakBuf.putInt(-1); // fileSize
                pakBuf.putInt(-1); // nextHeaderOffset
                pakBuf.putInt(UNK_HEADER_CONST); // unk
                pakBuf.flip();
                pakChan.write(pakBuf);
                pakBuf.clear();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        } catch (IOException e) {
            System.err.printf("An error has occurred while rebuilding \"%s\": %s%n", pakPath.getFileName(),
                    e.getLocalizedMessage());
            return false;
        }

        return true;
    }
}
