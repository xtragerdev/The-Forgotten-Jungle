package com.icedfoxstudios.forgottenjungle;

import java.util.Locale;

final class RaptorText {
    static String normalizeSuffix(String suffix) {
        return suffix == null ? "" : suffix.trim().toLowerCase(Locale.ROOT);
    }

    static String formatCompactDecimal(double value) {
        double rounded = Math.round(value * 10.0) / 10.0;
        if (Math.abs(rounded - Math.round(rounded)) < 0.0001) {
            return String.valueOf((long) Math.round(rounded));
        }
        return String.format(Locale.ROOT, "%.1f", rounded);
    }

    static String formatRemaining(long remainingMs) {
        long totalSeconds = Math.max(0L, (remainingMs + 999L) / 1_000L);
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return String.format(Locale.ROOT, "%02d:%02d", minutes, seconds);
    }

    static String formatCooldownShort(long remainingMs) {
        long totalMinutes = Math.max(1L, (remainingMs + 59_999L) / 60_000L);
        long hours = totalMinutes / 60L;
        long minutes = totalMinutes % 60L;
        if (hours > 0L) {
            return String.format(Locale.ROOT, "%dh%02d", hours, minutes);
        }
        return String.format(Locale.ROOT, "%02dm", minutes);
    }

    static String formatCooldownLong(long remainingMs) {
        long totalMinutes = Math.max(1L, (remainingMs + 59_999L) / 60_000L);
        long hours = totalMinutes / 60L;
        long minutes = totalMinutes % 60L;
        if (hours > 0L) {
            return hours + "h " + minutes + "m";
        }
        return minutes + "m";
    }

    static double clamp(double value, double min, double max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    static int clampInt(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    private RaptorText() {
    }
}
