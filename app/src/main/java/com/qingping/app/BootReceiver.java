package com.qingping.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Restores opted-in foreground protection after boot; Android binds accessibility. */
public final class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            try (Store store = new Store(context)) {
                store.boot();
                if (store.autoStart()) ProtectionService.sync(context);
            }
        }
    }
}
