package com.github.piorrro33.paktools;

public class NotInferrableFolderPathException extends Exception {
    public NotInferrableFolderPathException() {
        super();
    }

    public NotInferrableFolderPathException(String message) {
        super(message);
    }

    public NotInferrableFolderPathException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotInferrableFolderPathException(Throwable cause) {
        super(cause);
    }

    public NotInferrableFolderPathException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
