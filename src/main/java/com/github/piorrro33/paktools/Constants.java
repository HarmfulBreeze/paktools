package com.github.piorrro33.paktools;

public class Constants {

    public static final String APPLICATION_NAME = "paktools";
    public static final String APPLICATION_VERSION = "v0.1";
    public static final String APPLICATION_VERSION_STRING = APPLICATION_NAME + " " + APPLICATION_VERSION;
    public static final String DEFAULT_PACKAGE_EXTENSION = ".pak";
    public static final int BUFSIZE = 8192;
    public static final int HEADER_SIZE = 0x50;
    public static final int FILENAME_BUFSIZE = 0x40;
    public static final int UNK_HEADER_CONST = 0x43424140;

    private Constants() {
        throw new IllegalStateException("Constants class");
    }
}
