package com.qingping.app;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.SharedPreferences;
import android.os.Bundle;
import java.util.Map;

/** Non-destructive preference checks. Run on the explicitly selected test emulator. */
public final class FeatureInstrumentation extends Instrumentation {
    private int checks;
    @Override public void onCreate(Bundle args) { super.onCreate(args); start(); }
    @Override public void onStart() {
        Bundle result = new Bundle();
        SharedPreferences prefs = getTargetContext().getSharedPreferences(Store.PREFS, 0);
        Map<String, ?> saved = prefs.getAll();
        int code = Activity.RESULT_OK;
        try (Store store = new Store(getTargetContext())) {
            String pkg = "com.qingping.app.test";
            store.forgetId(pkg);
            store.autoIds(true);
            store.learnId(pkg, pkg + ":id/fixture_skip");
            check(store.learnedId(pkg).equals(pkg + ":id/fixture_skip"), "learn ID");
            store.learnId(pkg, "foreign.app:id/skip");
            check(store.learnedId(pkg).equals(pkg + ":id/fixture_skip"), "reject foreign ID");
            store.autoIds(false);
            store.learnId(pkg, pkg + ":id/other_skip");
            check(store.learnedId(pkg).equals(pkg + ":id/fixture_skip"), "learning switch stops writes");
            store.rule(pkg, "custom", pkg + ":id/manual");
            store.autoIds(true);
            store.learnId(pkg, pkg + ":id/other_skip");
            check(store.viewId(pkg).equals(pkg + ":id/manual") && store.text(pkg).equals("custom"), "learning preserves manual rules");
            store.forgetId(pkg);
            check(store.learnedId(pkg).isEmpty(), "forget learned ID");
            store.protection(false); store.autoStart(false);
            check(!ProtectionService.wanted(getTargetContext(), store), "manual off wins");
            store.protection(true); store.autoStart(true); store.active(false);
            check(!ProtectionService.wanted(getTargetContext(), store), "pause prevents protection restart");
            try (Store reopened = new Store(getTargetContext())) {
                check(reopened.protection() && reopened.autoStart(), "settings survive store recreation");
            }
            result.putString("stream", "PASS: " + checks + " feature assertions\n");
        } catch (Throwable error) {
            code = Activity.RESULT_CANCELED;
            result.putString("stream", android.util.Log.getStackTraceString(error));
        } finally {
            SharedPreferences.Editor restore = prefs.edit().clear();
            for (Map.Entry<String, ?> e : saved.entrySet()) {
                Object value = e.getValue(); String key = e.getKey();
                if (value instanceof Boolean) restore.putBoolean(key, (Boolean)value);
                else if (value instanceof String) restore.putString(key, (String)value);
                else if (value instanceof Long) restore.putLong(key, (Long)value);
                else if (value instanceof Integer) restore.putInt(key, (Integer)value);
                else if (value instanceof Float) restore.putFloat(key, (Float)value);
                else if (value instanceof java.util.Set) {
                    java.util.Set<String> values = new java.util.HashSet<>();
                    for (Object item : (java.util.Set<?>)value) values.add((String)item);
                    restore.putStringSet(key, values);
                }
            }
            restore.commit();
        }
        finish(code, result);
    }
    private void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
    }
}
