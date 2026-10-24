package com.qingping.app;

import android.accessibilityservice.AccessibilityService;
import android.app.KeyguardManager;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import java.util.ArrayDeque;
import java.util.Set;

public final class SkipAccessibilityService extends AccessibilityService {
    static volatile boolean connected;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final SkipPolicy.Session session = new SkipPolicy.Session();
    private Store store;
    private Set<String> excluded;
    private boolean demo;
    private long lastScan;
    private boolean scanPending;
    private long transitionUntil;
    private final Runnable scanTask = () -> { scanPending = false; scan(); };

    @Override protected void onServiceConnected() {
        store = new Store(this);
        excluded = AppCatalog.excluded(this);
        connected = true;
        session.reset();
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if (store == null || !store.consented() || !store.active()) { reset(); return; }
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            transitionUntil = SystemClock.elapsedRealtime() + 1500;
            if (getPackageName().equals(string(event.getPackageName()))) {
                demo = DemoActivity.class.getName().equals(string(event.getClassName()));
            }
        }
        schedule(Math.max(80, SkipPolicy.SCAN_INTERVAL_MS - (SystemClock.elapsedRealtime() - lastScan)));
    }

    private void scan() {
        lastScan = SystemClock.elapsedRealtime();
        PowerManager power = getSystemService(PowerManager.class);
        KeyguardManager keyguard = getSystemService(KeyguardManager.class);
        if (store == null || !store.active() || !store.consented() || power == null || !power.isInteractive()
                || (keyguard != null && keyguard.isKeyguardLocked())) { reset(); return; }
        AccessibilityNodeInfo root = activeRoot();
        if (root == null) {
            retryTransition();
            return;
        }
        String pkg = string(root.getPackageName());
        boolean isDemo = pkg.equals(getPackageName()) && demo;
        boolean open = session.observe(pkg, lastScan);
        if ((pkg.equals(getPackageName()) && !isDemo) || (!isDemo && excluded.contains(pkg))) {
            root.recycle();
            if (pkg.equals(getPackageName())) session.reset();
            retryTransition();
            return;
        }
        if (!isDemo && !store.enabled(pkg)) {
            report(pkg, "尚未启用，请到应用页开启该应用或使用一键启用");
            root.recycle(); retryTransition(); return;
        }
        if (!open) {
            root.recycle();
            if (lastScan < transitionUntil) schedule(SkipPolicy.SCAN_INTERVAL_MS);
            return;
        }
        Rect window = new Rect();
        root.getBoundsInScreen(window);
        AccessibilityNodeInfo target = findTarget(root, pkg, window, isDemo);
        if (target != null) {
            // One attempt per foreground session, including rejected actions, prevents loops.
            session.attempted();
            boolean accepted;
            try { accepted = target.performAction(AccessibilityNodeInfo.ACTION_CLICK); }
            catch (IllegalStateException e) { accepted = false; }
            finally { target.recycle(); }
            store.record(pkg, accepted, isDemo);
            report(pkg, accepted ? "已提交跳过点击，请确认广告是否消失" : "找到按钮，但系统拒绝了点击");
        } else if (session.canScan(pkg, SystemClock.elapsedRealtime())) {
            report(pkg, "暂未找到符合规则的可点击按钮；默认识别仅限右上角，未向无障碍提供的按钮无法识别");
            schedule(SkipPolicy.SCAN_INTERVAL_MS);
        }
    }

    private void retryTransition() {
        if (lastScan < transitionUntil || session.isOpen(lastScan)) schedule(SkipPolicy.SCAN_INTERVAL_MS);
    }

    private void report(String pkg, String reason) {
        store.scanStatus(AppCatalog.label(this, pkg) + "：" + reason);
    }

    private AccessibilityNodeInfo findTarget(AccessibilityNodeInfo root, String pkg, Rect window, boolean isDemo) {
        String exactText = isDemo ? "" : store.text(pkg);
        String exactId = isDemo ? "" : store.viewId(pkg);
        ArrayDeque<AccessibilityNodeInfo> queue = new ArrayDeque<>();
        queue.add(root);
        int visited = 0;
        AccessibilityNodeInfo found = null;
        try {
            while (!queue.isEmpty() && visited++ < 250) {
                AccessibilityNodeInfo node = queue.removeFirst();
                try {
                    if (!pkg.equals(string(node.getPackageName()))) continue;
                    Rect bounds = new Rect();
                    node.getBoundsInScreen(bounds);
                    boolean upperRight = bounds.centerX() >= window.left + window.width() * .5f
                            && bounds.centerY() <= window.top + window.height() * .4f;
                    if (node.isVisibleToUser() && node.isEnabled()
                            && SkipPolicy.matches(string(node.getText()), string(node.getContentDescription()),
                            node.getViewIdResourceName(), exactText, exactId, upperRight)) {
                        found = clickTarget(node, pkg, window);
                        if (found != null) break;
                    }
                    for (int i = 0; i < node.getChildCount() && queue.size() < 250; i++) {
                        AccessibilityNodeInfo child = node.getChild(i);
                        if (child != null) queue.addLast(child);
                    }
                } finally { node.recycle(); }
            }
        } finally { while (!queue.isEmpty()) queue.removeFirst().recycle(); }
        return found;
    }

    private AccessibilityNodeInfo clickTarget(AccessibilityNodeInfo matched, String pkg, Rect window) {
        AccessibilityNodeInfo current = AccessibilityNodeInfo.obtain(matched);
        for (int depth = 0; current != null && depth < 3; depth++) {
            Rect bounds = new Rect();
            current.getBoundsInScreen(bounds);
            long area = (long) bounds.width() * bounds.height();
            if (!pkg.equals(string(current.getPackageName())) || !current.isVisibleToUser() || !current.isEnabled()
                    || bounds.isEmpty() || !window.contains(bounds) || area > (long) window.width() * window.height() / 8) {
                current.recycle(); return null;
            }
            if (current.isClickable()) return current;
            AccessibilityNodeInfo parent = current.getParent();
            current.recycle();
            current = parent;
        }
        if (current != null) current.recycle();
        return null;
    }

    private static String string(CharSequence value) { return value == null ? "" : value.toString(); }
    private AccessibilityNodeInfo activeRoot() {
        // Prefer the focused app window: transition events can precede active-window updates.
        AccessibilityNodeInfo focused = null;
        for (AccessibilityWindowInfo window : getWindows()) {
            try {
                if (focused == null && window.getType() == AccessibilityWindowInfo.TYPE_APPLICATION && window.isFocused())
                    focused = window.getRoot();
            } finally { window.recycle(); }
        }
        return focused != null ? focused : getRootInActiveWindow();
    }
    private void schedule(long delay) {
        if (!scanPending) { scanPending = true; handler.postDelayed(scanTask, delay); }
    }
    private void reset() { session.reset(); demo = false; scanPending = false; handler.removeCallbacks(scanTask); }
    @Override public void onInterrupt() { reset(); }
    @Override public boolean onUnbind(android.content.Intent intent) { connected = false; reset(); return super.onUnbind(intent); }
    @Override public void onDestroy() { connected = false; reset(); if (store != null) store.close(); super.onDestroy(); }
}
