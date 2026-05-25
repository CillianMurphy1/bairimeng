package com.personal.xiaomiledger;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.List;

public class AutoLogActivity extends Activity {
    private TransactionStore store;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.WHITE);
        store = new TransactionStore(this);
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
        top.addView(Ui.text(this, "自动记账日志", 25, Ui.INK, Typeface.BOLD), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        root.addView(top);
        root.addView(Ui.spacer(this, 16));

        LinearLayout card = Ui.card(this);
        card.addView(Ui.text(this, "最近识别", 20, Ui.INK, Typeface.BOLD));
        card.addView(Ui.spacer(this, 10));
        List<AutoRecordLog> logs = store.autoLogs(80);
        if (logs.isEmpty()) {
            TextView empty = Ui.text(this, "暂时没有自动识别记录。", 16, Ui.MUTED, Typeface.NORMAL);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, Ui.dp(this, 28), 0, Ui.dp(this, 22));
            card.addView(empty);
        } else {
            for (AutoRecordLog log : logs) {
                card.addView(logRow(log));
            }
        }
        root.addView(card);
        setContentView(scroll);
    }

    private LinearLayout logRow(AutoRecordLog log) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 10));

        LinearLayout line = Ui.row(this);
        TextView status = Ui.text(this, statusLabel(log.status), 15, statusColor(log.status), Typeface.BOLD);
        status.setGravity(Gravity.CENTER);
        status.setBackground(Ui.bg(this, statusBg(log.status), 999));
        line.addView(status, new LinearLayout.LayoutParams(Ui.dp(this, 74), Ui.dp(this, 34)));
        TextView info = Ui.text(this, safe(log.sourceApp) + "  " + TransactionStore.formatDateTime(log.createdAt), 15, Ui.MUTED, Typeface.BOLD);
        LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        infoLp.setMargins(Ui.dp(this, 12), 0, 0, 0);
        line.addView(info, infoLp);
        TextView amount = Ui.text(this, log.amountCents > 0 ? PaymentParser.formatMoney(log.amountCents) : "", 16, Ui.INK, Typeface.BOLD);
        amount.setGravity(Gravity.END);
        line.addView(amount);
        row.addView(line);
        row.addView(Ui.spacer(this, 5));
        row.addView(Ui.text(this, safe(log.message), 15, Ui.INK, Typeface.NORMAL));
        if (log.rawText != null && log.rawText.length() > 0) {
            row.addView(Ui.text(this, truncate(log.rawText), 13, Ui.MUTED, Typeface.NORMAL));
        }
        return row;
    }

    private String statusLabel(String status) {
        if ("duplicate".equals(status)) return "重复";
        if ("saved".equals(status)) return "保存";
        return "识别";
    }

    private int statusColor(String status) {
        if ("duplicate".equals(status)) return Color.rgb(92, 112, 140);
        if ("saved".equals(status)) return Color.rgb(44, 188, 128);
        return Ui.ACCENT;
    }

    private int statusBg(String status) {
        if ("duplicate".equals(status)) return Color.rgb(238, 241, 245);
        if ("saved".equals(status)) return Color.rgb(230, 248, 240);
        return Color.rgb(232, 245, 255);
    }

    private String safe(String value) {
        return value == null || value.length() == 0 ? "未填写" : value;
    }

    private String truncate(String value) {
        return value.length() <= 90 ? value : value.substring(0, 90) + "...";
    }
}
