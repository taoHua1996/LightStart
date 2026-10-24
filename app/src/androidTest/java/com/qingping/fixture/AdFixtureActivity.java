package com.qingping.fixture;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Separate-package fixture, shipped only inside the test APK, never the user APK. */
public final class AdFixtureActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(40, 40, 40, 40);
        Button skip = new Button(this);
        skip.setText("跳过 5s");
        LinearLayout.LayoutParams position = new LinearLayout.LayoutParams(-2, -2);
        position.gravity = Gravity.END;
        root.addView(skip, position);
        TextView status = new TextView(this);
        status.setTextSize(24);
        status.setText("跨应用测试样例：等待跳过\n此应用没有广告、网络、倒计时或自动跳转。只有真实点击才会改变文字。");
        root.addView(status);
        skip.setOnClickListener(view -> { status.setText("跨应用测试样例：跳过已响应"); root.removeView(skip); });
        setContentView(root);
    }
}
