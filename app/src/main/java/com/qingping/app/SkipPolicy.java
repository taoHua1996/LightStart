package com.qingping.app;

import java.util.regex.Pattern;

/** Platform-independent rules: no substring matching, arbitrary regex, or gesture taps. */
public final class SkipPolicy {
    public static final long WINDOW_MS = 12_000;
    public static final long SCAN_INTERVAL_MS = 350;
    private static final Pattern SKIP = Pattern.compile(
            "(?i)^(?:跳过(?:广告)?|skip(?: ad)?)(?:\\s*[|｜]?\\s*[(（]?\\s*\\d{1,2}\\s*(?:s|秒)?\\s*[)）]?)?$|^\\d{1,2}\\s*(?:s|秒)?\\s*[|｜]?\\s*(?:跳过(?:广告)?|skip(?: ad)?)$");

    private SkipPolicy() {}

    public static boolean skipLabel(String text, String description) {
        return SKIP.matcher(clean(text)).matches() || SKIP.matcher(clean(description)).matches();
    }

    public static boolean skipRegion(float x, float y) {
        return x >= 0 && x <= 1 && y >= 0 && y <= 1
                && ((x >= .5f && y <= .4f) || (y >= .75f && (x <= .3f || x >= .7f)));
    }

    public static boolean resourceId(String pkg, String id) {
        return pkg != null && id != null && id.startsWith(pkg + ":id/")
                && id.substring((pkg + ":id/").length()).matches("[A-Za-z0-9_]+");
    }

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
        private String firstAcceptedId = "";
        private long nextAttemptAt;

        public void enter(String next, long now) {
            if (!next.equals(packageName)) {
                packageName = next;
                startedAt = now;
                attempted = false;
                firstAcceptedId = "";
                nextAttemptAt = now;
            }
        }

        public boolean canScan(String current, long now) {
            return packageName.equals(current) && isOpen(now);
        }

        // Observe the actual foreground root at scan time, after any window transition.
        // A null root is temporary and must not invent or restart a foreground session.
        public boolean observe(String current, long now) {
            if (current == null || current.isEmpty()) return false;
            enter(current, now);
            return canScan(current, now);
        }

        public boolean isOpen(long now) {
            return !packageName.isEmpty() && !attempted && now >= startedAt && now - startedAt < WINDOW_MS;
        }

        public void attempted() { attempted = true; }
        // A second startup ad is allowed only for a different verified skip-button ID.
        // Missing IDs and rejected clicks retain the original single-attempt behavior.
        public boolean canAttempt(String verifiedId, long now) {
            return isOpen(now) && now >= nextAttemptAt
                    && (firstAcceptedId.isEmpty() || (verifiedId != null && !verifiedId.isEmpty()
                    && !firstAcceptedId.equals(verifiedId)));
        }
        public void attempted(String verifiedId, boolean accepted, long now) {
            if (!canAttempt(verifiedId, now)) return;
            if (!firstAcceptedId.isEmpty() || !accepted || verifiedId == null || verifiedId.isEmpty()) {
                attempted = true;
            } else {
                firstAcceptedId = verifiedId;
                nextAttemptAt = now + 1000;
            }
        }
        public void reset() { packageName = ""; attempted = true; }
    }
}
