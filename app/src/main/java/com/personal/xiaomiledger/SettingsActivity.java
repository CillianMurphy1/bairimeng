package com.personal.xiaomiledger;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

public class SettingsActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Ui.applyBackground(this);
        buildUi();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Ui.PAPER);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(Ui.dp(this, 18), Ui.dp(this, 16), Ui.dp(this, 18), Ui.dp(this, 30));
        scroll.addView(root);

        LinearLayout top = Ui.row(this);
        TextView back = Ui.text(this, "‹", 38, Ui.INK, Typeface.NORMAL);
        back.setGravity(Gravity.CENTER);
        back.setOnClickListener(v -> finish());
        top.addView(back, new LinearLayout.LayoutParams(Ui.dp(this, 42), Ui.dp(this, 42)));
        top.addView(Ui.text(this, "设置·关于", 25, Ui.INK, Typeface.BOLD), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        root.addView(top);
        root.addView(Ui.spacer(this, 16));

        LinearLayout card = Ui.card(this);
        card.addView(Ui.text(this, "白日夢", 24, Ui.INK, Typeface.BOLD));
        card.addView(Ui.text(this, "本地私用记账版，不做云端同步。", 15, Ui.MUTED, Typeface.NORMAL));
        card.addView(Ui.spacer(this, 16));
        Button notification = new Button(this);
        notification.setText("打开通知读取权限");
        notification.setAllCaps(false);
        notification.setTextColor(Color.WHITE);
        notification.setBackground(Ui.bg(this, Ui.ACCENT, 14));
        notification.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
        card.addView(notification, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 50)));
        card.addView(Ui.spacer(this, 12));

        Button accessibility = new Button(this);
        accessibility.setText("打开支付页面识别权限");
        accessibility.setAllCaps(false);
        accessibility.setTextColor(Color.WHITE);
        accessibility.setBackground(Ui.bg(this, Ui.ACCENT_DARK, 14));
        accessibility.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        card.addView(accessibility, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 50)));
        card.addView(Ui.spacer(this, 12));

        Switch autoSave = new Switch(this);
        autoSave.setText("高置信度自动保存");
        autoSave.setTextSize(17);
        autoSave.setTextColor(Ui.INK);
        autoSave.setChecked(AutoSaveManager.isAutoSaveEnabled(this));
        autoSave.setOnCheckedChangeListener((buttonView, isChecked) -> AutoSaveManager.setAutoSaveEnabled(this, isChecked));
        card.addView(autoSave, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 52)));
        card.addView(Ui.text(this, "开启后，仅在账户和分类都能识别时直接入账；不确定的账单仍会弹出确认页。", 14, Ui.MUTED, Typeface.NORMAL));
        card.addView(Ui.spacer(this, 12));

        Button logs = new Button(this);
        logs.setText("查看自动记账日志");
        logs.setAllCaps(false);
        logs.setTextColor(Ui.INK);
        logs.setBackground(Ui.strokeBg(this, Color.WHITE, 14, Ui.LINE));
        logs.setOnClickListener(v -> startActivity(new Intent(this, AutoLogActivity.class)));
        card.addView(logs, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 50)));
        root.addView(card);
        setContentView(scroll);
    }
}
