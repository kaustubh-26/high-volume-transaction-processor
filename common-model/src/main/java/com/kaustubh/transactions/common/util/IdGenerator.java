package com.kaustubh.transactions.common.util;

import java.util.UUID;

public final class IdGenerator {

    private IdGenerator() {
    }

    public static String newTransactionId() {
        return UUID.randomUUID().toString();
    }

    public static UUID newEventId() {
        return UUID.randomUUID();
    }
}
