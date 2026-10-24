package com.qingping.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

/** User-controlled foreground protection. Android still owns accessibility binding. */
public final class ProtectionService extends Service {
    static final String STOP = "com.qingping.app.STOP_PROTECTION";
    private static final String CHANNEL = "background_protection";
    private static final int NOTIFICATION_ID = 41;
    static volatile boolean running;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable checkEnabled = new Runnable() {
        @Override public void run() {
            try (Store store = new Store(ProtectionService.this)) {
                if (!wanted(ProtectionService.this, store)) { stopSelf(); return; }
            }
            handler.postDelayed(this, 5000);
        }
    };

    static boolean accessibilityEnabled(Context context) {
        String enabled = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        ComponentName expected = new ComponentName(context, SkipAccessibilityService.class);
        if (enabled != null) for (String item : enabled.split(":"))
            if (expected.equals(ComponentName.unflattenFromString(item))) return true;
        return false;
    }

    static boolean wanted(Context context, Store store) {
        return store.protection() && store.consented() && store.active() && accessibilityEnabled(context);
    }

    static void sync(Context context) {
        try (Store store = new Store(context)) {
            if (!wanted(context, store)) {
                context.stopService(new Intent(context, ProtectionService.class));
                return;
            }
            try {
                context.startForegroundService(new Intent(context, ProtectionService.class));
            } catch (IllegalStateException | SecurityException e) {
                store.protectionStatus("系统暂未允许启动后台保护，请打开轻启后点击恢复后台保护。");
            }
        }
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        try (Store store = new Store(this)) {
            if (intent != null && STOP.equals(intent.getAction())) {
                store.protection(false);
                store.protectionStatus("");
                stopForeground(STOP_FOREGROUND_REMOVE);
                stopSelf();
                running = false;
                return START_NOT_STICKY;
            }
            if (!wanted(this, store)) {
                stopSelf();
                return START_NOT_STICKY;
            }
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(new NotificationChannel(CHANNEL, "后台保护", NotificationManager.IMPORTANCE_LOW));
            PendingIntent open = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            PendingIntent stop = PendingIntent.getService(this, 1, new Intent(this, ProtectionService.class).setAction(STOP), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            Notification notification = new Notification.Builder(this, CHANNEL)
                    .setSmallIcon(R.drawable.ic_protection).setContentTitle("轻启 · 后台保护中")
                    .setContentText("保持跳过服务可用，点击查看实际连接状态")
                    .setContentIntent(open).setOngoing(true).setOnlyAlertOnce(true)
                    .addAction(new Notification.Action.Builder(null, "关闭后台保护", stop).build()).build();
            try {
                if (Build.VERSION.SDK_INT >= 34) startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
                else startForeground(NOTIFICATION_ID, notification);
                running = true;
                store.protectionStatus("");
                handler.removeCallbacks(checkEnabled);
                handler.postDelayed(checkEnabled, 5000);
            } catch (IllegalStateException | SecurityException e) {
                store.protectionStatus("系统拒绝后台保护，请检查后台运行限制并重新开启。");
                stopSelf();
                return START_NOT_STICKY;
            }
        }
        // A process reclaim can be restarted by Android. Force-stop cannot be bypassed.
        return START_STICKY;
    }

    @Override public void onDestroy() { handler.removeCallbacks(checkEnabled); running = false; super.onDestroy(); }
    @Override public IBinder onBind(Intent intent) { return null; }
}
