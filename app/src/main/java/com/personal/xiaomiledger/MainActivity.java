package com.personal.xiaomiledger;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private FrameLayout screen;
    private LinearLayout content;
    private TransactionStore store;
    private Calendar selectedMonth = Calendar.getInstance();
    private TextView monthText;
    private float drawerSwipeStartX;
    private float drawerSwipeStartY;
    private boolean drawerSwipeWatching;
    private boolean drawerSwipeOpened;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.WHITE);
        store = new TransactionStore(this);
        buildUi();
        requestPostNotifications();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        handleDrawerSwipe(event);
        return super.dispatchTouchEvent(event);
    }

    private void handleDrawerSwipe(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                drawerSwipeStartX = event.getX();
                drawerSwipeStartY = event.getY();
                drawerSwipeWatching = drawerSwipeStartX <= Ui.dp(this, 34);
                drawerSwipeOpened = false;
                break;
            case MotionEvent.ACTION_MOVE:
                if (!drawerSwipeWatching || drawerSwipeOpened) {
                    return;
                }
                float dx = event.getX() - drawerSwipeStartX;
                float dy = Math.abs(event.getY() - drawerSwipeStartY);
                if (dx > Ui.dp(this, 72) && dx > dy * 1.4f) {
                    drawerSwipeOpened = true;
                    drawerSwipeWatching = false;
                    showDrawer();
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                drawerSwipeWatching = false;
                drawerSwipeOpened = false;
                break;
            default:
                break;
        }
    }

    private void buildUi() {
        screen = new FrameLayout(this);
        screen.setBackgroundColor(Ui.PAPER);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        screen.addView(page, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        page.addView(topBar());

        ScrollView scroll = new ScrollView(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(Ui.dp(this, 14), Ui.dp(this, 14), Ui.dp(this, 14), Ui.dp(this, 110));
        scroll.addView(content);
        page.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        TextView fab = Ui.text(this, "+", 40, Color.WHITE, Typeface.NORMAL);
        fab.setGravity(Gravity.CENTER);
        fab.setBackground(Ui.bg(this, Ui.ACCENT, 999));
        fab.setOnClickListener(v -> startActivity(new Intent(this, AddBillActivity.class)));
        FrameLayout.LayoutParams fabLp = new FrameLayout.LayoutParams(Ui.dp(this, 74), Ui.dp(this, 74), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        fabLp.setMargins(0, 0, 0, Ui.dp(this, 34));
        screen.addView(fab, fabLp);

        setContentView(screen);
    }

    private LinearLayout topBar() {
        LinearLayout bar = Ui.row(this);
        bar.setBackgroundColor(Color.WHITE);
        bar.setPadding(Ui.dp(this, 18), Ui.dp(this, 16), Ui.dp(this, 18), Ui.dp(this, 12));
        bar.setMinimumHeight(Ui.dp(this, 82));

        TextView menu = Ui.text(this, "☰", 34, Color.BLACK, Typeface.NORMAL);
        menu.setGravity(Gravity.CENTER);
        menu.setOnClickListener(v -> showDrawer());
        bar.addView(menu, new LinearLayout.LayoutParams(Ui.dp(this, 54), Ui.dp(this, 54)));

        monthText = Ui.text(this, monthTitle() + "⌄", 22, Color.BLACK, Typeface.BOLD);
        monthText.setGravity(Gravity.CENTER);
        monthText.setOnClickListener(v -> showMonthPicker());
        bar.addView(monthText, new LinearLayout.LayoutParams(0, Ui.dp(this, 54), 1));

        TextView calendar = icon("▣");
        calendar.setOnClickListener(v -> showMonthPicker());
        bar.addView(calendar);

        TextView chart = icon("▥");
        chart.setOnClickListener(v -> startActivity(new Intent(this, StatsActivity.class)));
        bar.addView(chart);

        TextView asset = icon("▢");
        asset.setOnClickListener(v -> startActivity(new Intent(this, AdjustBalanceActivity.class)));
        bar.addView(asset);
        return bar;
    }

    private TextView icon(String value) {
        TextView icon = Ui.text(this, value, 28, Color.BLACK, Typeface.BOLD);
        icon.setGravity(Gravity.CENTER);
        return icon;
    }

    private void refresh() {
        content.removeAllViews();
        addAssetCard();
        addMonthOverviewCard();
        addQuickActionsCard();
        addAccountsCard();
        addBillsCard();
        addAutoRecordCard();
    }

    private void addAssetCard() {
        long totalAssets = store.totalByKind("asset");
        long totalLiabilities = Math.abs(store.totalByKind("liability"));
        long netAssets = totalAssets - totalLiabilities;
        LinearLayout card = Ui.card(this);
        card.setPadding(Ui.dp(this, 20), Ui.dp(this, 24), Ui.dp(this, 20), Ui.dp(this, 24));
        LinearLayout head = Ui.row(this);
        head.addView(Ui.text(this, "白日夢", 18, Ui.MUTED, Typeface.BOLD), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView adjust = Ui.text(this, "平账", 14, Ui.ACCENT, Typeface.BOLD);
        adjust.setGravity(Gravity.CENTER);
        adjust.setBackground(Ui.bg(this, Color.rgb(235, 243, 255), 999));
        adjust.setPadding(Ui.dp(this, 14), Ui.dp(this, 7), Ui.dp(this, 14), Ui.dp(this, 7));
        adjust.setOnClickListener(v -> startActivity(new Intent(this, AdjustBalanceActivity.class)));
        head.addView(adjust);
        card.addView(head);
        card.addView(Ui.spacer(this, 12));
        card.addView(center("净资产", 16, Ui.MUTED, Typeface.BOLD));
        card.addView(center(TransactionStore.formatMoneySigned(netAssets), 40, Color.BLACK, Typeface.BOLD));
        LinearLayout row = Ui.row(this);
        row.setGravity(Gravity.CENTER);
        row.addView(assetMetric("总资产", TransactionStore.formatMoneySigned(totalAssets)));
        row.addView(assetMetric("总负债", totalLiabilities == 0 ? "无" : TransactionStore.formatMoneySigned(totalLiabilities)));
        card.addView(row);
        content.addView(card);
    }

    private void addMonthOverviewCard() {
        long[] range = TransactionStore.currentMonthRange();
        long income = store.sumBetween("income", range[0], range[1]);
        long expense = store.sumBetween("expense", range[0], range[1]);
        LinearLayout card = Ui.card(this);
        LinearLayout header = Ui.row(this);
        header.addView(Ui.text(this, "本月收支", 21, Color.BLACK, Typeface.BOLD), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView more = Ui.text(this, "统计", 15, Ui.ACCENT, Typeface.BOLD);
        more.setOnClickListener(v -> startActivity(new Intent(this, StatsActivity.class)));
        header.addView(more);
        card.addView(header);
        card.addView(Ui.spacer(this, 14));
        LinearLayout row = Ui.row(this);
        row.addView(monthMetric("收入", PaymentParser.formatMoney(income), Color.rgb(44, 188, 128), Color.rgb(232, 248, 241)));
        row.addView(monthMetric("支出", PaymentParser.formatMoney(expense), Ui.WARNING, Color.rgb(255, 239, 240)));
        row.addView(monthMetric("结余", TransactionStore.formatMoneySigned(income - expense), Ui.INK, Color.rgb(240, 244, 248)));
        card.addView(row);
        content.addView(card);
    }

    private TextView monthMetric(String label, String value, int color, int bg) {
        TextView view = Ui.text(this, label + "\n" + value, 15, color, Typeface.BOLD);
        view.setGravity(Gravity.CENTER);
        view.setBackground(Ui.bg(this, bg, 14));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, Ui.dp(this, 74), 1);
        lp.setMargins(Ui.dp(this, 4), 0, Ui.dp(this, 4), 0);
        view.setLayoutParams(lp);
        return view;
    }

    private void addQuickActionsCard() {
        LinearLayout card = Ui.card(this);
        card.setPadding(Ui.dp(this, 14), Ui.dp(this, 12), Ui.dp(this, 14), Ui.dp(this, 12));
        LinearLayout row = Ui.row(this);
        row.addView(quickAction("支出", Ui.WARNING, v -> startActivity(addBillIntent("expense"))));
        row.addView(quickAction("收入", Color.rgb(44, 188, 128), v -> startActivity(addBillIntent("income"))));
        row.addView(quickAction("转账", Ui.ACCENT, v -> startActivity(addBillIntent("transfer"))));
        row.addView(quickAction("预算", Ui.INK, v -> startActivity(new Intent(this, BudgetActivity.class))));
        card.addView(row);
        content.addView(card);
    }

    private Intent addBillIntent(String mode) {
        Intent intent = new Intent(this, AddBillActivity.class);
        intent.putExtra("mode", mode);
        return intent;
    }

    private TextView quickAction(String label, int color, View.OnClickListener listener) {
        TextView button = Ui.text(this, label, 16, color, Typeface.BOLD);
        button.setGravity(Gravity.CENTER);
        button.setBackground(Ui.bg(this, Color.rgb(248, 250, 252), 14));
        button.setOnClickListener(listener);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, Ui.dp(this, 50), 1);
        lp.setMargins(Ui.dp(this, 4), 0, Ui.dp(this, 4), 0);
        button.setLayoutParams(lp);
        return button;
    }

    private void addBorrowCard() {
        LinearLayout card = Ui.card(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setPadding(Ui.dp(this, 18), Ui.dp(this, 16), Ui.dp(this, 18), Ui.dp(this, 16));
        card.addView(borrowMetric("⇩", "总借入", "0.00"));
        View line = new View(this);
        line.setBackgroundColor(Ui.LINE);
        card.addView(line, new LinearLayout.LayoutParams(Ui.dp(this, 1), Ui.dp(this, 58)));
        card.addView(borrowMetric("⇧", "总借出", "0.00"));
        content.addView(card);
    }

    private TextView center(String text, int size, int color, int style) {
        TextView view = Ui.text(this, text, size, color, style);
        view.setGravity(Gravity.CENTER);
        return view;
    }

    private TextView assetMetric(String label, String value) {
        TextView view = Ui.text(this, label + "\n" + value, 17, Color.BLACK, Typeface.BOLD);
        view.setGravity(Gravity.CENTER);
        view.setPadding(0, Ui.dp(this, 22), 0, 0);
        view.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        return view;
    }

    private LinearLayout borrowMetric(String icon, String label, String value) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.HORIZONTAL);
        box.setGravity(Gravity.CENTER);
        TextView iconView = Ui.text(this, icon, 26, Ui.MUTED, Typeface.BOLD);
        iconView.setGravity(Gravity.CENTER);
        box.addView(iconView, new LinearLayout.LayoutParams(Ui.dp(this, 54), Ui.dp(this, 54)));
        TextView text = Ui.text(this, label + "\n" + value, 18, Color.BLACK, Typeface.BOLD);
        box.addView(text);
        box.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        return box;
    }

    private void addAccountsCard() {
        LinearLayout card = Ui.card(this);
        LinearLayout header = Ui.row(this);
        header.addView(Ui.text(this, "资金", 22, Color.BLACK, Typeface.BOLD), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView add = Ui.text(this, "+", 26, Ui.ACCENT, Typeface.BOLD);
        add.setGravity(Gravity.CENTER);
        add.setOnClickListener(v -> startActivity(new Intent(this, AddAccountActivity.class)));
        header.addView(add, new LinearLayout.LayoutParams(Ui.dp(this, 42), Ui.dp(this, 42)));
        TextView total = Ui.text(this, TransactionStore.formatMoneySigned(store.totalByKind("asset")) + "⌄", 22, Color.BLACK, Typeface.BOLD);
        total.setGravity(Gravity.END);
        header.addView(total);
        card.addView(header);
        card.addView(line());

        List<Account> accounts = store.accounts();
        for (int i = 0; i < accounts.size(); i++) {
            card.addView(accountRow(accounts.get(i)));
            if (i < accounts.size() - 1) {
                card.addView(line());
            }
        }
        content.addView(card);
    }

    private LinearLayout accountRow(Account account) {
        LinearLayout row = Ui.row(this);
        row.setPadding(0, Ui.dp(this, 13), 0, Ui.dp(this, 13));
        row.setOnClickListener(v -> startActivity(AdjustBalanceActivity.intentForAccount(this, account.name)));
        TextView icon = Ui.text(this, accountInitial(account.name), 19, Color.WHITE, Typeface.BOLD);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(Ui.bg(this, accountColor(account.name), 999));
        row.addView(icon, new LinearLayout.LayoutParams(Ui.dp(this, 48), Ui.dp(this, 48)));
        TextView name = Ui.text(this, account.name, 22, Color.BLACK, Typeface.NORMAL);
        LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        nameLp.setMargins(Ui.dp(this, 16), 0, 0, 0);
        row.addView(name, nameLp);
        TextView balance = Ui.text(this, TransactionStore.formatMoneySigned(account.balanceCents), 21, Color.BLACK, Typeface.NORMAL);
        row.addView(balance);
        return row;
    }

    private void addBillsCard() {
        LinearLayout card = Ui.card(this);
        LinearLayout header = Ui.row(this);
        header.addView(Ui.text(this, "最近账单", 22, Color.BLACK, Typeface.BOLD), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView more = Ui.text(this, "搜索", 15, Ui.ACCENT, Typeface.BOLD);
        more.setOnClickListener(v -> startActivity(new Intent(this, SearchActivity.class)));
        header.addView(more);
        card.addView(header);
        List<Transaction> transactions = store.recent(6);
        if (transactions.isEmpty()) {
            TextView empty = center("没有数据", 18, Ui.MUTED, Typeface.NORMAL);
            empty.setPadding(0, Ui.dp(this, 28), 0, Ui.dp(this, 20));
            card.addView(empty);
        } else {
            for (Transaction tx : transactions) {
                card.addView(line());
                card.addView(billRow(tx));
            }
        }
        content.addView(card);
    }

    private LinearLayout billRow(Transaction tx) {
        LinearLayout row = Ui.row(this);
        row.setPadding(0, Ui.dp(this, 12), 0, Ui.dp(this, 12));
        row.setOnClickListener(v -> openTransaction(tx));
        TextView icon = Ui.text(this, billIcon(tx), 16, billColor(tx), Typeface.BOLD);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(Ui.bg(this, billBg(tx), 999));
        row.addView(icon, new LinearLayout.LayoutParams(Ui.dp(this, 44), Ui.dp(this, 44)));
        TextView info = Ui.text(this, billTitle(tx) + "\n" + safe(tx.accountName), 16, Color.BLACK, Typeface.NORMAL);
        LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        infoLp.setMargins(Ui.dp(this, 14), 0, Ui.dp(this, 8), 0);
        row.addView(info, infoLp);
        TextView amount = Ui.text(this, billAmount(tx), 19, billColor(tx), Typeface.BOLD);
        row.addView(amount);
        return row;
    }

    private void openTransaction(Transaction tx) {
        if ("income".equals(tx.type) || "expense".equals(tx.type) || "refund".equals(tx.type)) {
            startActivity(EditTransactionActivity.intentForTransaction(this, tx.id));
        } else if ("adjustment".equals(tx.type)) {
            startActivity(AdjustBalanceActivity.intentForAccount(this, tx.accountName));
        } else {
            startActivity(new Intent(this, SearchActivity.class));
        }
    }

    private void addAutoRecordCard() {
        LinearLayout card = Ui.card(this);
        card.addView(Ui.text(this, "自动记账", 20, Color.BLACK, Typeface.BOLD));
        card.addView(Ui.text(this, "微信、支付宝支付通知会生成待确认账单；这里可以查看识别记录。", 14, Ui.MUTED, Typeface.NORMAL));
        card.setOnClickListener(v -> startActivity(new Intent(this, AutoLogActivity.class)));
        content.addView(card);
    }

    private void showDrawer() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.HORIZONTAL);

        ScrollView drawerScroll = new ScrollView(this);
        drawerScroll.setBackgroundColor(Color.WHITE);
        drawerScroll.setFillViewport(true);
        root.addView(drawerScroll, new LinearLayout.LayoutParams((int) (getResources().getDisplayMetrics().widthPixels * 0.70f), ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout drawer = new LinearLayout(this);
        drawer.setOrientation(LinearLayout.VERTICAL);
        drawer.setBackgroundColor(Color.WHITE);
        drawer.setPadding(Ui.dp(this, 28), Ui.dp(this, 56), Ui.dp(this, 20), Ui.dp(this, 20));
        drawerScroll.addView(drawer, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView avatar = Ui.text(this, "夢", 34, Color.WHITE, Typeface.BOLD);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackground(Ui.bg(this, Color.rgb(246, 210, 74), 999));
        drawer.addView(avatar, new LinearLayout.LayoutParams(Ui.dp(this, 88), Ui.dp(this, 88)));
        drawer.addView(Ui.spacer(this, 18));
        drawer.addView(Ui.text(this, "白日夢", 24, Color.BLACK, Typeface.BOLD));
        drawer.addView(Ui.text(this, "已使用 1 天", 16, Ui.MUTED, Typeface.NORMAL));
        drawer.addView(Ui.spacer(this, 42));
        drawer.addView(drawerItem("▣", "我的账本", "日常账本", v -> dialog.dismiss()));
        drawer.addView(drawerItem("¥", "报销管理", "", v -> Toast.makeText(this, "报销管理下一版继续补", Toast.LENGTH_SHORT).show()));
        drawer.addView(drawerItem("⌕", "搜索账单", "", v -> { dialog.dismiss(); startActivity(new Intent(this, SearchActivity.class)); }));
        drawer.addView(drawerItem("▥", "统计分析", "", v -> { dialog.dismiss(); startActivity(new Intent(this, StatsActivity.class)); }));
        drawer.addView(drawerItem("▤", "预算管理", "", v -> { dialog.dismiss(); startActivity(new Intent(this, BudgetActivity.class)); }));
        drawer.addView(drawerItem("▦", "分类管理", "", v -> { dialog.dismiss(); startActivity(new Intent(this, CategoryManageActivity.class)); }));
        drawer.addView(drawerItem("◎", "自动记账日志", "", v -> { dialog.dismiss(); startActivity(new Intent(this, AutoLogActivity.class)); }));
        drawer.addView(drawerItem("⇄", "分期·周期", "", v -> Toast.makeText(this, "周期账单下一版继续补", Toast.LENGTH_SHORT).show()));
        drawer.addView(drawerItem("●", "存钱计划", "", v -> Toast.makeText(this, "存钱计划下一版继续补", Toast.LENGTH_SHORT).show()));
        drawer.addView(drawerItem("⚙", "设置·关于", "", v -> { dialog.dismiss(); startActivity(new Intent(this, SettingsActivity.class)); }));

        View shade = new View(this);
        shade.setBackgroundColor(Color.argb(150, 0, 0, 0));
        shade.setOnClickListener(v -> dialog.dismiss());
        root.addView(shade, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));

        dialog.setContentView(root);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
        dialog.show();
        Window shown = dialog.getWindow();
        if (shown != null) {
            shown.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
    }

    private LinearLayout drawerItem(String icon, String title, String tail, View.OnClickListener listener) {
        LinearLayout row = Ui.row(this);
        row.setPadding(0, Ui.dp(this, 12), 0, Ui.dp(this, 12));
        row.setOnClickListener(listener);
        TextView i = Ui.text(this, icon, 28, Ui.MUTED, Typeface.BOLD);
        i.setGravity(Gravity.CENTER);
        row.addView(i, new LinearLayout.LayoutParams(Ui.dp(this, 54), Ui.dp(this, 54)));
        TextView t = Ui.text(this, title, 22, Color.BLACK, Typeface.NORMAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        lp.setMargins(Ui.dp(this, 16), 0, 0, 0);
        row.addView(t, lp);
        if (tail.length() > 0) {
            row.addView(Ui.text(this, tail, 16, Ui.MUTED, Typeface.NORMAL));
        }
        return row;
    }

    private void showMonthPicker() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout card = Ui.card(this);
        card.setPadding(0, Ui.dp(this, 16), 0, Ui.dp(this, 16));
        card.addView(settingRow("显示方式", "按月 ›"));
        card.addView(line());
        card.addView(settingRow("月份起始日", "01 ›"));
        card.addView(line());
        LinearLayout body = Ui.row(this);
        LinearLayout years = new LinearLayout(this);
        years.setOrientation(LinearLayout.VERTICAL);
        for (int y = 2029; y >= 2023; y--) {
            TextView year = center(y + "年", 22, y == selectedMonth.get(Calendar.YEAR) ? Color.WHITE : Color.BLACK, Typeface.NORMAL);
            year.setBackground(Ui.bg(this, y == selectedMonth.get(Calendar.YEAR) ? Ui.ACCENT : Color.WHITE, 999));
            final int fy = y;
            year.setOnClickListener(v -> {
                selectedMonth.set(Calendar.YEAR, fy);
                if (monthText != null) {
                    monthText.setText(monthTitle() + "⌄");
                }
                dialog.dismiss();
                refresh();
            });
            years.addView(year, new LinearLayout.LayoutParams(Ui.dp(this, 150), Ui.dp(this, 54)));
        }
        body.addView(years);
        GridLayout months = new GridLayout(this);
        months.setColumnCount(2);
        for (int m = 1; m <= 12; m++) {
            TextView month = center(m + "月", 22, m == selectedMonth.get(Calendar.MONTH) + 1 ? Ui.ACCENT : Color.BLACK, Typeface.BOLD);
            final int fm = m;
            month.setOnClickListener(v -> {
                selectedMonth.set(Calendar.MONTH, fm - 1);
                if (monthText != null) {
                    monthText.setText(monthTitle() + "⌄");
                }
                dialog.dismiss();
                refresh();
            });
            months.addView(month, new ViewGroup.LayoutParams(Ui.dp(this, 120), Ui.dp(this, 58)));
        }
        body.addView(months);
        card.addView(body);
        dialog.setContentView(card);
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout((int) (getResources().getDisplayMetrics().widthPixels * 0.84f), ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private LinearLayout settingRow(String left, String right) {
        LinearLayout row = Ui.row(this);
        row.setPadding(Ui.dp(this, 18), 0, Ui.dp(this, 18), 0);
        row.addView(Ui.text(this, left, 20, Color.BLACK, Typeface.NORMAL), new LinearLayout.LayoutParams(0, Ui.dp(this, 58), 1));
        TextView r = Ui.text(this, right, 20, Ui.MUTED, Typeface.BOLD);
        r.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
        row.addView(r);
        return row;
    }

    private String monthTitle() {
        return new SimpleDateFormat("yyyy-MM", Locale.CHINA).format(selectedMonth.getTime());
    }

    private View line() {
        View line = new View(this);
        line.setBackgroundColor(Ui.LINE);
        line.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 1)));
        return line;
    }

    private String billTitle(Transaction tx) {
        if ("adjustment".equals(tx.type)) return "平账";
        if ("transfer".equals(tx.type)) return "转账";
        return safe(tx.category);
    }

    private String billAmount(Transaction tx) {
        if ("adjustment".equals(tx.type)) return "调 " + PaymentParser.formatMoney(tx.amountCents);
        if ("transfer".equals(tx.type)) return "转 " + PaymentParser.formatMoney(tx.amountCents);
        return ("income".equals(tx.type) ? "+" : "-") + PaymentParser.formatMoney(tx.amountCents);
    }

    private String billIcon(Transaction tx) {
        if ("income".equals(tx.type)) return "入";
        if ("transfer".equals(tx.type)) return "转";
        if ("adjustment".equals(tx.type)) return "平";
        return "支";
    }

    private int billColor(Transaction tx) {
        if ("income".equals(tx.type)) return Color.rgb(44, 188, 128);
        if ("transfer".equals(tx.type)) return Ui.ACCENT;
        if ("adjustment".equals(tx.type)) return Color.rgb(92, 112, 140);
        return Ui.WARNING;
    }

    private int billBg(Transaction tx) {
        if ("income".equals(tx.type)) return Color.rgb(230, 248, 240);
        if ("transfer".equals(tx.type)) return Color.rgb(232, 245, 255);
        if ("adjustment".equals(tx.type)) return Color.rgb(238, 241, 245);
        return Color.rgb(255, 238, 241);
    }

    private String accountInitial(String name) {
        if (name.contains("中国")) return "中";
        if (name.contains("交通")) return "交";
        if (name.contains("招商")) return "招";
        if (name.contains("微信")) return "微";
        if (name.contains("支付宝")) return "支";
        if (name.contains("京东")) return "京";
        return name.length() == 0 ? "账" : name.substring(0, 1);
    }

    private int accountColor(String name) {
        if (name.contains("中国")) return Color.rgb(197, 34, 49);
        if (name.contains("交通")) return Color.rgb(39, 72, 169);
        if (name.contains("招商")) return Color.rgb(201, 58, 70);
        if (name.contains("微信")) return Color.rgb(24, 183, 82);
        if (name.contains("支付宝")) return Color.rgb(24, 132, 241);
        if (name.contains("京东")) return Color.rgb(226, 45, 52);
        return Ui.ACCENT;
    }

    private String safe(String value) {
        return value == null || value.length() == 0 ? "未填写" : value;
    }

    private void requestPostNotifications() {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1001);
        }
    }
}
