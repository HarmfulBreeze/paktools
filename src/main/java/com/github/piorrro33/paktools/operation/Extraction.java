package com.github.piorrro33.paktools.operation;

import com.github.piorrro33.paktools.Utils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.stream.Stream;

import static com.github.piorrro33.paktools.Constants.*;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static java.nio.file.StandardOpenOption.*;

public class Extraction {
    private static final Charset CS_SHIFT_JIS = Charset.forName("Shift_JIS");

    private static void bufSkip(ByteBuffer buf, int offset) {
        buf.position(buf.position() + offset);
    }

    public static boolean perform(Path pakPath, Path destFolderPath) {
        try (Stream<Path> walk = Files.walk(destFolderPath, 1)) {
            if (walk.count() > 1) {
                String message = String.format(
                        "Warning! The destination folder (%s) is not empty. Some files may be overwritten.%n" +
                        "Do you want to proceed (yes or no)? ", destFolderPath
                );
                if (!Utils.promptYesOrNo(message)) {
                    // User did not answer yes
                    System.out.println("Aborting.");
                    return false;
                }
            }
        } catch (NoSuchFileException fnfe) {
            System.out.println("Destination folder does not exist. Creating it...");
            try {
                Files.createDirectories(destFolderPath);
            } catch (IOException e) {
                System.err.println("Could not create directory at given folder path! " + e.getLocalizedMessage());
                return false;
            }
        } catch (IOException e) {
            System.err.println("An I/O error has occurred while walking the folder path! " + e.getLocalizedMessage());
            return false;
        }

        try (ByteChannel inChan = Files.newByteChannel(pakPath)) {
            ByteBuffer inBuf = ByteBuffer.allocate(BUFSIZE).order(LITTLE_ENDIAN);
            long pakFileSize = Files.size(pakPath);

            while (true) {
                inChan.read(inBuf);
                inBuf.flip();

                String filename = CS_SHIFT_JIS.decode(inBuf.slice(0, FILENAME_BUFSIZE)).toString();
                filename = filename.substring(0, filename.indexOf('\0')); // Remove null byte and dummy data
                if (filename.isEmpty()) {
                    break; // We have reached the final entry, we can exit the loop
                }

                inBuf.position(FILENAME_BUFSIZE);

                int headerSize = inBuf.getInt();
                if (headerSize != HEADER_SIZE) {
                    String message = String.format(
                            "Odd header size (0x%x), would you like to continue extracting %s (yes or no)? ",
                            headerSize, pakPath.getFileName()
                    );
                    if (!Utils.promptYesOrNo(message)) {
                        System.out.printf("Stopped extraction of %s.%n", pakPath.getFileName());
                        return false;
                    }
                }

                int fileSize = inBuf.getInt();
                if (fileSize >= pakFileSize) {
                    System.err.printf("Invalid file size (0x%x), stopping extraction.%n", fileSize);
                    return false;
                }

                int nextHeaderOffset = inBuf.getInt();
                if (nextHeaderOffset < fileSize) {
                    String message = String.format(
                            "Odd next header offset (0x%s), would you like to continue extracting %s (yes or no)? ",
                            nextHeaderOffset, pakPath.getFileName()
                    );
                    if (!Utils.promptYesOrNo(message)) {
                        System.out.printf("Stopped extraction of %s.%n", pakPath.getFileName());
                        return false;
                    }
                }

                int unk = inBuf.getInt();
                if (unk != UNK_HEADER_CONST) {
                    System.out.printf("Warning: odd unknown int. Expected 0x43424140, got 0x%x%n", unk);
                }

                inBuf.compact();

                System.out.printf("Extracting file \"%s\"...%n", filename);

                Path outFilePath = destFolderPath.resolve(filename);
                try (ByteChannel outChan = Files.newByteChannel(outFilePath, WRITE, CREATE, TRUNCATE_EXISTING)) {
                    /*
                     * We have probably already have file data in the buffer, so we have to initialize it to the
                     * buffer's position.
                     */
                    int inBytesRead = inBuf.position();
                    int outBytesWritten = 0;
                    while (outBytesWritten < fileSize) {
                        inBytesRead += inChan.read(inBuf);
                        inBuf.flip();

                        if (inBytesRead > fileSize) {
                            // Last write to outChan
                            inBuf.limit(fileSize - outBytesWritten); // Limit buffer to write just enough
                            outBytesWritten += outChan.write(inBuf);
                            inBuf.limit(inBuf.capacity()); // Revert the limit to the capacity to be able to compact
                        } else {
                            // Any other write
                            outBytesWritten += outChan.write(inBuf);
                        }

                        inBuf.compact(); // We compact rather than clear to avoid discarding next file's data
                    }
                } catch (IOException e) {
                    System.err.println("An error has occurred while writing to the output file: " + e.getLocalizedMessage());
                    return false;
                }

                /*
                 * Here we move to the next header. It shouldn't be needed as usually the next header is right after
                 * the current file's data. But who knows! So let's move to it anyway just to be sure.
                 */
                inChan.read(inBuf);
                inBuf.flip();
                bufSkip(inBuf, nextHeaderOffset - fileSize - headerSize);
                inBuf.compact();
            }
        } catch (IOException e) {
            System.err.printf("An error has occurred while extracting \"%s\": %s%n", pakPath.getFileName(), e.getLocalizedMessage());
            return false;
        }

        return true;
    }
}
