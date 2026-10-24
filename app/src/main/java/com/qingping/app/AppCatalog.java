package com.qingping.app;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.view.inputmethod.InputMethodManager;
import java.text.Collator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

final class AppCatalog {
    record Entry(String pkg, String label) {}

    static Set<String> excluded(Context context) {
        Set<String> result = new HashSet<>(java.util.Arrays.asList("android", "com.android.systemui", "com.android.settings",
                "com.google.android.permissioncontroller", "com.android.permissioncontroller",
                "com.android.packageinstaller", "com.google.android.packageinstaller", context.getPackageName()));
        Intent home = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
        for (ResolveInfo info : context.getPackageManager().queryIntentActivities(home, 0)) result.add(info.activityInfo.packageName);
        Intent dialer = new Intent(Intent.ACTION_DIAL);
        for (ResolveInfo info : context.getPackageManager().queryIntentActivities(dialer, 0)) result.add(info.activityInfo.packageName);
        InputMethodManager im = context.getSystemService(InputMethodManager.class);
        if (im != null) im.getInputMethodList().forEach(info -> result.add(info.getPackageName()));
        return result;
    }

    static List<Entry> list(Context context) {
        PackageManager pm = context.getPackageManager();
        Set<String> excluded = excluded(context);
        Set<String> seen = new HashSet<>();
        List<Entry> result = new ArrayList<>();
        Intent intent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        for (ResolveInfo info : pm.queryIntentActivities(intent, 0)) {
            String pkg = info.activityInfo.packageName;
            if (!excluded.contains(pkg) && seen.add(pkg)) result.add(new Entry(pkg, info.loadLabel(pm).toString()));
        }
        Collator collator = Collator.getInstance(Locale.CHINA);
        result.sort((a, b) -> collator.compare(a.label(), b.label()));
        return result;
    }
    static String label(Context context, String pkg) {
        try { return context.getPackageManager().getApplicationLabel(context.getPackageManager().getApplicationInfo(pkg, 0)).toString(); }
        catch (PackageManager.NameNotFoundException e) { return pkg; }
    }
}
