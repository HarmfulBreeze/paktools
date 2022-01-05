package com.github.piorrro33.paktools;

import com.github.piorrro33.paktools.operation.Operations;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

import static com.github.piorrro33.paktools.Constants.*;
import static com.github.piorrro33.paktools.operation.Operations.OperationMode.EXTRACT;
import static com.github.piorrro33.paktools.operation.Operations.OperationMode.REBUILD;
import static picocli.CommandLine.*;

@Command(name = APPLICATION_NAME, version = APPLICATION_VERSION_STRING, mixinStandardHelpOptions = true)
public class Main implements Callable<Integer> {
    @Parameters(paramLabel = "INPUT",
            description = "Paths to package files or to folders.", arity = "1..*")
    private static Path[] inputPath;

    @Option(names = {"-o", "--output"},
            description = "Path to an output file or folder. " +
                          "Only one input path can be given when this option is set.")
    private static Path outputPath;

    public static void main(String[] args) {
        CommandLine commandLine = new CommandLine(new Main());
        int exitCode = commandLine.execute(args);
        if (exitCode == ExitCode.USAGE) {
            commandLine.usage(commandLine.getErr());
        }
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws IllegalArgumentException {
        System.out.println(APPLICATION_VERSION_STRING);

        if (inputPath.length > 1 && outputPath != null) {
            System.err.println("More than one input path was given with the output path");
            return ExitCode.USAGE;
        }

        boolean isSuccessful = true;
        for (Path input : inputPath) {
            if (Files.isRegularFile(input)) {
                Path folderPath;
                if (outputPath == null) {
                    // User did not give an output folder path, let's make our own
                    String inputFilenameStr = input.getFileName().toString();

                    String folderName;
                    if (!inputFilenameStr.contains(".")) {
                        folderName = inputFilenameStr + "_dir";
                    } else {
                        folderName = inputFilenameStr.substring(0, inputFilenameStr.lastIndexOf('.'));
                    }
                    folderPath = input.resolveSibling(folderName);
                } else {
                    folderPath = outputPath;
                }

                if (Files.isRegularFile(folderPath)) {
                    System.err.printf(
                            "Output path (%s) cannot be a file if input path (%s) is a file%n" +
                            "Specify a different output path!%n", outputPath, input
                    );
                    isSuccessful = false;
                    continue;
                }

                boolean ret = Operations.perform(EXTRACT, input, folderPath);
                if (!ret) {
                    // Avoid setting it to true if an operation on a previous input failed
                    isSuccessful = false;
                }
            } else if (Files.isDirectory(input)) {
                Path pakPath;
                if (outputPath == null) {
                    // User did not give an output file path, let's make our own
                    String filename = input + DEFAULT_PACKAGE_EXTENSION;
                    pakPath = input.resolveSibling(filename);
                } else {
                    pakPath = outputPath;
                }

                if (Files.isDirectory(pakPath)) {
                    System.err.printf(
                            "Output path (%s) cannot be a folder if input path (%s) is a folder%n" +
                            "Specify a different output path!%n", outputPath, input
                    );
                    isSuccessful = false;
                    continue;
                }

                boolean ret = Operations.perform(REBUILD, input, pakPath);
                if (!ret) {
                    isSuccessful = false;
                }
            } else {
                System.err.printf("Invalid input path \"%s\"%n", input);
                isSuccessful = false;
            }
        }

        if (isSuccessful) {
            System.out.println("Operation completed.");
        } else {
            System.err.println("Operation failed.");
        }

        return isSuccessful ? ExitCode.OK : ExitCode.SOFTWARE;
    }
}
