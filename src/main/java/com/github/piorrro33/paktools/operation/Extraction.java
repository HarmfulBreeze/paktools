package com.github.piorrro33.paktools.operation;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;
import java.util.stream.Stream;

import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static java.nio.file.StandardOpenOption.*;

class Extraction {
    private static final Charset CS_SHIFT_JIS = Charset.forName("Shift_JIS");
    private static final int BUFFER_SIZE = 64;

    private static void bufSkip(ByteBuffer buf, int offset) {
        buf.position(buf.position() + offset);
    }

    public static boolean perform(Path pakPath, Path destFolderPath) {
        // Create destination folder if needed
        if (Files.notExists(destFolderPath)) {
            System.out.println("Destination folder does not exist. Creating it...");
            try {
                Files.createDirectories(destFolderPath);
            } catch (IOException e) {
                System.err.println("Could not create directory at given folder path! " + e.getLocalizedMessage());
                return false;
            }
        } else {
            try (Stream<Path> walk = Files.walk(destFolderPath, 1)) {
                if (walk.count() > 1) {
                    System.out.printf("Warning! The destination folder (%s) is not empty. Some files may be " +
                                      "overwritten.%n" +
                                      "Do you want to proceed (yes or no)? ", destFolderPath);
                    Scanner sc = new Scanner(System.in);
                    final String userAnswer = sc.nextLine();
                    if (!userAnswer.equalsIgnoreCase("yes") && !userAnswer.equalsIgnoreCase("y")) {
                        // User did not answer yes
                        System.out.println("Aborting.");
                        return false;
                    }
                }
            } catch (IOException e) {
                System.err.println("An I/O error has occurred while walking the folder path! " + e.getLocalizedMessage());
                return false;
            }
        }

        ByteBuffer inBuf = ByteBuffer.allocate(BUFFER_SIZE).order(LITTLE_ENDIAN);

        String filename;
        int headerSize, fileSize, nextHeaderOffset;
        try (ByteChannel inChan = Files.newByteChannel(pakPath)) {
            do {
                inChan.read(inBuf);
                inBuf.flip();

                filename = CS_SHIFT_JIS.decode(inBuf).toString();
                if (filename.charAt(0) == '\0') {
                    // We have reached the final entry, we can exit the loop
                    break;
                }

                filename = filename.substring(0, filename.indexOf('\0')); // Remove dummy data after null byte

                inBuf.clear();
                inChan.read(inBuf);
                inBuf.flip();

                headerSize = inBuf.getInt();
                fileSize = inBuf.getInt();
                nextHeaderOffset = inBuf.getInt();
                bufSkip(inBuf, 4); // unknown

                inBuf.compact();

                System.out.printf("Extracting file \"%s\"...%n", filename);

                Path outFilePath = destFolderPath.resolve(filename);
                try (ByteChannel outChan = Files.newByteChannel(outFilePath, WRITE, CREATE, TRUNCATE_EXISTING)) {
                    ByteBuffer outBuf = ByteBuffer.allocate(BUFFER_SIZE).order(LITTLE_ENDIAN);

                    int outBytesWritten = 0;
                    while (outBytesWritten < fileSize) {
                        inChan.read(inBuf);
                        inBuf.flip();

                        if (fileSize - outBytesWritten < 64) {
                            // We have to copy less than a full buffer to outBuf
                            outBuf.put(0, inBuf, 0, fileSize - outBytesWritten);
                            outBuf.position(fileSize - outBytesWritten);
                            inBuf.position(fileSize - outBytesWritten);
                        } else {
                            outBuf.put(inBuf);
                        }

                        outBuf.flip();
                        outBytesWritten += outChan.write(outBuf);

                        inBuf.compact(); // We compact rather than clear to avoid discarding next file's data
                        outBuf.clear();
                    }
                } catch (IOException e) {
                    System.err.println("An error has occurred while writing to the output file: " + e.getLocalizedMessage());
                    return false;
                }

                // Move to the next header
                inChan.read(inBuf);
                inBuf.flip();
                bufSkip(inBuf, nextHeaderOffset - fileSize - headerSize);
                inBuf.compact();
            } while (true);
        } catch (IOException e) {
            System.err.printf("An error has occurred while extracting \"%s\": %s%n", pakPath.getFileName(), e.getLocalizedMessage());
            return false;
        }

        return true;
    }
}
