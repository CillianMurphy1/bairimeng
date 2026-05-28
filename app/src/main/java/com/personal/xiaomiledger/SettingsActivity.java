package com.personal.xiaomiledger;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import java.io.File;

public class SettingsActivity extends Activity {
    private static final int REQ_BG = 1;
    private static final int REQ_AVATAR = 2;

    private ImageView bgPreview;
    private TextView bgLabel;
    private ImageView avatarPreview;
    private TextView avatarLabel;
    private EditText displayNameInput;
    private EditText signatureInput;
    private EditText notifyNameInput;

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
        top.addView(Ui.text(this, "设置·关于", 25, Ui.INK, Typeface.BOLD),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        root.addView(top);

        // ── 外观卡片 ──
        root.addView(sectionTitle("外观"));

        LinearLayout lookCard = Ui.card(this);
        lookCard.addView(bgRow());
        lookCard.addView(Ui.spacer(this, 16));
        lookCard.addView(avatarRow());
        root.addView(lookCard);

        // ── 个人化卡片 ──
        root.addView(sectionTitle("个人化"));

        LinearLayout profileCard = Ui.card(this);
        profileCard.addView(editRow("昵称", displayNameInput = editField(PrefsManager.getDisplayName(this))));
        profileCard.addView(Ui.spacer(this, 10));
        profileCard.addView(editRow("签名", signatureInput = editField(PrefsManager.getSignature(this))));
        profileCard.addView(Ui.spacer(this, 10));
        profileCard.addView(editRow("提醒名称", notifyNameInput = editField(PrefsManager.getNotifyName(this))));
        root.addView(profileCard);

        // ── 权限卡片 ──
        root.addView(sectionTitle("权限"));

        LinearLayout permCard = Ui.card(this);
        Button notification = new Button(this);
        notification.setText("打开通知读取权限");
        notification.setAllCaps(false);
        notification.setTextColor(Color.WHITE);
        notification.setBackground(Ui.bg(this, Ui.ACCENT, 14));
        notification.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
        permCard.addView(notification, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 50)));
        permCard.addView(Ui.spacer(this, 12));

        Button accessibility = new Button(this);
        accessibility.setText("打开支付页面识别权限");
        accessibility.setAllCaps(false);
        accessibility.setTextColor(Color.WHITE);
        accessibility.setBackground(Ui.bg(this, Ui.ACCENT_DARK, 14));
        accessibility.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        permCard.addView(accessibility, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 50)));
        permCard.addView(Ui.spacer(this, 12));

        Switch autoSave = new Switch(this);
        autoSave.setText("高置信度自动保存");
        autoSave.setTextSize(17);
        autoSave.setTextColor(Ui.INK);
        autoSave.setChecked(AutoSaveManager.isAutoSaveEnabled(this));
        autoSave.setOnCheckedChangeListener((buttonView, isChecked) -> AutoSaveManager.setAutoSaveEnabled(this, isChecked));
        permCard.addView(autoSave, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 52)));
        permCard.addView(Ui.text(this, "开启后，仅在账户和分类都能识别时直接入账；不确定的账单仍会弹出确认页。", 14, Ui.MUTED, Typeface.NORMAL));
        permCard.addView(Ui.spacer(this, 12));

        Button logs = new Button(this);
        logs.setText("查看自动记账日志");
        logs.setAllCaps(false);
        logs.setTextColor(Ui.INK);
        logs.setBackground(Ui.strokeBg(this, Color.WHITE, 14, Ui.LINE));
        logs.setOnClickListener(v -> startActivity(new Intent(this, AutoLogActivity.class)));
        permCard.addView(logs, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 50)));
        root.addView(permCard);

        setContentView(scroll);
    }

    // ── section title ──
    private TextView sectionTitle(String title) {
        TextView tv = Ui.text(this, title, 14, Ui.MUTED, Typeface.BOLD);
        tv.setPadding(Ui.dp(this, 6), Ui.dp(this, 20), 0, Ui.dp(this, 10));
        return tv;
    }

    // ── background row ──
    private LinearLayout bgRow() {
        LinearLayout row = Ui.row(this);
        row.addView(Ui.text(this, "背景图", 16, Ui.INK, Typeface.BOLD),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        bgPreview = new ImageView(this);
        updateBgPreview();
        bgPreview.setScaleType(ImageView.ScaleType.CENTER_CROP);
        bgPreview.setClipToOutline(true);
        bgPreview.setOutlineProvider(new android.view.ViewOutlineProvider() {
            @Override
            public void getOutline(android.view.View v, android.graphics.Outline outline) {
                outline.setRoundRect(0, 0, v.getWidth(), v.getHeight(), Ui.dp(SettingsActivity.this, 8));
            }
        });
        int ps = Ui.dp(this, 44);
        row.addView(bgPreview, new LinearLayout.LayoutParams(ps, ps));

        TextView sel = Ui.text(this, "选择", 14, Ui.ACCENT, Typeface.BOLD);
        sel.setGravity(Gravity.CENTER);
        sel.setPadding(Ui.dp(this, 10), 0, Ui.dp(this, 4), 0);
        sel.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            startActivityForResult(intent, REQ_BG);
        });
        row.addView(sel);

        TextView resetBg = Ui.text(this, "默认", 14, Ui.MUTED, Typeface.NORMAL);
        resetBg.setGravity(Gravity.CENTER);
        resetBg.setPadding(Ui.dp(this, 4), 0, 0, 0);
        resetBg.setOnClickListener(v -> {
            PrefsManager.clearBackground(this);
            updateBgPreview();
        });
        row.addView(resetBg);
        return row;
    }

    // ── avatar row ──
    private LinearLayout avatarRow() {
        LinearLayout row = Ui.row(this);
        row.addView(Ui.text(this, "头像", 16, Ui.INK, Typeface.BOLD),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        avatarPreview = new ImageView(this);
        updateAvatarPreview();
        avatarPreview.setScaleType(ImageView.ScaleType.CENTER_CROP);
        avatarPreview.setClipToOutline(true);
        avatarPreview.setOutlineProvider(new android.view.ViewOutlineProvider() {
            @Override
            public void getOutline(android.view.View v, android.graphics.Outline outline) {
                outline.setOval(0, 0, v.getWidth(), v.getHeight());
            }
        });
        int as = Ui.dp(this, 44);
        row.addView(avatarPreview, new LinearLayout.LayoutParams(as, as));

        TextView sel = Ui.text(this, "选择", 14, Ui.ACCENT, Typeface.BOLD);
        sel.setGravity(Gravity.CENTER);
        sel.setPadding(Ui.dp(this, 10), 0, Ui.dp(this, 4), 0);
        sel.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            startActivityForResult(intent, REQ_AVATAR);
        });
        row.addView(sel);

        TextView resetAv = Ui.text(this, "默认", 14, Ui.MUTED, Typeface.NORMAL);
        resetAv.setGravity(Gravity.CENTER);
        resetAv.setPadding(Ui.dp(this, 4), 0, 0, 0);
        resetAv.setOnClickListener(v -> {
            PrefsManager.clearAvatar(this);
            updateAvatarPreview();
        });
        row.addView(resetAv);
        return row;
    }

    private void updateBgPreview() {
        Drawable d = PrefsManager.hasCustomBackground(this)
                ? PrefsManager.loadBackgroundDrawable(this)
                : getDrawable(android.R.drawable.ic_menu_gallery);
        bgPreview.setImageDrawable(d);
    }

    private void updateAvatarPreview() {
        Drawable d = PrefsManager.loadAvatarDrawable(this, 44);
        if (d != null) {
            avatarPreview.setImageDrawable(d);
            avatarPreview.setBackground(null);
        } else {
            avatarPreview.setImageDrawable(null);
            avatarPreview.setBackground(Ui.bg(this, Ui.ACCENT_GOLD, 999));
        }
    }

    // ── edit row ──
    private LinearLayout editRow(String label, EditText input) {
        LinearLayout row = Ui.row(this);
        TextView lbl = Ui.text(this, label, 16, Ui.INK, Typeface.BOLD);
        lbl.setMinWidth(Ui.dp(this, 80));
        lbl.setPadding(0, 0, Ui.dp(this, 10), 0);
        row.addView(lbl);
        row.addView(input, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        return row;
    }

    private EditText editField(String initial) {
        EditText et = new EditText(this);
        et.setText(initial);
        et.setTextSize(15);
        et.setTextColor(Ui.INK);
        et.setHintTextColor(Ui.MUTED);
        et.setSingleLine(true);
        et.setBackgroundColor(Color.TRANSPARENT);
        et.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 8));
        et.setInputType(InputType.TYPE_CLASS_TEXT);
        return et;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        if (requestCode == REQ_BG) {
            String path = PrefsManager.saveImageToPrivate(this, uri, "custom_bg.jpg");
            if (path != null) {
                PrefsManager.setBackgroundPath(this, path);
                updateBgPreview();
            }
        } else if (requestCode == REQ_AVATAR) {
            String path = PrefsManager.saveImageToPrivate(this, uri, "custom_avatar.jpg");
            if (path != null) {
                PrefsManager.setAvatarPath(this, path);
                updateAvatarPreview();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        String name = displayNameInput.getText().toString().trim();
        String sig = signatureInput.getText().toString().trim();
        String notify = notifyNameInput.getText().toString().trim();
        if (!name.isEmpty()) PrefsManager.setDisplayName(this, name);
        if (!sig.isEmpty()) PrefsManager.setSignature(this, sig);
        if (!notify.isEmpty()) PrefsManager.setNotifyName(this, notify);
    }
}
