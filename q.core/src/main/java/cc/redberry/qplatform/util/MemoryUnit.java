package cc.redberry.qplatform.util;

/**
 *
 */
public enum MemoryUnit {
    BYTES(MemoryUnit.BYTE_SCALE),
    KILOBYTES(MemoryUnit.KILO_SCALE),
    MEGABYTES(MemoryUnit.MEGA_SCALE),
    GIGABYTES(MemoryUnit.GIGA_SCALE),
    TERABYTES(MemoryUnit.TERA_SCALE);

    private final long scale;

    MemoryUnit(long scale) {
        this.scale = scale;
    }

    public long toBytes(long b) {
        return scale * b;
    }

    public long toKilobytes(long b) {
        return scale * b / MemoryUnit.KILO_SCALE;
    }

    public long toMegabytes(long b) {
        return scale * b / MemoryUnit.MEGA_SCALE;
    }

    public long toGigabytes(long b) {
        return scale * b / MemoryUnit.GIGA_SCALE;
    }

    public long toTerabytes(long b) {
        return scale * b / MemoryUnit.TERA_SCALE;
    }

    private static final long BYTE_SCALE = 1L;
    private static final long KILO_SCALE = 1024L * BYTE_SCALE;
    private static final long MEGA_SCALE = 1024L * KILO_SCALE;
    private static final long GIGA_SCALE = 1024L * MEGA_SCALE;
    private static final long TERA_SCALE = 1024L * GIGA_SCALE;
}
