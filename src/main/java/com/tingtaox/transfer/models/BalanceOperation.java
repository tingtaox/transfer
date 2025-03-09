package com.tingtaox.transfer.models;

public enum BalanceOperation {
    ADD(0),
    SUBTRACT(1);

    private final int value;

    BalanceOperation(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
