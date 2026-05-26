package com.personal.xiaomiledger;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

final class Ui {
    // ── 千与千寻 油屋 palette ──
    static final int INK           = Color.rgb(62, 39, 35);       // 浓茶色 - 主文字
    static final int PAPER_OPAQUE  = Color.rgb(254, 249, 230);    // 和纸色 - 不透明底色
    static final int PAPER         = Color.argb(150, 254, 249, 230); // 半透明和纸
    static final int PAPER_LIGHT   = Color.argb(120, 254, 249, 230); // 更透和纸
    static final int PANEL         = Color.rgb(255, 254, 250);     // 米白卡片
    static final int ACCENT        = Color.rgb(203, 67, 53);       // 油屋红 - 主色调
    static final int ACCENT_GOLD   = Color.rgb(212, 160, 23);      // 油屋金 - 点缀色
    static final int ACCENT_DARK   = Color.rgb(165, 42, 29);       // 深红
    static final int WARNING       = Color.rgb(229, 115, 115);     // 温柔珊瑚 - 支出
    static final int SUCCESS       = Color.rgb(102, 187, 106);     // 柔软绿 - 收入
    static final int TRANSFER      = Color.rgb(100, 181, 246);     // 汤屋水蓝 - 转账
    static final int MUTED         = Color.rgb(161, 136, 127);     // 暖灰棕 - 辅助文字
    static final int LINE          = Color.rgb(239, 235, 228);     // 和纸隔线
    static final int CHIP_BG       = Color.rgb(252, 246, 232);     // 暖米色底
    static final int INCOME_BG     = Color.rgb(232, 245, 233);     // 收入背景
    static final int EXPENSE_BG    = Color.rgb(255, 235, 235);     // 支出背景
    static final int TRANSFER_BG   = Color.rgb(227, 242, 253);     // 转账背景
    static final int ADJUST_BG     = Color.rgb(252, 246, 232);     // 调账背景
    static final int PROGRESS_TRACK= Color.rgb(239, 235, 228);     // 进度条轨道

    static final int ELEVATION_CARD = 2;
    static final int ELEVATION_FAB = 8;

    private Ui() {}

    static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    static TextView text(Context context, String value, float sp, int color, int style) {
        TextView textView = new TextView(context);
        textView.setText(value);
        textView.setTextSize(sp);
        textView.setTextColor(color);
        textView.setTypeface(Typeface.DEFAULT, style);
        textView.setIncludeFontPadding(true);
        return textView;
    }

    static GradientDrawable bg(Context context, int color, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(context, radiusDp));
        return drawable;
    }

    static GradientDrawable strokeBg(Context context, int color, int radiusDp, int strokeColor) {
        GradientDrawable drawable = bg(context, color, radiusDp);
        drawable.setStroke(dp(context, 1), strokeColor);
        return drawable;
    }

    static LinearLayout card(Context context) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(bg(context, PANEL, 20));
        card.setPadding(dp(context, 18), dp(context, 16), dp(context, 18), dp(context, 16));
        if (Build.VERSION.SDK_INT >= 21) {
            card.setElevation(dp(context, ELEVATION_CARD));
        }
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, dp(context, 12));
        card.setLayoutParams(params);
        return card;
    }

    static LinearLayout row(Context context) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        return row;
    }

    static TextView chip(Context context, String value, int textColor, int bgColor) {
        TextView chip = text(context, value, 13, textColor, Typeface.BOLD);
        chip.setGravity(android.view.Gravity.CENTER);
        chip.setBackground(bg(context, bgColor, 999));
        chip.setPadding(dp(context, 12), dp(context, 6), dp(context, 12), dp(context, 6));
        return chip;
    }

    static View spacer(Context context, int heightDp) {
        View view = new View(context);
        view.setLayoutParams(new ViewGroup.LayoutParams(1, dp(context, heightDp)));
        return view;
    }

    static void ripple(View view) {
        if (Build.VERSION.SDK_INT >= 21) {
            view.setBackground(new RippleDrawable(
                    ColorStateList.valueOf(Color.argb(20, 0, 0, 0)),
                    view.getBackground(),
                    null));
        }
    }

    static void rippleMasked(View view) {
        if (Build.VERSION.SDK_INT >= 21) {
            view.setBackground(new RippleDrawable(
                    ColorStateList.valueOf(Color.argb(20, 0, 0, 0)),
                    null,
                    null));
        }
    }

    static View line(Context context) {
        View line = new View(context);
        line.setBackgroundColor(LINE);
        line.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(context, 1)));
        return line;
    }

    static void applyBackground(Activity activity) {
        Drawable bg = activity.getDrawable(R.drawable.back_ground);
        if (bg != null) {
            activity.getWindow().setBackgroundDrawable(bg);
        }
        if (Build.VERSION.SDK_INT >= 21) {
            activity.getWindow().setStatusBarColor(Color.TRANSPARENT);
            if (Build.VERSION.SDK_INT >= 26) {
                activity.getWindow().setNavigationBarColor(Color.argb(160, 254, 249, 230));
            }
        }
    }
}
