package com.qingping.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public final class DemoActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout frame = Ui.column(this);
        LinearLayout body = Ui.column(this);
        body.setPadding(Ui.dp(this, 24), Ui.dp(this, 20), Ui.dp(this, 24), Ui.dp(this, 24));
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(body);
        frame.addView(scroll, new LinearLayout.LayoutParams(-1, -1));
        Button skip = Ui.button(this, "跳过 5s", () -> showResult(body));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-2, -2);
        p.gravity = Gravity.END;
        body.addView(skip, p);
        Ui.add(body, Ui.text(this, "这是一张模拟开屏页", 24, true), this, 40);
        Ui.add(body, Ui.text(this, "没有广告，也不会跳转。服务已连接且未暂停时，会自动点击右上角的跳过按钮。\n\n此页面不设倒计时自动关闭，以便确认是否真的执行了点击。你也可以手动点击。", 16, false), this, 16);
        Ui.add(body, Ui.button(this, "返回轻启", this::finish), this, 24);
        setContentView(frame);
        Ui.insets(this, frame);
    }
    private void showResult(LinearLayout body) {
        body.removeAllViews();
        Ui.add(body, Ui.text(this, "跳过按钮已响应", 24, true), this, 32);
        TextView details = Ui.text(this, "模拟开屏已移除。返回“记录”页查看是否有演示点击记录：只有无障碍服务执行的点击会留下记录，手动点击不会记录。\n\n演示不计入第三方应用统计。", 16, false);
        Ui.add(body, details, this, 16);
        Ui.add(body, Ui.button(this, "返回轻启", this::finish), this, 24);
    }
}
