package com.personal.xiaomiledger;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.List;

public class StatsActivity extends Activity {
    private TransactionStore store;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Ui.applyBackground(this);
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
        TextView back = Ui.text(this, "‹", 34, Ui.INK, Typeface.NORMAL);
        back.setGravity(Gravity.CENTER);
        back.setOnClickListener(v -> finish());
        top.addView(back, new LinearLayout.LayoutParams(Ui.dp(this, 40), Ui.dp(this, 40)));
        TextView title = Ui.text(this, "收支统计", 22, Ui.INK, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        top.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        top.addView(new View(this), new LinearLayout.LayoutParams(Ui.dp(this, 40), Ui.dp(this, 40)));
        root.addView(top);
        root.addView(Ui.spacer(this, 16));

        long[] range = TransactionStore.currentMonthRange();
        long expense = store.sumBetween("expense", range[0], range[1]);
        long income = store.sumBetween("income", range[0], range[1]);

        LinearLayout summary = Ui.card(this);
        TextView summaryTitle = Ui.text(this, "收支总览", 20, Ui.INK, Typeface.BOLD);
        summaryTitle.setGravity(Gravity.CENTER);
        summary.addView(summaryTitle);
        summary.addView(Ui.spacer(this, 18));
        LinearLayout metrics = Ui.row(this);
        metrics.addView(metric("支出", PaymentParser.formatMoney(expense), Ui.WARNING));
        metrics.addView(metric("结余", TransactionStore.formatMoneySigned(income - expense), Ui.INK));
        metrics.addView(metric("收入", PaymentParser.formatMoney(income), Ui.SUCCESS));
        summary.addView(metrics);
        root.addView(summary);

        LinearLayout categories = Ui.card(this);
        categories.addView(Ui.text(this, "支出分类", 20, Ui.INK, Typeface.BOLD));
        categories.addView(Ui.spacer(this, 10));
        List<String> names = store.categoryNames("expense");
        boolean hasRows = false;
        for (String name : names) {
            long value = store.categoryExpenseBetween(name, range[0], range[1]);
            if (value > 0) {
                hasRows = true;
                categories.addView(categoryRow(name, value, expense));
            }
        }
        if (!hasRows) {
            TextView empty = Ui.text(this, "本月还没有支出数据", 15, Ui.MUTED, Typeface.NORMAL);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, Ui.dp(this, 28), 0, Ui.dp(this, 22));
            categories.addView(empty);
        }
        root.addView(categories);

        LinearLayout asset = Ui.card(this);
        asset.addView(Ui.text(this, "资产快照", 20, Ui.INK, Typeface.BOLD));
        asset.addView(Ui.spacer(this, 12));
        asset.addView(Ui.text(this, "总资产  " + TransactionStore.formatMoneySigned(store.totalByKind("asset")), 18, Ui.INK, Typeface.BOLD));
        asset.addView(Ui.spacer(this, 6));
        asset.addView(Ui.text(this, "总负债  " + TransactionStore.formatMoneySigned(store.totalByKind("liability")), 15, Ui.MUTED, Typeface.NORMAL));
        root.addView(asset);
        setContentView(scroll);
    }

    private TextView metric(String label, String value, int color) {
        TextView view = Ui.text(this, label + "\n" + value, 18, color, Typeface.BOLD);
        view.setGravity(Gravity.CENTER);
        view.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        return view;
    }

    private LinearLayout categoryRow(String name, long value, long total) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(0, Ui.dp(this, 9), 0, Ui.dp(this, 9));

        LinearLayout row = Ui.row(this);
        row.addView(Ui.text(this, name, 16, Ui.INK, Typeface.BOLD), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView amount = Ui.text(this, PaymentParser.formatMoney(value), 16, Ui.WARNING, Typeface.BOLD);
        amount.setGravity(Gravity.END);
        row.addView(amount);
        box.addView(row);
        box.addView(Ui.spacer(this, 6));

        int percent = total <= 0 ? 1 : Math.max(1, (int) Math.min(100, value * 100 / total));
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setBackground(Ui.bg(this, Ui.PROGRESS_TRACK, 999));
        View filled = new View(this);
        filled.setBackground(Ui.bg(this, Ui.WARNING, 999));
        bar.addView(filled, new LinearLayout.LayoutParams(0, Ui.dp(this, 6), percent));
        bar.addView(new View(this), new LinearLayout.LayoutParams(0, Ui.dp(this, 6), 100 - percent));
        box.addView(bar);
        return box;
    }
}
