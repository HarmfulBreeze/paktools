package com.github.piorrro33.paktools.operation;

public enum OperationMode {
    EXTRACT("extract"), REBUILD("rebuild");

    private final String name;

    OperationMode(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
