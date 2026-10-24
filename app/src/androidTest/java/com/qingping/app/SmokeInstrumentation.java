package com.qingping.app;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;

/** Runs only on a disposable test emulator; exercises real SQLite, services, and activities. */
public final class SmokeInstrumentation extends Instrumentation {
    private int assertions;
    private boolean databaseOnly;
    @Override public void onCreate(Bundle arguments) {
        super.onCreate(arguments);
        databaseOnly = arguments == null || !"true".equals(arguments.getString("includeService"));
        start();
    }

    @Override public void onStart() {
        Bundle output = new Bundle();
        try {
            Context context = getTargetContext();
            try (Store store = new Store(context)) {
                store.clear();
                store.rule("sample.test", "跳过广告", "sample.test:id/skip");
                check(!store.enabled("sample.never_enabled"), "apps default disabled");
                for (int i = 0; i < 505; i++) store.record("sample.test", true, false);
                store.record("sample.other", true, false);
                store.record("sample.test", false, false);
                store.record(context.getPackageName(), true, true);
                check(store.total() == 506, "aggregate survives log retention");
                check(store.today() == 506, "daily survives log retention");
                check(store.counts().size() == 2, "per-app aggregation");
                check(store.counts().get(0).count() == 505, "counts sorted descending");
                check(store.events().size() == 100, "display capped at 100");
                check(store.events().get(0).demo(), "demo distinguished");
                check(store.events().get(1).outcome().equals("rejected"), "rejected click recorded separately");
                try (android.database.Cursor c = store.getReadableDatabase().rawQuery("SELECT COUNT(*) FROM events", null)) {
                    check(c.moveToFirst() && c.getInt(0) == 500, "database retains at most 500 details");
                }
                store.clear();
                check(store.total() == 0 && store.today() == 0 && store.events().isEmpty(), "clear resets all statistics");
                check(store.text("sample.test").equals("跳过广告"), "clear preserves rules");
                store.rule("sample.test", "", "");
                store.active(true);
                store.consent();
            }

            // Instrumentation force-stops the target, disconnecting enabled accessibility services.
            // Default to database checks; service integration is exercised externally through UI.
            if (databaseOnly) {
                output.putString("stream", "PASS: " + assertions + " database assertions\n");
                finish(Activity.RESULT_OK, output);
                return;
            }
            Activity main = startActivitySync(new Intent(context, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            waitForIdleSync();
            check(main != null && !main.isFinishing(), "overview launches");
            long deadline = SystemClock.elapsedRealtime() + 10_000;
            while (!SkipAccessibilityService.connected && SystemClock.elapsedRealtime() < deadline) SystemClock.sleep(100);
            check(SkipAccessibilityService.connected, "accessibility connected on test emulator");
            Activity demo = startActivitySync(new Intent(context, DemoActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            waitForIdleSync();
            deadline = SystemClock.elapsedRealtime() + 5_000;
            boolean clicked = false;
            try (Store store = new Store(context)) {
                while (SystemClock.elapsedRealtime() < deadline) {
                    if (!store.events().isEmpty()) { clicked = true; break; }
                    SystemClock.sleep(100);
                }
                check(clicked, "service automatically clicks demo");
                check(store.events().size() == 1 && store.events().get(0).demo()
                        && store.events().get(0).outcome().equals("accepted"), "exactly one accepted demo action");
                check(store.total() == 0, "demo never increments third-party totals");
            }
            runOnMainSync(demo::finish);
            waitForIdleSync();
            try (Store store = new Store(context)) { store.active(false); store.clear(); }
            Activity pausedDemo = startActivitySync(new Intent(context, DemoActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            SystemClock.sleep(1500);
            try (Store store = new Store(context)) {
                check(store.events().isEmpty(), "pause prevents demo action");
                store.active(true);
            }
            runOnMainSync(pausedDemo::finish);
            waitForIdleSync();
            output.putString("stream", "PASS: " + assertions + " device assertions\n");
            finish(Activity.RESULT_OK, output);
        } catch (Throwable error) {
            output.putString("stream", "FAIL after " + assertions + " assertions: " + android.util.Log.getStackTraceString(error));
            finish(Activity.RESULT_CANCELED, output);
        }
    }
    private void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
        assertions++;
    }
}
