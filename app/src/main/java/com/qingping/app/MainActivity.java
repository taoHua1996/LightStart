package com.qingping.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity {
    private Store store;
    private LinearLayout content;
    private int page;
    private int generation;
    private String appQuery = "";
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private Runnable refreshStatus = () -> {};
    private final Runnable statusTick = new Runnable() {
        @Override public void run() {
            refreshStatus.run();
            uiHandler.postDelayed(this, 1000);
        }
    };
    private final ExecutorService worker = Executors.newSingleThreadExecutor();

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        store = new Store(this);
        if (state != null) { page = state.getInt("page", 0); appQuery = state.getString("appQuery", ""); }
    }
    @Override protected void onResume() { super.onResume(); ProtectionService.sync(this); render(); uiHandler.post(statusTick); }
    @Override protected void onPause() { uiHandler.removeCallbacks(statusTick); super.onPause(); }
    @Override protected void onSaveInstanceState(Bundle out) { out.putInt("page", page); out.putString("appQuery", appQuery); super.onSaveInstanceState(out); }
    @Override protected void onDestroy() { generation++; worker.shutdownNow(); store.close(); super.onDestroy(); }

    private void render() {
        generation++;
        refreshStatus = () -> {};
        LinearLayout root = Ui.column(this);
        root.setBackgroundColor(getColor(R.color.surface));
        LinearLayout header = Ui.column(this);
        header.setPadding(dp(24), dp(20), dp(24), dp(8));
        Ui.add(header, Ui.text(this, "轻启", 28, true), this, 0);
        Ui.add(header, secondary("少等一会，轻松打开。"), this, 4);
        root.addView(header);

        LinearLayout tabs = new LinearLayout(this);
        tabs.setPadding(dp(16), 0, dp(16), dp(8));
        String[] labels = {"概览", "应用", "记录", "设置"};
        for (int i = 0; i < labels.length; i++) {
            final int index = i;
            Button tab = Ui.button(this, labels[i], () -> { page = index; render(); });
            tab.setSelected(page == i);
            tab.setTextColor(getColor(page == i ? R.color.primary : R.color.secondary));
            tab.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(page == i ? R.color.tonal : R.color.surface)));
            if (page == i) tab.setTypeface(null, android.graphics.Typeface.BOLD);
            tab.setContentDescription(labels[i] + (page == i ? "，当前页面" : ""));
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1);
            p.setMargins(dp(4), 0, dp(4), 0);
            tabs.addView(tab, p);
        }
        root.addView(tabs);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        content = Ui.column(this);
        content.setPadding(dp(24), dp(8), dp(24), dp(32));
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
        Ui.insets(this, root);
        if (page == 0) overview(); else if (page == 1) apps(); else if (page == 2) history(); else settings();
    }

    private void overview() {
        boolean enabled = serviceEnabled();
        TextView state = Ui.text(this, "正在检查使用条件…", 22, true);
        state.setTextColor(getColor(R.color.primary));
        state.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        Ui.add(content, state, this, 12);
        Ui.add(content, secondary("首次使用：授权无障碍 → 一键启用应用 → 测试跳过。默认规则无需填写。"), this, 8);
        TextView checklist = secondary("");
        Ui.add(content, checklist, this, 12);
        Button next = Ui.button(this, "正在检查…", () -> {});
        next.setEnabled(false);
        Ui.add(content, next, this, 8);
        Ui.add(content, Ui.button(this, "测试跳过功能", this::testSkip), this, 8);
        Ui.add(content, secondary("测试入口始终可用，无需先启用第三方应用。若条件未满足，点击后会说明原因。"), this, 4);
        TextView scanStatus = secondary("");
        Ui.add(content, scanStatus, this, 8);
        final int[] enabledCount = {-1};
        refreshStatus = () -> {
            String blocker = serviceBlocker();
            setTextIfChanged(scanStatus, store.scanStatus().isEmpty()
                    ? "尚无应用检查结果。先测试功能，再从桌面打开已启用的应用。"
                    : "最近检查：" + store.scanStatus());
            setTextIfChanged(state, !blocker.isEmpty() ? blocker : enabledCount[0] < 0 ? "正在读取应用…"
                    : enabledCount[0] == 0 ? "还未启用应用" : "已就绪 · 等待应用开屏");
            setTextIfChanged(checklist, "无障碍：" + (!store.consented() ? "待阅读并同意说明" : !serviceEnabled() ? "待系统授权"
                    : !SkipAccessibilityService.connected ? "已授权，等待连接" : "已连接")
                    + "\n自动跳过：" + (store.active() ? "总开关已开启" : "已暂停")
                    + "\n生效应用：" + (enabledCount[0] < 0 ? "正在读取" : getString(R.string.enabled_app_count, enabledCount[0])));
            next.setEnabled(true);
            next.setVisibility(View.VISIBLE);
            if (!store.consented() || !serviceEnabled()) {
                setTextIfChanged(next, "授权无障碍服务"); next.setOnClickListener(v -> enableService());
            } else if (!store.active()) {
                setTextIfChanged(next, "恢复自动跳过"); next.setOnClickListener(v -> { store.active(true); ProtectionService.sync(this); render(); });
            } else if (!SkipAccessibilityService.connected) {
                setTextIfChanged(next, "检查无障碍连接"); next.setOnClickListener(v -> enableService());
            } else if (enabledCount[0] < 0) {
                setTextIfChanged(next, "正在读取应用…"); next.setEnabled(false);
            } else if (enabledCount[0] == 0) {
                setTextIfChanged(next, "去一键启用应用"); next.setOnClickListener(v -> { page = 1; render(); });
            } else {
                next.setVisibility(View.GONE);
            }
        };
        refreshStatus.run();
        final int request = generation;
        worker.execute(() -> {
            List<AppCatalog.Entry> entries = AppCatalog.list(getApplicationContext());
            runOnUiThread(() -> {
                if (isDestroyed() || generation != request) return;
                enabledCount[0] = countEnabled(entries);
                refreshStatus.run();
            });
        });
        Switch active = new Switch(this);
        active.setText("自动跳过总开关");
        active.setTextSize(16);
        active.setMinHeight(dp(56));
        active.setChecked(store.active());
        active.setOnCheckedChangeListener((button, checked) -> { store.active(checked); ProtectionService.sync(this); render(); });
        Ui.add(content, active, this, 12);
        Ui.add(content, Ui.button(this, enabled ? "检查无障碍设置" : "开启无障碍服务", this::enableService), this, 8);
        Ui.add(content, Ui.button(this, "管理生效应用", () -> { page = 1; render(); }), this, 8);
        Ui.add(content, Ui.button(this, "自动识别与后台设置", () -> { page = 3; render(); }), this, 8);

        section("本机统计");
        Ui.add(content, Ui.text(this, "今日 " + store.today() + " 次  ·  累计 " + store.total() + " 次", 20, true), this, 8);
        Ui.add(content, secondary("计入系统已接受的跳过点击，不代表已确认广告消失；演示不计入统计。"), this, 8);
        Ui.add(content, secondary("已就绪表示运行条件满足，不保证每个广告都能识别；不支持全面拦截广告或摇一摇跳转。"), this, 8);

    }

    private void settings() {
        Ui.add(content, Ui.text(this, "自动识别与后台运行", 22, true), this, 8);
        Switch ids = settingSwitch("自动获取跳过按钮 ID", store.autoIds());
        ids.setOnCheckedChangeListener((v, checked) -> store.autoIds(checked));
        Ui.add(content, ids, this, 12);
        Ui.add(content, secondary("默认开启。识别右上角或底部两角的完整跳过文字，点击被接受后记住按钮 ID。无需逐个填写；没有 ID 时继续使用文字识别。手动规则优先。"), this, 4);

        section("后台保护");
        Switch protection = settingSwitch("开启后台保护", store.protection());
        protection.setOnCheckedChangeListener((v, checked) -> {
            store.protection(checked);
            if (checked && Build.VERSION.SDK_INT >= 33 && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED)
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 41);
            ProtectionService.sync(this);
            refreshStatus.run();
        });
        Ui.add(content, protection, this, 8);
        Ui.add(content, secondary("显示常驻通知，减少后台被回收的机会。划掉轻启界面不会主动关闭保护；你可以在这里或通知中关闭。"), this, 4);
        Switch boot = settingSwitch("开机恢复后台保护", store.autoStart());
        boot.setOnCheckedChangeListener((v, checked) -> store.autoStart(checked));
        Ui.add(content, boot, this, 8);
        Ui.add(content, secondary("需要同时开启后台保护和自动跳过，并保留无障碍授权。系统自启动权限仍需在下方设置页允许。"), this, 4);
        TextView status = secondary("");
        Ui.add(content, status, this, 12);
        refreshStatus = () -> {
            PowerManager power = getSystemService(PowerManager.class);
            boolean battery = power != null && power.isIgnoringBatteryOptimizations(getPackageName());
            boolean notifications = getSystemService(android.app.NotificationManager.class).areNotificationsEnabled();
            String guard = !store.protection() ? "已关闭" : ProtectionService.running ? "运行中"
                    : !store.consented() || !serviceEnabled() ? "等待无障碍授权"
                    : !store.active() ? "随自动跳过暂停" : "未运行，请点击恢复";
            if (protection.isChecked() != store.protection()) protection.setChecked(store.protection());
            setTextIfChanged(status, "后台保护：" + guard + "\n无障碍连接：" + (SkipAccessibilityService.connected ? "已连接" : "未连接")
                    + "\n电池限制：" + (battery ? "已豁免" : "待允许")
                    + "\n通知：" + (notifications ? "已允许" : "未允许，状态通知可能隐藏")
                    + "\n系统自启动权限：需在系统设置确认"
                    + (store.protectionStatus().isEmpty() ? "" : "\n" + store.protectionStatus()));
        };
        refreshStatus.run();
        Ui.add(content, Ui.button(this, "恢复后台保护", () -> {
            if (!store.protection()) protection.setChecked(true);
            if (!store.consented() || !serviceEnabled()) enableService();
            else if (!store.active()) new AlertDialog.Builder(this).setTitle("自动跳过已暂停")
                    .setMessage("请在概览恢复自动跳过后，再启动后台保护。")
                    .setPositiveButton("前往概览", (d, w) -> { page = 0; render(); }).setNegativeButton("返回", null).show();
            else ProtectionService.sync(this);
        }), this, 8);
        Ui.add(content, Ui.button(this, "设置系统自启动", () -> {
            if (!BackgroundSettings.openAutostart(this)) new AlertDialog.Builder(this).setTitle("在系统中允许自启动")
                    .setMessage("此系统未提供可直接打开的自启动页面。请进入轻启应用信息，查找自启动或后台运行；也可以在系统设置中搜索这两个名称。")
                    .setPositiveButton("打开应用信息", (d, w) -> open(BackgroundSettings.appDetails(this)))
                    .setNegativeButton("返回", null).show();
        }), this, 8);
        Ui.add(content, Ui.button(this, "允许不受电池优化限制", () -> {
            PowerManager power = getSystemService(PowerManager.class);
            if (power != null && power.isIgnoringBatteryOptimizations(getPackageName()))
                open(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
            else open(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:" + getPackageName())));
        }), this, 8);
        Ui.add(content, Ui.button(this, "通知设置", () -> open(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName()))), this, 8);
        Ui.add(content, Ui.button(this, "后台运行与应用信息", () -> open(BackgroundSettings.appDetails(this))), this, 8);
        Ui.add(content, secondary("系统强行停止、系统任务管理器停止或部分厂商一键清理仍可终止保护。此时需重新打开轻启；普通应用无法保证永不被杀。部分系统还需在最近任务中锁定轻启。"), this, 12);
        if (store.bootAt() != 0) Ui.add(content, secondary("最近收到开机通知：" + date(store.bootAt()) + "\n开机通知不等于无障碍已连接，请以上方实际状态为准。"), this, 8);
        Ui.add(content, Ui.button(this, "权限与隐私说明", this::privacy), this, 16);
    }

    private Switch settingSwitch(String title, boolean checked) {
        Switch view = new Switch(this);
        view.setText(title); view.setTextSize(16); view.setMinHeight(dp(56)); view.setChecked(checked);
        return view;
    }

    private void apps() {
        Ui.add(content, Ui.text(this, "让哪些应用更清静", 22, true), this, 8);
        Ui.add(content, secondary("不用逐个填写规则。开启后自动识别“跳过 / Skip”等按钮；高级规则仅在默认识别无效时使用。"), this, 8);
        Ui.add(content, secondary("一键启用当前全部可配置应用，包括搜索未显示的应用。保留已有规则；新装应用需再次启用。"), this, 8);
        Button enableAll = Ui.button(this, "一键启用", () -> {});
        Button disableAll = Ui.button(this, "全部关闭", () -> {});
        enableAll.setEnabled(false);
        disableAll.setEnabled(false);
        Ui.add(content, enableAll, this, 12);
        Ui.add(content, disableAll, this, 8);
        TextView batchStatus = secondary("正在读取应用，完成后即可一键启用…");
        batchStatus.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        Ui.add(content, batchStatus, this, 8);
        Button resolve = Ui.button(this, "完成使用设置", () -> { page = 0; render(); });
        Ui.add(content, resolve, this, 8);
        EditText search = new EditText(this);
        search.setHint("搜索应用名称或包名");
        search.setSingleLine(true);
        search.setMinHeight(dp(56));
        search.setText(appQuery);
        Ui.add(content, search, this, 12);
        LinearLayout list = Ui.column(this);
        Ui.add(content, list, this, 8);
        Ui.add(list, secondary("正在读取可启动的应用…"), this, 8);
        final int request = generation;
        worker.execute(() -> {
            List<AppCatalog.Entry> entries = AppCatalog.list(getApplicationContext());
            runOnUiThread(() -> {
                if (isDestroyed() || generation != request) return;
                Runnable updateStatus = () -> {
                    int count = countEnabled(entries);
                    String blocker = serviceBlocker();
                    setTextIfChanged(batchStatus, getString(R.string.apps_enabled_summary, count, entries.size(),
                            !blocker.isEmpty() ? "\n" + blocker : count == 0 ? "\n点击一键启用，或开启下方应用" : "\n已就绪，可从桌面打开已启用的应用"));
                    setTextIfChanged(resolve, blocker.isEmpty() ? "查看运行状态"
                            : !store.consented() || !serviceEnabled() ? "去完成无障碍授权"
                            : !store.active() ? "去恢复自动跳过" : "去检查服务连接");
                };
                refreshStatus = updateStatus;
                enableAll.setEnabled(!entries.isEmpty());
                disableAll.setEnabled(!entries.isEmpty());
                enableAll.setOnClickListener(view -> {
                    store.enableAll(entries, true);
                    populate(list, entries, search.getText().toString(), updateStatus);
                    updateStatus.run();
                });
                disableAll.setOnClickListener(view -> {
                    store.enableAll(entries, false);
                    populate(list, entries, search.getText().toString(), updateStatus);
                    updateStatus.run();
                });
                updateStatus.run();
                populate(list, entries, search.getText().toString(), updateStatus);
                search.addTextChangedListener(new TextWatcher() {
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        appQuery = s.toString(); populate(list, entries, appQuery, updateStatus);
                    }
                    public void afterTextChanged(Editable e) {}
                });
            });
        });
    }

    private void populate(LinearLayout list, List<AppCatalog.Entry> entries, String query, Runnable updateStatus) {
        list.removeAllViews();
        String q = query.trim().toLowerCase(Locale.ROOT);
        int visible = 0;
        for (AppCatalog.Entry entry : entries) {
            if (!(entry.label() + " " + entry.pkg()).toLowerCase(Locale.ROOT).contains(q)) continue;
            visible++;
            Switch toggle = new Switch(this);
            toggle.setText(entry.label());
            toggle.setTextSize(18);
            toggle.setMinHeight(dp(56));
            toggle.setChecked(store.enabled(entry.pkg()));
            toggle.setOnCheckedChangeListener((view, checked) -> { store.enable(entry.pkg(), checked); updateStatus.run(); });
            Ui.add(list, toggle, this, 12);
            Ui.add(list, secondary(entry.pkg()), this, 0);
            TextView ruleStatus = secondary(ruleSummary(entry.pkg()));
            Ui.add(list, ruleStatus, this, 4);
            Ui.add(list, Ui.button(this, "高级规则（可选）· " + entry.label(),
                    () -> editRule(entry, () -> ruleStatus.setText(ruleSummary(entry.pkg())))), this, 8);
            divider(list);
        }
        if (visible == 0) Ui.add(list, secondary(q.isEmpty() ? "没有发现可配置的应用。工作资料或其他用户中的应用可能不可见。" : "没有匹配的应用，试试应用名或包名。"), this, 16);
    }

    private String ruleSummary(String pkg) {
        if (!store.text(pkg).isEmpty() || !store.viewId(pkg).isEmpty()) return "手动规则 · 优先使用";
        if (!store.autoIds()) return "文字自动识别 · 自动获取 ID 已关闭";
        return store.learnedId(pkg).isEmpty() ? "自动识别 · 打开应用后获取跳过按钮 ID"
                : "已自动获取 ID：" + store.learnedId(pkg);
    }

    private void editRule(AppCatalog.Entry entry, Runnable changed) {
        LinearLayout form = Ui.column(this);
        form.setPadding(dp(24), dp(8), dp(24), dp(8));
        Ui.add(form, secondary("留空即可自动识别右上角和底部两角的跳过按钮，并自动记住 ID。手动填写两项时必须同时满足。"), this, 0);
        Ui.add(form, Ui.text(this, "按钮完整文字（可选）", 16, true), this, 16);
        EditText text = new EditText(this);
        text.setSingleLine(true);
        text.setHint("例如：跳过广告");
        text.setText(store.text(entry.pkg()));
        Ui.add(form, text, this, 4);
        Ui.add(form, Ui.text(this, "控件资源 ID（可选，进阶）", 16, true), this, 16);
        EditText id = new EditText(this);
        id.setSingleLine(true);
        id.setHint(entry.pkg() + ":id/skip");
        id.setText(store.viewId(entry.pkg()));
        Ui.add(form, id, this, 4);
        String learned = store.learnedId(entry.pkg());
        Ui.add(form, secondary(learned.isEmpty() ? "尚未自动获取 ID。保持自动获取开启，再打开此应用的开屏广告。" : "自动获取的 ID：" + learned), this, 8);
        if (!learned.isEmpty()) Ui.add(form, Ui.button(this, "填入已获取的 ID", () -> id.setText(learned)), this, 8);
        Ui.add(form, secondary("不要填写“确定”“支付”“允许”等通用按钮。手动规则取消角落位置限制，仍限制按钮面积与开屏时间。"), this, 12);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(form);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle(entry.label() + " · 规则").setView(scroll)
                .setNegativeButton("取消", null).setNeutralButton("重新自动识别", (d, which) -> { store.rule(entry.pkg(), "", ""); store.forgetId(entry.pkg()); store.autoIds(true); changed.run(); })
                .setPositiveButton("保存", null).create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            String t = text.getText().toString().trim();
            String v = id.getText().toString().trim();
            if (t.length() > 40) { text.setError("请填写不超过 40 字的完整按钮文字"); return; }
            if (!v.isEmpty() && !v.matches("[A-Za-z0-9_.]+:id/[A-Za-z0-9_]+")) { id.setError("格式示例：应用包名:id/skip"); return; }
            store.rule(entry.pkg(), t, v);
            changed.run();
            dialog.dismiss();
        }));
        dialog.show();
    }

    private void history() {
        Ui.add(content, Ui.text(this, "每一次跳过，有据可查", 22, true), this, 8);
        Ui.add(content, secondary("累计按应用保存，详细记录最多保留 500 条，此处显示最近 100 条。拒绝执行及演示不计入累计。"), this, 8);
        section("按应用统计");
        List<Store.Count> counts = store.counts();
        if (counts.isEmpty()) Ui.add(content, secondary("还没有跳过记录。开启服务、选择应用后，再从桌面打开该应用。"), this, 8);
        for (Store.Count count : counts) {
            Ui.add(content, Ui.text(this, AppCatalog.label(this, count.pkg()) + "  ·  " + count.count() + " 次", 18, true), this, 16);
            Ui.add(content, secondary(count.pkg()), this, 4);
        }
        section("最近操作");
        List<Store.Event> events = store.events();
        if (events.isEmpty()) Ui.add(content, secondary("暂无操作。测试演示后，这里也会显示对应记录。"), this, 8);
        for (Store.Event event : events) {
            Ui.add(content, Ui.text(this, (event.demo() ? "功能演示" : AppCatalog.label(this, event.pkg())) + " · "
                    + (event.outcome().equals("accepted") ? "点击已提交" : "控件拒绝点击"), 16, true), this, 16);
            Ui.add(content, secondary(date(event.at())), this, 4);
            divider(content);
        }
        Ui.add(content, Ui.button(this, "清空统计与记录", () -> new AlertDialog.Builder(this).setTitle("清空本机统计？")
                .setMessage("累计次数、每日统计与操作记录都会删除，应用开关与规则会保留。此操作无法撤销。")
                .setNegativeButton("取消", null).setPositiveButton("清空", (dialog, which) -> { store.clear(); render(); }).show()), this, 24);
    }

    private boolean serviceEnabled() {
        String value = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        String component = new ComponentName(this, SkipAccessibilityService.class).flattenToString();
        if (value != null) for (String item : value.split(":")) {
            ComponentName parsed = ComponentName.unflattenFromString(item);
            if (parsed != null && component.equals(parsed.flattenToString())) return true;
        }
        return false;
    }
    private int countEnabled(List<AppCatalog.Entry> entries) {
        int count = 0;
        for (AppCatalog.Entry entry : entries) if (store.enabled(entry.pkg())) count++;
        return count;
    }
    private String serviceBlocker() {
        if (!store.consented()) return "请先阅读并同意无障碍说明";
        if (!serviceEnabled()) return "尚未开启无障碍服务";
        if (!store.active()) return "自动跳过已暂停";
        if (!SkipAccessibilityService.connected) return "已授权，等待服务连接";
        return "";
    }
    private void testSkip() {
        String blocker = serviceBlocker();
        if (!blocker.isEmpty()) {
            boolean paused = store.consented() && serviceEnabled() && !store.active();
            new AlertDialog.Builder(this).setTitle("暂时无法自动测试")
                    .setMessage(blocker + (paused ? "。恢复总开关后，再点击测试。"
                            : "。请先完成无障碍设置，再返回测试。若已授权仍未连接，可在系统设置关闭后重新开启轻启服务。"))
                    .setNegativeButton("返回", null)
                    .setPositiveButton(paused ? "恢复自动跳过" : "前往设置", (d, w) -> {
                        if (paused) { store.active(true); ProtectionService.sync(this); render(); } else enableService();
                    }).show();
            return;
        }
        startActivity(new Intent(this, DemoActivity.class));
    }
    private void enableService() {
        if (store.consented()) { open(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)); return; }
        new AlertDialog.Builder(this).setTitle("允许轻启识别并点击跳过？")
                .setMessage(getString(R.string.service_description) + "\n\n下一步将在安卓设置中由你手动开启。部分 Android 13+ 手机需要在应用详情的菜单中先允许受限设置。")
                .setNegativeButton("暂不开启", null).setPositiveButton("同意并前往设置", (d, w) -> {
                    store.consent(); open(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                }).show();
    }
    private void privacy() {
        new AlertDialog.Builder(this).setTitle("权限与隐私")
                .setMessage("无障碍：读取窗口控件并点击匹配的跳过按钮，只在本机处理，不保存页面文字。自动获取只保存应用包名与跳过按钮 ID，可在规则页重新识别。\n\n后台保护：经你开启后显示常驻通知；可在设置页或通知中关闭。开机恢复为可选开关，由系统决定是否允许启动。电池优化豁免需要系统确认。\n\n应用列表：仅查询可启动应用。不申请网络、悬浮窗、通讯录或全部应用可见性权限。配置、ID 和统计存于本机，卸载或清除数据会删除。\n\n暂停会停止扫描和后台保护；彻底撤销访问请在系统无障碍设置中关闭轻启。")
                .setPositiveButton("知道了", null).show();
    }
    private void open(Intent intent) {
        try { startActivity(intent); }
        catch (ActivityNotFoundException | SecurityException e) {
            new AlertDialog.Builder(this).setTitle("无法打开该设置页").setMessage("请手动打开系统设置，搜索“无障碍”“电池优化”或“轻启”。不同品牌的入口可能不同。")
                    .setPositiveButton("知道了", null).show();
        }
    }
    private void section(String title) { divider(content); Ui.add(content, Ui.text(this, title, 18, true), this, 16); }
    private void divider(LinearLayout parent) {
        View v = new View(this);
        v.setBackgroundColor(getColor(R.color.outline));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(1));
        p.topMargin = dp(16);
        parent.addView(v, p);
    }
    private TextView secondary(String text) { TextView v = Ui.text(this, text, 14, false); v.setTextColor(getColor(R.color.secondary)); return v; }
    private static void setTextIfChanged(TextView view, String text) {
        if (!text.contentEquals(view.getText())) view.setText(text);
    }
    private int dp(int value) { return Ui.dp(this, value); }
    private static String date(long value) { return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM, Locale.CHINA).format(new Date(value)); }
}
