package com.github.piorrro33.paktools.operation;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;
import java.util.stream.Stream;

import static java.nio.ByteOrder.LITTLE_ENDIAN;

class Extraction {
    private static final Charset CS_SHIFT_JIS = Charset.forName("Shift_JIS");
    private static final CharsetDecoder SJIS_DECODER = CS_SHIFT_JIS.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT);

    public static boolean perform(Path pakPath, Path destFolderPath) {
        // Check if pak exists, create destination folder if needed
        if (Files.notExists(pakPath)) {
            System.err.println("Package file could not be found!");
            return false;
        }
        if (Files.notExists(destFolderPath)) {
            System.out.println("Destination folder does not exist. Creating it...");
            try {
                Files.createDirectories(destFolderPath);
            } catch (IOException e) {
                System.err.println("Could not create directory at given folder path! " + e.getLocalizedMessage());
                return false;
            }
        }
        try (Stream<Path> walk = Files.walk(destFolderPath, 1)) {
            if (walk.count() > 1) {
                System.out.println("Warning! The destination folder is not empty. Some files may be overwritten.\n" +
                        "Do you want to proceed (yes or no)?");
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
        }

        // Open stream to pak file
        InputStream pakStream;
        try {
            pakStream = new BufferedInputStream(Files.newInputStream(pakPath));
        } catch (IOException e) {
            System.err.println("Could not open package file! " + e.getLocalizedMessage());
            return false;
        }

        int pakStreamOffset = 0;
        try {
            // Create ByteBuffers
            ByteBuffer bb_name = ByteBuffer.allocate(64).order(LITTLE_ENDIAN);
            ByteBuffer bb_headerSize = ByteBuffer.allocate(4).order(LITTLE_ENDIAN);
            ByteBuffer bb_fileSize = ByteBuffer.allocate(4).order(LITTLE_ENDIAN);
            ByteBuffer bb_nextHeaderOffset = ByteBuffer.allocate(4).order(LITTLE_ENDIAN);
            ByteBuffer bb_unk = ByteBuffer.allocate(4).order(LITTLE_ENDIAN);

            // Define header variables
            String name;
            int headerSize, fileSize, nextHeaderOffset;

            do {
                // Read every field of the header
                pakStreamOffset += pakStream.read(bb_name.array());
                name = SJIS_DECODER.decode(bb_name).toString();
                pakStreamOffset += pakStream.read(bb_headerSize.array());
                headerSize = bb_headerSize.getInt();
                pakStreamOffset += pakStream.read(bb_fileSize.array());
                fileSize = bb_fileSize.getInt();
                pakStreamOffset += pakStream.read(bb_nextHeaderOffset.array());
                nextHeaderOffset = bb_nextHeaderOffset.getInt();
                pakStreamOffset += pakStream.read(bb_unk.array());

                if (fileSize != -1) { // Avoids writing the final dummy
                    // Remove undefined data from the buffer
                    name = name.substring(0, name.indexOf('\0'));

                    // Read file data
                    ByteBuffer bb_fileData = ByteBuffer.allocate(fileSize);
                    pakStreamOffset += pakStream.read(bb_fileData.array());

                    // Write file data into the output
                    Path outFilePath = destFolderPath.resolve(name);
                    try (OutputStream outFileOs = new BufferedOutputStream(Files.newOutputStream(outFilePath))) {
                        outFileOs.write(bb_fileData.array());
                    } catch (IOException e) {
                        System.err.println("An error has occurred while writing an output file: " + e.getLocalizedMessage());
                        return false;
                    }

                    // Skip to the next header
                    pakStreamOffset += pakStream.skip(nextHeaderOffset - fileSize - headerSize);
                }

                // Rewind ByteBuffers
                bb_name.rewind();
                bb_headerSize.rewind();
                bb_fileSize.rewind();
                bb_nextHeaderOffset.rewind();
                bb_unk.rewind();
            } while (fileSize != -1);
        } catch (CharacterCodingException e) {
            System.err.println("Could not decode a character in filename at offset " + pakStreamOffset);
            return false;
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
