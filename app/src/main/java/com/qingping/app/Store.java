package com.qingping.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

final class Store extends SQLiteOpenHelper {
    static final String PREFS = "qingping";
    private final SharedPreferences prefs;

    Store(Context context) {
        super(context.getApplicationContext(), "qingping.db", null, 1);
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE totals (package TEXT PRIMARY KEY, count INTEGER NOT NULL DEFAULT 0)");
        db.execSQL("CREATE TABLE daily (day TEXT PRIMARY KEY, count INTEGER NOT NULL DEFAULT 0)");
        db.execSQL("CREATE TABLE events (_id INTEGER PRIMARY KEY AUTOINCREMENT, package TEXT NOT NULL, at INTEGER NOT NULL, outcome TEXT NOT NULL, demo INTEGER NOT NULL DEFAULT 0)");
    }
    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        throw new IllegalStateException("Database migration required");
    }
    boolean consented() { return prefs.getBoolean("consent", false); }
    void consent() { prefs.edit().putBoolean("consent", true).apply(); }
    boolean active() { return prefs.getBoolean("active", true); }
    void active(boolean value) { prefs.edit().putBoolean("active", value).apply(); }
    boolean enabled(String pkg) { return prefs.getBoolean("enabled:" + pkg, false); }
    void enable(String pkg, boolean value) { prefs.edit().putBoolean("enabled:" + pkg, value).apply(); }
    void enableAll(List<AppCatalog.Entry> apps, boolean value) {
        SharedPreferences.Editor edit = prefs.edit();
        for (AppCatalog.Entry app : apps) edit.putBoolean("enabled:" + app.pkg(), value);
        edit.apply();
    }
    String text(String pkg) { return prefs.getString("text:" + pkg, ""); }
    String viewId(String pkg) { return prefs.getString("id:" + pkg, ""); }
    void rule(String pkg, String text, String id) {
        prefs.edit().putString("text:" + pkg, text).putString("id:" + pkg, id).apply();
    }
    long bootAt() { return prefs.getLong("bootAt", 0); }
    void boot() { prefs.edit().putLong("bootAt", System.currentTimeMillis()).apply(); }

    synchronized void record(String pkg, boolean accepted, boolean demo) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put("package", pkg);
            values.put("at", System.currentTimeMillis());
            values.put("outcome", accepted ? "accepted" : "rejected");
            values.put("demo", demo ? 1 : 0);
            db.insertOrThrow("events", null, values);
            if (accepted && !demo) {
                db.execSQL("INSERT OR IGNORE INTO totals(package,count) VALUES (?,0)", new Object[]{pkg});
                db.execSQL("UPDATE totals SET count=count+1 WHERE package=?", new Object[]{pkg});
                String day = java.time.LocalDate.now().toString();
                db.execSQL("INSERT OR IGNORE INTO daily(day,count) VALUES (?,0)", new Object[]{day});
                db.execSQL("UPDATE daily SET count=count+1 WHERE day=?", new Object[]{day});
            }
            db.execSQL("DELETE FROM events WHERE _id NOT IN (SELECT _id FROM events ORDER BY _id DESC LIMIT 500)");
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }

    long total() {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT COALESCE(SUM(count),0) FROM totals", null)) {
            return c.moveToFirst() ? c.getLong(0) : 0;
        }
    }
    long today() {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT count FROM daily WHERE day=?", new String[]{java.time.LocalDate.now().toString()})) {
            return c.moveToFirst() ? c.getLong(0) : 0;
        }
    }
    List<Count> counts() {
        List<Count> result = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery("SELECT package,count FROM totals ORDER BY count DESC,package", null)) {
            while (c.moveToNext()) result.add(new Count(c.getString(0), c.getLong(1)));
        }
        return result;
    }
    List<Event> events() {
        List<Event> result = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery("SELECT package,at,outcome,demo FROM events ORDER BY _id DESC LIMIT 100", null)) {
            while (c.moveToNext()) result.add(new Event(c.getString(0), c.getLong(1), c.getString(2), c.getInt(3) == 1));
        }
        return result;
    }
    void clear() {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete("totals", null, null);
            db.delete("events", null, null);
            db.delete("daily", null, null);
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }
    record Count(String pkg, long count) {}
    record Event(String pkg, long at, String outcome, boolean demo) {}
}
