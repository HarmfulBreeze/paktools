package com.github.piorrro33.paktools;

import com.github.piorrro33.paktools.operation.OperationMode;
import com.github.piorrro33.paktools.operation.Operations;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

import static picocli.CommandLine.*;

@Command(name = Main.APPLICATION_NAME, version = Main.APPLICATION_VERSION_STRING, mixinStandardHelpOptions = true)
public class Main implements Callable<Integer> {
    public static final String APPLICATION_NAME = "paktools";
    public static final String APPLICATION_VERSION = "v0.1";
    public static final String APPLICATION_VERSION_STRING = APPLICATION_NAME + " " + APPLICATION_VERSION;

    public static final String DEFAULT_PACKAGE_EXTENSION = ".pak";

    @Parameters(paramLabel = "INPUT",
            description = "Paths to package files or to folders.", arity = "1..*")
    private static Path[] inputPath;

    @Option(names = {"-o", "--output"},
            description = "Path to an output file or folder. " +
                          "Only one input path can be given when this option is set.")
    private static Path outputPath;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main())
                .setExecutionExceptionHandler(new ExecutionExceptionHandler())
                .execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws IllegalArgumentException {
        System.out.println(APPLICATION_VERSION_STRING);

        if (inputPath.length > 1 && outputPath != null) {
            throw new IllegalArgumentException("More than one input path was given with the output path");
        }

        boolean isSuccessful = true;
        for (Path input : inputPath) {
            if (Files.isRegularFile(input)) {
                Path folderPath;
                if (outputPath == null) {
                    String inputFilenameStr = input.getFileName().toString();

                    String folderName;
                    if (!inputFilenameStr.contains(".")) {
                        folderName = inputFilenameStr + "_dir"; // TODO: handle this better
                    } else {
                        folderName = inputFilenameStr.substring(0, inputFilenameStr.lastIndexOf('.'));
                    }
                    folderPath = input.resolveSibling(folderName);
                } else {
                    folderPath = outputPath;
                }

                if (Files.isRegularFile(folderPath)) {
                    throw new IllegalArgumentException(
                            "Output path (%s) cannot be a file if input path (%s) is a file"
                                    .formatted(outputPath, input)
                    );
                }

                isSuccessful = Operations.perform(OperationMode.EXTRACT, input, folderPath);
            } else if (Files.isDirectory(input)) {
                Path pakPath;
                if (outputPath == null) {
                    String filename = input + DEFAULT_PACKAGE_EXTENSION;
                    pakPath = input.resolveSibling(filename);
                } else {
                    pakPath = outputPath;
                }

                if (Files.isDirectory(pakPath)) {
                    throw new IllegalArgumentException(
                            "Output path (%s) cannot be a folder if input path (%s) is a folder"
                                    .formatted(outputPath, input)
                    );
                }

                isSuccessful = Operations.perform(OperationMode.REBUILD, pakPath, input);
            } else {
                // TODO: handle this better
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

class ExecutionExceptionHandler implements CommandLine.IExecutionExceptionHandler {
    @Override
    public int handleExecutionException(Exception ex, CommandLine commandLine, ParseResult parseResult) {
        if (ex instanceof IllegalArgumentException) {
            commandLine.getErr().println(ex.getMessage());
            commandLine.usage(commandLine.getErr());
            return commandLine.getCommandSpec().exitCodeOnInvalidInput();
        }
        return commandLine.getCommandSpec().exitCodeOnExecutionException();
    }
}
