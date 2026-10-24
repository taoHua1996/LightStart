package com.qingping.app;

import android.app.Activity;
import android.os.Build;
import android.graphics.Insets;
import android.graphics.Typeface;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

final class Ui {
    private Ui() {}
    static int dp(Activity activity, int value) { return Math.round(value * activity.getResources().getDisplayMetrics().density); }
    static LinearLayout column(Activity a) {
        LinearLayout v = new LinearLayout(a);
        v.setOrientation(LinearLayout.VERTICAL);
        return v;
    }
    static TextView text(Activity a, String value, int sp, boolean bold) {
        TextView v = new TextView(a);
        v.setText(value);
        v.setTextSize(sp);
        v.setTextColor(a.getColor(R.color.on_surface));
        v.setLineSpacing(dp(a, 3), 1);
        if (bold) v.setTypeface(null, Typeface.BOLD);
        return v;
    }
    static Button button(Activity a, String label, Runnable action) {
        Button v = new Button(a);
        v.setText(label);
        v.setAllCaps(false);
        v.setMinHeight(dp(a, 48));
        v.setOnClickListener(view -> action.run());
        return v;
    }
    static void add(LinearLayout parent, View child, Activity a, int top) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.topMargin = dp(a, top);
        parent.addView(child, p);
    }
    static void insets(Activity a, View root) {
        if (Build.VERSION.SDK_INT >= 27) {
            boolean night = (a.getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                    == android.content.res.Configuration.UI_MODE_NIGHT_YES;
            int flags = a.getWindow().getDecorView().getSystemUiVisibility();
            a.getWindow().getDecorView().setSystemUiVisibility(night ? flags & ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                    : flags | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
            a.getWindow().setNavigationBarColor(a.getColor(R.color.surface));
        }
        a.getWindow().setStatusBarColor(a.getColor(R.color.surface));
        if (Build.VERSION.SDK_INT >= 30) a.getWindow().setDecorFitsSystemWindows(false);
        else a.getWindow().getDecorView().setSystemUiVisibility(a.getWindow().getDecorView().getSystemUiVisibility()
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        root.setOnApplyWindowInsetsListener((view, wi) -> {
            if (Build.VERSION.SDK_INT >= 30) {
                Insets i = wi.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout() | WindowInsets.Type.ime());
                view.setPadding(i.left, i.top, i.right, i.bottom);
            } else {
                view.setPadding(wi.getSystemWindowInsetLeft(), wi.getSystemWindowInsetTop(), wi.getSystemWindowInsetRight(), wi.getSystemWindowInsetBottom());
            }
            return wi;
        });
        root.requestApplyInsets();
    }
}
