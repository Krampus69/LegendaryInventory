package com.krampus.legendaryinventory.client;

public final class ClientWeightState {

    private static final long FLASH_MILLIS = 3000L;

    private static int weight = -1;
    private static int capacity = -1;
    private static long flashUntil = 0L;
    private static long suppressUntil = 0L;

    private ClientWeightState() {}

    public static void set(int value, int maximum) {
        if (weight >= 0 && value != weight && System.currentTimeMillis() >= suppressUntil) {
            flashUntil = System.currentTimeMillis() + FLASH_MILLIS;
        }
        weight = value;
        capacity = maximum;
    }

    public static int capacity() {
        return capacity;
    }

    public static int get() {
        return weight;
    }

    public static boolean flashing() {
        return System.currentTimeMillis() < flashUntil;
    }

    public static void suppress(long millis) {
        suppressUntil = System.currentTimeMillis() + millis;
    }

    public static void reset() {
        weight = -1;
        capacity = -1;
        flashUntil = 0L;
        suppressUntil = 0L;
    }
}
