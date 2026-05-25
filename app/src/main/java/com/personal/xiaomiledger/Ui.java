package com.personal.xiaomiledger;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

final class Ui {
    static final int INK = Color.rgb(20, 27, 35);
    static final int PAPER = Color.rgb(246, 248, 251);
    static final int PANEL = Color.WHITE;
    static final int ACCENT = Color.rgb(47, 128, 237);
    static final int ACCENT_DARK = Color.rgb(35, 93, 174);
    static final int WARNING = Color.rgb(235, 87, 87);
    static final int MUTED = Color.rgb(132, 143, 156);
    static final int LINE = Color.rgb(229, 234, 240);

    private Ui() {
    }

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
        card.setBackground(bg(context, PANEL, 16));
        card.setPadding(dp(context, 18), dp(context, 16), dp(context, 18), dp(context, 16));
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
        TextView chip = text(context, value, 14, textColor, Typeface.BOLD);
        chip.setGravity(android.view.Gravity.CENTER);
        chip.setBackground(bg(context, bgColor, 999));
        chip.setPadding(dp(context, 14), dp(context, 8), dp(context, 14), dp(context, 8));
        return chip;
    }

    static View spacer(Context context, int heightDp) {
        View view = new View(context);
        view.setLayoutParams(new ViewGroup.LayoutParams(1, dp(context, heightDp)));
        return view;
    }
}
