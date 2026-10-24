package com.qingping.app;

import java.util.regex.Pattern;

/** Platform-independent rules: no substring matching, arbitrary regex, or gesture taps. */
public final class SkipPolicy {
    public static final long WINDOW_MS = 12_000;
    public static final long SCAN_INTERVAL_MS = 350;
    private static final Pattern SKIP = Pattern.compile(
            "(?i)^(?:跳过|skip)(?:\\s*[(（]?\\s*\\d{1,2}\\s*(?:s|秒)?\\s*[)）]?)?$|^\\d{1,2}\\s*(?:s|秒)?\\s*(?:跳过|skip)$");

    private SkipPolicy() {}

    public static boolean matches(String text, String description, String viewId,
                                  String exactText, String exactId, boolean upperRight) {
        // Custom constraints are conjunctive. An ID rule never falls back to generic text.
        boolean customText = exactText != null && !exactText.trim().isEmpty();
        boolean customId = exactId != null && !exactId.trim().isEmpty();
        if (customText || customId) {
            return (!customId || exactId.equals(viewId))
                    && (!customText || exactText.equals(clean(text)) || exactText.equals(clean(description)));
        }
        return upperRight && (SKIP.matcher(clean(text)).matches() || SKIP.matcher(clean(description)).matches());
    }

    public static String clean(String value) {
        return value == null ? "" : value.replace('\u00a0', ' ').trim();
    }

    public static final class Session {
        private String packageName = "";
        private long startedAt;
        private boolean attempted;

        public void enter(String next, long now) {
            if (!next.equals(packageName)) {
                packageName = next;
                startedAt = now;
                attempted = false;
            }
        }

        public boolean canScan(String current, long now) {
            return packageName.equals(current) && isOpen(now);
        }

        public boolean isOpen(long now) {
            return !packageName.isEmpty() && !attempted && now >= startedAt && now - startedAt < WINDOW_MS;
        }

        public void attempted() { attempted = true; }
        public void reset() { packageName = ""; attempted = true; }
    }
}
