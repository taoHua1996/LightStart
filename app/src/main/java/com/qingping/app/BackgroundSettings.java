package com.qingping.app;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import java.util.Locale;

final class BackgroundSettings {
    private BackgroundSettings() {}

    static boolean openAutostart(Activity activity) {
        String maker = Build.MANUFACTURER.toLowerCase(Locale.ROOT);
        String[][] targets;
        if (maker.contains("xiaomi")) targets = new String[][]{{"com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"}};
        else if (maker.contains("vivo")) targets = new String[][]{
                {"com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"},
                {"com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager"}};
        else if (maker.contains("huawei") || maker.contains("honor")) targets = new String[][]{
                {"com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"},
                {"com.hihonor.systemmanager", "com.hihonor.systemmanager.startupmgr.ui.StartupNormalAppListActivity"}};
        else if (maker.contains("oppo") || maker.contains("realme") || maker.contains("oneplus")) targets = new String[][]{
                {"com.oplus.safecenter", "com.oplus.safecenter.startupapp.StartupAppListActivity"},
                {"com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"}};
        else targets = new String[0][];
        for (String[] target : targets) {
            try {
                activity.startActivity(new Intent().setComponent(new ComponentName(target[0], target[1]))
                        .putExtra("packageName", activity.getPackageName()));
                return true;
            } catch (ActivityNotFoundException | SecurityException ignored) { /* Try the next public entry. */ }
        }
        return false;
    }

    static Intent appDetails(Activity activity) {
        return new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + activity.getPackageName()));
    }
}
