package com.personal.xiaomiledger;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
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
        Ui.applyBackground(this);
        if (Build.VERSION.SDK_INT >= 29) {
            getWindow().getAttributes().layoutInDisplayCutoutMode =
                    android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT;
        }
        store = new TransactionStore(this);
        buildUi();
        requestPostNotifications();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
        LedgerWidgetProvider.updateAllWidgets(this);
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
                if (!drawerSwipeWatching || drawerSwipeOpened) return;
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
        }
    }

    private void buildUi() {
        FrameLayout screen = new FrameLayout(this);
        screen.setBackgroundColor(Ui.PAPER);
        screen.setClipToPadding(false);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);

        page.addView(topBar());

        ScrollView scroll = new ScrollView(this);
        scroll.setClipToPadding(false);
        scroll.setPadding(0, 0, 0, Ui.dp(this, 12));
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(Ui.dp(this, 14), Ui.dp(this, 8), Ui.dp(this, 14), Ui.dp(this, 96));
        scroll.addView(content);
        page.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        page.addView(bottomNav());

        screen.addView(page, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        TextView fab = Ui.text(this, "+", 36, Color.WHITE, Typeface.NORMAL);
        fab.setGravity(Gravity.CENTER);
        fab.setBackground(Ui.bg(this, Ui.ACCENT_GOLD, 999));
        if (Build.VERSION.SDK_INT >= 21) {
            fab.setElevation(Ui.dp(this, Ui.ELEVATION_FAB));
        }
        fab.setOnClickListener(v -> startActivity(new Intent(this, AddBillActivity.class)));
        FrameLayout.LayoutParams fabLp = new FrameLayout.LayoutParams(Ui.dp(this, 64), Ui.dp(this, 64), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        fabLp.setMargins(0, 0, 0, Ui.dp(this, 30));
        screen.addView(fab, fabLp);

        setContentView(screen);
    }

    // ── top bar ──

    private int statusBarHeight() {
        int id = getResources().getIdentifier("status_bar_height", "dimen", "android");
        return id > 0 ? getResources().getDimensionPixelSize(id) : 0;
    }

    private LinearLayout topBar() {
        LinearLayout bar = Ui.row(this);
        bar.setBackgroundColor(Color.WHITE);
        int top = statusBarHeight();
        bar.setPadding(Ui.dp(this, 18), top, Ui.dp(this, 18), Ui.dp(this, 10));
        bar.setMinimumHeight(Ui.dp(this, 48) + statusBarHeight());
        if (Build.VERSION.SDK_INT >= 21) {
            bar.setElevation(Ui.dp(this, 1));
        }

        TextView menu = Ui.text(this, "☰", 30, Ui.INK, Typeface.NORMAL);
        menu.setGravity(Gravity.CENTER);
        menu.setOnClickListener(v -> showDrawer());
        bar.addView(menu, new LinearLayout.LayoutParams(Ui.dp(this, 48), Ui.dp(this, 48)));

        monthText = Ui.text(this, monthTitle(), 20, Ui.INK, Typeface.BOLD);
        monthText.setGravity(Gravity.CENTER);
        monthText.setOnClickListener(v -> showMonthPicker());
        bar.addView(monthText, new LinearLayout.LayoutParams(0, Ui.dp(this, 48), 1));

        TextView statsBtn = Ui.text(this, "统计", 15, Ui.MUTED, Typeface.NORMAL);
        statsBtn.setGravity(Gravity.CENTER);
        statsBtn.setClickable(true);
        statsBtn.setOnClickListener(v -> startActivity(new Intent(this, StatsActivity.class)));
        bar.addView(statsBtn, new LinearLayout.LayoutParams(Ui.dp(this, 48), Ui.dp(this, 48)));

        TextView calBtn = Ui.text(this, "日历", 15, Ui.MUTED, Typeface.NORMAL);
        calBtn.setGravity(Gravity.CENTER);
        calBtn.setClickable(true);
        calBtn.setOnClickListener(v -> startActivity(new Intent(this, CalendarActivity.class)));
        bar.addView(calBtn, new LinearLayout.LayoutParams(Ui.dp(this, 48), Ui.dp(this, 48)));

        TextView searchBtn = Ui.text(this, "搜索", 15, Ui.MUTED, Typeface.NORMAL);
        searchBtn.setGravity(Gravity.CENTER);
        searchBtn.setClickable(true);
        searchBtn.setOnClickListener(v -> startActivity(new Intent(this, SearchActivity.class)));
        bar.addView(searchBtn, new LinearLayout.LayoutParams(Ui.dp(this, 48), Ui.dp(this, 48)));

        return bar;
    }

    // ── bottom nav ──

    private LinearLayout bottomNav() {
        LinearLayout nav = Ui.row(this);
        nav.setBackgroundColor(Color.WHITE);
        nav.setPadding(0, Ui.dp(this, 6), 0, Ui.dp(this, 6));
        nav.setMinimumHeight(Ui.dp(this, 56));
        if (Build.VERSION.SDK_INT >= 21) {
            nav.setElevation(Ui.dp(this, 4));
        }

        nav.addView(navItem("首页", true, v -> {}));
        nav.addView(navItem("统计", false, v -> startActivity(new Intent(this, StatsActivity.class))));
        nav.addView(navSpacer());
        nav.addView(navItem("预算", false, v -> startActivity(new Intent(this, BudgetActivity.class))));
        nav.addView(navItem("分类", false, v -> startActivity(new Intent(this, CategoryManageActivity.class))));
        return nav;
    }

    private View navSpacer() {
        View spacer = new View(this);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(0, 1, 1));
        return spacer;
    }

    private TextView navItem(String label, boolean active, View.OnClickListener listener) {
        TextView item = Ui.text(this, label, 12, active ? Ui.ACCENT : Ui.MUTED, active ? Typeface.BOLD : Typeface.NORMAL);
        item.setGravity(Gravity.CENTER);
        item.setOnClickListener(listener);
        item.setPadding(0, Ui.dp(this, 6), 0, Ui.dp(this, 4));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        lp.gravity = Gravity.CENTER;
        item.setLayoutParams(lp);
        return item;
    }

    // ── refresh ──

    private long[] selectedMonthRange() {
        Calendar cal = (Calendar) selectedMonth.clone();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long start = cal.getTimeInMillis();
        cal.add(Calendar.MONTH, 1);
        long end = cal.getTimeInMillis();
        return new long[]{start, end};
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

    // ── asset card ──

    private void addAssetCard() {
        long totalAssets = store.totalByKind("asset");
        long totalLiabilities = Math.abs(store.totalByKind("liability"));
        long netAssets = totalAssets - totalLiabilities;
        List<Account> accounts = store.accounts();
        final boolean allHidden = PrefsManager.areAllBalancesHidden(this, accounts);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(Ui.bg(this, Color.WHITE, 16));
        card.setPadding(0, 0, 0, 0);
        if (Build.VERSION.SDK_INT >= 21) {
            card.setElevation(Ui.dp(this, Ui.ELEVATION_CARD));
        }
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.setMargins(0, 0, 0, Ui.dp(this, 12));
        card.setLayoutParams(cardLp);

        LinearLayout header = Ui.row(this);
        header.setBackground(Ui.bg(this, Ui.ACCENT, 0));
        header.setPadding(Ui.dp(this, 20), Ui.dp(this, 20), Ui.dp(this, 20), Ui.dp(this, 16));
        GradientDrawable headerBg = Ui.bg(this, Ui.ACCENT, 16);
        headerBg.setCornerRadii(new float[]{
                Ui.dp(this, 16), Ui.dp(this, 16),
                Ui.dp(this, 16), Ui.dp(this, 16),
                0, 0,
                0, 0
        });
        header.setBackground(headerBg);

        TextView brand = Ui.text(this, "白日夢", 16, Color.argb(200, 255, 255, 255), Typeface.BOLD);
        brand.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(brand, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        // sun/moon toggle
        int sunColor = Color.rgb(255, 215, 0); // gold
        int moonColor = Color.rgb(66, 133, 244); // blue
        TextView eyeBtn = Ui.text(this, allHidden ? "☾" : "☀", 22, allHidden ? moonColor : sunColor, Typeface.NORMAL);
        eyeBtn.setGravity(Gravity.CENTER);
        eyeBtn.setPadding(Ui.dp(this, 6), 0, Ui.dp(this, 6), 0);
        eyeBtn.setOnClickListener(v -> {
            List<Account> freshAccounts = store.accounts();
            if (PrefsManager.areAllBalancesHidden(MainActivity.this, freshAccounts)) {
                PrefsManager.showAllBalances(MainActivity.this);
            } else {
                PrefsManager.hideAllBalances(MainActivity.this, freshAccounts);
            }
            refresh();
        });
        header.addView(eyeBtn);

        TextView adjustBtn = Ui.text(this, "平账", 13, Color.WHITE, Typeface.BOLD);
        adjustBtn.setGravity(Gravity.CENTER);
        adjustBtn.setBackground(Ui.bg(this, Color.argb(80, 255, 255, 255), 999));
        adjustBtn.setPadding(Ui.dp(this, 14), Ui.dp(this, 6), Ui.dp(this, 14), Ui.dp(this, 6));
        adjustBtn.setOnClickListener(v -> startActivity(new Intent(this, AdjustBalanceActivity.class)));
        header.addView(adjustBtn);
        card.addView(header);

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(Ui.dp(this, 20), Ui.dp(this, 14), Ui.dp(this, 20), Ui.dp(this, 20));
        body.setBackgroundColor(Color.WHITE);
        card.addView(body);

        TextView netLabel = Ui.text(this, "净资产", 14, Ui.MUTED, Typeface.NORMAL);
        netLabel.setGravity(Gravity.CENTER);
        body.addView(netLabel);

        String netText = allHidden ? "****" : TransactionStore.formatMoneySigned(netAssets);
        TextView netValue = Ui.text(this, netText, 38, Ui.INK, Typeface.BOLD);
        netValue.setGravity(Gravity.CENTER);
        body.addView(netValue);
        body.addView(Ui.spacer(this, 16));

        LinearLayout row = Ui.row(this);
        row.setGravity(Gravity.CENTER);
        row.addView(assetMetric("总资产", allHidden ? "****" : TransactionStore.formatMoneySigned(totalAssets), Ui.SUCCESS));
        View divider = new View(this);
        divider.setBackgroundColor(Ui.LINE);
        row.addView(divider, new LinearLayout.LayoutParams(Ui.dp(this, 1), Ui.dp(this, 32)));
        row.addView(assetMetric("总负债", allHidden ? "****" : (totalLiabilities == 0 ? "无" : TransactionStore.formatMoneySigned(totalLiabilities)), Ui.WARNING));
        body.addView(row);

        content.addView(card);
    }

    private TextView assetMetric(String label, String value, int valueColor) {
        TextView view = Ui.text(this, label + "\n" + value, 15, Ui.INK, Typeface.NORMAL);
        view.setGravity(Gravity.CENTER);
        view.setPadding(Ui.dp(this, 20), Ui.dp(this, 4), Ui.dp(this, 20), Ui.dp(this, 4));
        return view;
    }

    // ── month overview ──

    private void addMonthOverviewCard() {
        long[] range = selectedMonthRange();
        long income = store.sumBetween("income", range[0], range[1]);
        long expense = store.sumBetween("expense", range[0], range[1]);

        LinearLayout card = Ui.card(this);
        LinearLayout header = Ui.row(this);
        header.addView(Ui.text(this, "本月收支", 20, Ui.INK, Typeface.BOLD), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView more = Ui.text(this, "统计", 14, Ui.ACCENT, Typeface.BOLD);
        more.setOnClickListener(v -> startActivity(new Intent(this, StatsActivity.class)));
        header.addView(more);
        card.addView(header);
        card.addView(Ui.spacer(this, 14));

        LinearLayout row = Ui.row(this);
        row.addView(monthMetric("收入", PaymentParser.formatMoney(income), Ui.SUCCESS, Ui.INCOME_BG));
        row.addView(monthMetric("支出", PaymentParser.formatMoney(expense), Ui.WARNING, Ui.EXPENSE_BG));
        row.addView(monthMetric("结余", TransactionStore.formatMoneySigned(income - expense), Ui.INK, Ui.CHIP_BG));
        card.addView(row);
        content.addView(card);
    }

    private TextView monthMetric(String label, String value, int color, int bg) {
        TextView view = Ui.text(this, label + "\n" + value, 14, color, Typeface.BOLD);
        view.setGravity(Gravity.CENTER);
        view.setBackground(Ui.bg(this, bg, 12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, Ui.dp(this, 68), 1);
        lp.setMargins(Ui.dp(this, 3), 0, Ui.dp(this, 3), 0);
        view.setLayoutParams(lp);
        return view;
    }

    // ── quick actions ──

    private void addQuickActionsCard() {
        LinearLayout card = Ui.card(this);
        card.setPadding(Ui.dp(this, 10), Ui.dp(this, 12), Ui.dp(this, 10), Ui.dp(this, 12));
        LinearLayout row = Ui.row(this);
        row.addView(quickAction("支出", Ui.WARNING, v -> startActivity(addBillIntent("expense"))));
        row.addView(quickAction("收入", Ui.SUCCESS, v -> startActivity(addBillIntent("income"))));
        row.addView(quickAction("转账", Ui.TRANSFER, v -> startActivity(addBillIntent("transfer"))));
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
        TextView button = Ui.text(this, label, 15, color, Typeface.BOLD);
        button.setGravity(Gravity.CENTER);
        button.setBackground(Ui.bg(this, Ui.CHIP_BG, 12));
        button.setOnClickListener(listener);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, Ui.dp(this, 46), 1);
        lp.setMargins(Ui.dp(this, 4), 0, Ui.dp(this, 4), 0);
        button.setLayoutParams(lp);
        return button;
    }

    // ── accounts ──

    private void addAccountsCard() {
        LinearLayout card = Ui.card(this);
        LinearLayout header = Ui.row(this);
        header.addView(Ui.text(this, "资金账户", 20, Ui.INK, Typeface.BOLD), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView add = Ui.text(this, "+", 24, Ui.ACCENT, Typeface.BOLD);
        add.setGravity(Gravity.CENTER);
        add.setOnClickListener(v -> startActivity(new Intent(this, AddAccountActivity.class)));
        header.addView(add, new LinearLayout.LayoutParams(Ui.dp(this, 40), Ui.dp(this, 40)));
        TextView remove = Ui.text(this, "-", 24, Ui.WARNING, Typeface.BOLD);
        remove.setGravity(Gravity.CENTER);
        remove.setOnClickListener(v -> startActivity(new Intent(this, RemoveAccountActivity.class)));
        header.addView(remove, new LinearLayout.LayoutParams(Ui.dp(this, 40), Ui.dp(this, 40)));

        List<Account> accounts = store.accounts();
        final boolean allHidden = PrefsManager.areAllBalancesHidden(this, accounts);
        int sunColor = Color.rgb(255, 215, 0);
        int moonColor = Color.rgb(135, 206, 250);
        TextView eyeBtn = Ui.text(this, allHidden ? "☾" : "☀", 22, allHidden ? moonColor : sunColor, Typeface.NORMAL);
        eyeBtn.setGravity(Gravity.CENTER);
        eyeBtn.setPadding(Ui.dp(this, 6), 0, Ui.dp(this, 8), 0);
        eyeBtn.setOnClickListener(v -> {
            List<Account> fresh = store.accounts();
            if (PrefsManager.areAllBalancesHidden(MainActivity.this, fresh)) {
                PrefsManager.showAllBalances(MainActivity.this);
            } else {
                PrefsManager.hideAllBalances(MainActivity.this, fresh);
            }
            refresh();
        });
        header.addView(eyeBtn);

        String totalText = allHidden ? "****" : TransactionStore.formatMoneySigned(store.totalByKind("asset"));
        TextView total = Ui.text(this, totalText + "  ", 18, allHidden ? Ui.MUTED : Ui.INK, Typeface.NORMAL);
        total.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        header.addView(total);

        card.addView(header);
        card.addView(Ui.line(this));

        for (int i = 0; i < accounts.size(); i++) {
            card.addView(accountRow(accounts.get(i)));
            if (i < accounts.size() - 1) card.addView(Ui.line(this));
        }
        content.addView(card);
    }

    private LinearLayout accountRow(Account account) {
        LinearLayout row = Ui.row(this);
        row.setPadding(0, Ui.dp(this, 12), 0, Ui.dp(this, 12));
        row.setOnClickListener(v -> startActivity(AdjustBalanceActivity.intentForAccount(this, account.name)));

        TextView icon = Ui.text(this, accountInitial(account.name), 17, Color.WHITE, Typeface.BOLD);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(Ui.bg(this, accountColor(account.name), 10));
        row.addView(icon, new LinearLayout.LayoutParams(Ui.dp(this, 42), Ui.dp(this, 42)));

        TextView name = Ui.text(this, account.name, 18, Ui.INK, Typeface.NORMAL);
        LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        nameLp.setMargins(Ui.dp(this, 14), 0, 0, 0);
        row.addView(name, nameLp);

        boolean hidden = PrefsManager.isAccountBalanceHidden(this, account.name);
        String balanceText = hidden ? "****" : TransactionStore.formatMoneySigned(account.balanceCents);
        TextView balance = Ui.text(this, balanceText, 18, hidden ? Ui.MUTED : Ui.INK, Typeface.NORMAL);
        row.addView(balance);

        // per-account sun/moon
        int sunColor = Color.rgb(255, 215, 0);
        int moonColor = Color.rgb(135, 206, 250);
        TextView eye = Ui.text(this, hidden ? "☾" : "☀", 18, hidden ? moonColor : sunColor, Typeface.NORMAL);
        eye.setGravity(Gravity.CENTER);
        eye.setPadding(Ui.dp(this, 4), 0, 0, 0);
        eye.setClickable(true);
        eye.setOnClickListener(v -> {
            PrefsManager.toggleAccountBalanceHidden(MainActivity.this, account.name);
            refresh();
        });
        row.addView(eye);
        return row;
    }

    // ── recent bills ──

    private void addBillsCard() {
        LinearLayout card = Ui.card(this);
        LinearLayout header = Ui.row(this);
        header.addView(Ui.text(this, "最近账单", 20, Ui.INK, Typeface.BOLD), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView more = Ui.text(this, "全部", 14, Ui.ACCENT, Typeface.BOLD);
        more.setOnClickListener(v -> startActivity(new Intent(this, CalendarActivity.class)));
        header.addView(more);
        card.addView(header);

        List<Transaction> transactions = store.recent(5);
        if (transactions.isEmpty()) {
            TextView empty = Ui.text(this, "还没有账单，点 + 记一笔吧", 15, Ui.MUTED, Typeface.NORMAL);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, Ui.dp(this, 28), 0, Ui.dp(this, 20));
            card.addView(empty);
        } else {
            for (int i = 0; i < transactions.size(); i++) {
                card.addView(Ui.line(this));
                card.addView(billRow(transactions.get(i)));
            }
        }
        content.addView(card);
    }

    private LinearLayout billRow(Transaction tx) {
        LinearLayout row = Ui.row(this);
        row.setPadding(0, Ui.dp(this, 11), 0, Ui.dp(this, 11));
        row.setOnClickListener(v -> openTransaction(tx));

        TextView icon = Ui.text(this, billIcon(tx), 15, billColor(tx), Typeface.BOLD);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(Ui.bg(this, billBg(tx), 8));
        row.addView(icon, new LinearLayout.LayoutParams(Ui.dp(this, 38), Ui.dp(this, 38)));

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.addView(Ui.text(this, billTitle(tx), 16, Ui.INK, Typeface.BOLD));
        info.addView(Ui.text(this, billSubTitle(tx), 14, Ui.INK, Typeface.NORMAL));
        info.addView(Ui.text(this, TransactionStore.formatDateTime(tx.occurredAt), 13, Ui.MUTED, Typeface.NORMAL));
        LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        infoLp.setMargins(Ui.dp(this, 12), 0, Ui.dp(this, 8), 0);
        row.addView(info, infoLp);

        TextView amount = Ui.text(this, billAmount(tx), 17, billColor(tx), Typeface.BOLD);
        amount.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        row.addView(amount);
        return row;
    }

    private void openTransaction(Transaction tx) {
        if ("income".equals(tx.type) || "expense".equals(tx.type) || "refund".equals(tx.type)) {
            startActivity(EditTransactionActivity.intentForTransaction(this, tx.id));
        } else if ("adjustment".equals(tx.type)) {
            startActivity(AdjustBalanceActivity.intentForAccount(this, tx.accountName));
        }
    }

    // ── auto record ──

    private void addAutoRecordCard() {
        LinearLayout card = Ui.card(this);
        card.setPadding(Ui.dp(this, 18), Ui.dp(this, 14), Ui.dp(this, 18), Ui.dp(this, 14));
        LinearLayout row = Ui.row(this);
        TextView indicator = new TextView(this);
        indicator.setBackground(Ui.bg(this, Ui.ACCENT, 999));
        row.addView(indicator, new LinearLayout.LayoutParams(Ui.dp(this, 4), Ui.dp(this, 18)));
        TextView label = Ui.text(this, "  自动记账", 18, Ui.INK, Typeface.BOLD);
        row.addView(label, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView arrow = Ui.text(this, ">", 20, Ui.MUTED, Typeface.NORMAL);
        row.addView(arrow);
        card.addView(row);
        card.addView(Ui.spacer(this, 4));
        card.addView(Ui.text(this, "微信、支付宝支付通知自动识别记录", 13, Ui.MUTED, Typeface.NORMAL));
        card.setOnClickListener(v -> startActivity(new Intent(this, AutoLogActivity.class)));
        content.addView(card);
    }

    // ── drawer ──

    private void showDrawer() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.HORIZONTAL);

        int drawerWidth = (int) (getResources().getDisplayMetrics().widthPixels * 0.74f);
        ScrollView drawerScroll = new ScrollView(this);
        drawerScroll.setBackgroundColor(Ui.PANEL);
        drawerScroll.setFillViewport(true);
        root.addView(drawerScroll, new LinearLayout.LayoutParams(drawerWidth, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout drawer = new LinearLayout(this);
        drawer.setOrientation(LinearLayout.VERTICAL);
        drawer.setBackgroundColor(Ui.PANEL);
        drawer.setPadding(Ui.dp(this, 28), Ui.dp(this, 56), Ui.dp(this, 24), Ui.dp(this, 28));
        drawerScroll.addView(drawer, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // ── header ──
        Drawable avatarDrawable = PrefsManager.loadAvatarDrawable(this, 72);
        if (avatarDrawable != null) {
            ImageView avatar = new ImageView(this);
            avatar.setImageDrawable(avatarDrawable);
            avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
            avatar.setClipToOutline(true);
            avatar.setOutlineProvider(new android.view.ViewOutlineProvider() {
                @Override
                public void getOutline(View v, android.graphics.Outline outline) {
                    outline.setOval(0, 0, v.getWidth(), v.getHeight());
                }
            });
            int avatarSize = Ui.dp(this, 72);
            drawer.addView(avatar, new LinearLayout.LayoutParams(avatarSize, avatarSize));
        } else {
            TextView avatar = Ui.text(this, "夢", 30, Color.WHITE, Typeface.BOLD);
            avatar.setGravity(Gravity.CENTER);
            avatar.setBackground(Ui.bg(this, Ui.ACCENT_GOLD, 999));
            drawer.addView(avatar, new LinearLayout.LayoutParams(Ui.dp(this, 72), Ui.dp(this, 72)));
        }
        drawer.addView(Ui.spacer(this, 16));
        drawer.addView(Ui.text(this, PrefsManager.getDisplayName(this), 26, Ui.INK, Typeface.BOLD));
        drawer.addView(Ui.spacer(this, 4));
        drawer.addView(Ui.text(this, PrefsManager.getSignature(this), 14, Ui.MUTED, Typeface.NORMAL));
        drawer.addView(Ui.spacer(this, 6));
        drawer.addView(Ui.line(this));
        drawer.addView(Ui.spacer(this, 20));

        // ── 功能 ──
        drawer.addView(drawerSection("功能"));
        drawer.addView(drawerItem("📅", "日历视图", v -> { dialog.dismiss(); startActivity(new Intent(this, CalendarActivity.class)); }));
        drawer.addView(drawerItem("🔍", "搜索账单", v -> { dialog.dismiss(); startActivity(new Intent(this, SearchActivity.class)); }));
        drawer.addView(drawerItem("📊", "统计分析", v -> { dialog.dismiss(); startActivity(new Intent(this, StatsActivity.class)); }));
        drawer.addView(Ui.spacer(this, 14));

        // ── 管理 ──
        drawer.addView(drawerSection("管理"));
        drawer.addView(drawerItem("💰", "预算管理", v -> { dialog.dismiss(); startActivity(new Intent(this, BudgetActivity.class)); }));
        drawer.addView(drawerItem("📂", "分类管理", v -> { dialog.dismiss(); startActivity(new Intent(this, CategoryManageActivity.class)); }));
        drawer.addView(drawerItem("🔄", "周期账单", v -> Toast.makeText(this, "周期账单下一版继续补", Toast.LENGTH_SHORT).show(), true));
        drawer.addView(Ui.spacer(this, 14));

        // ── 工具 ──
        drawer.addView(drawerSection("工具"));
        drawer.addView(drawerItem("🤖", "自动记账日志", v -> { dialog.dismiss(); startActivity(new Intent(this, AutoLogActivity.class)); }));
        drawer.addView(drawerItem("💼", "报销管理", v -> Toast.makeText(this, "报销管理下一版继续补", Toast.LENGTH_SHORT).show(), true));
        drawer.addView(drawerItem("🎯", "存钱计划", v -> Toast.makeText(this, "存钱计划下一版继续补", Toast.LENGTH_SHORT).show(), true));
        drawer.addView(Ui.spacer(this, 14));

        // ── 其他 ──
        drawer.addView(drawerSection("其他"));
        drawer.addView(drawerItem("⚙️", "设置·关于", v -> { dialog.dismiss(); startActivity(new Intent(this, SettingsActivity.class)); }));

        // shade
        View shade = new View(this);
        shade.setBackgroundColor(Color.argb(160, 0, 0, 0));
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

    private TextView drawerSection(String title) {
        TextView tv = Ui.text(this, title, 12, Ui.MUTED, Typeface.BOLD);
        tv.setPadding(Ui.dp(this, 4), Ui.dp(this, 4), 0, Ui.dp(this, 6));
        return tv;
    }

    private LinearLayout drawerItem(String icon, String title, View.OnClickListener listener) {
        return drawerItem(icon, title, listener, false);
    }

    private LinearLayout drawerItem(String icon, String title, View.OnClickListener listener, boolean comingSoon) {
        LinearLayout row = Ui.row(this);
        row.setPadding(Ui.dp(this, 8), Ui.dp(this, 11), Ui.dp(this, 8), Ui.dp(this, 11));
        row.setOnClickListener(listener);
        row.setClickable(true);
        Ui.rippleMasked(row);

        TextView i = Ui.text(this, icon, 22, comingSoon ? Ui.MUTED : Ui.INK, Typeface.NORMAL);
        i.setGravity(Gravity.CENTER);
        row.addView(i, new LinearLayout.LayoutParams(Ui.dp(this, 44), Ui.dp(this, 44)));

        TextView t = Ui.text(this, title, 18, comingSoon ? Ui.MUTED : Ui.INK, Typeface.NORMAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        lp.setMargins(Ui.dp(this, 14), 0, 0, 0);
        row.addView(t, lp);

        if (comingSoon) {
            TextView tag = Ui.text(this, "待做", 11, Ui.MUTED, Typeface.NORMAL);
            tag.setGravity(Gravity.CENTER);
            tag.setBackground(Ui.bg(this, Ui.LINE, 999));
            tag.setPadding(Ui.dp(this, 8), Ui.dp(this, 2), Ui.dp(this, 8), Ui.dp(this, 2));
            row.addView(tag);
        }
        return row;
    }

    // ── month picker ──

    private void showMonthPicker() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(Ui.bg(this, Color.WHITE, 16));
        root.setPadding(Ui.dp(this, 18), Ui.dp(this, 16), Ui.dp(this, 18), Ui.dp(this, 16));

        LinearLayout body = Ui.row(this);

        LinearLayout years = new LinearLayout(this);
        years.setOrientation(LinearLayout.VERTICAL);
        for (int y = 2029; y >= 2023; y--) {
            final int fy = y;
            boolean sel = y == selectedMonth.get(Calendar.YEAR);
            TextView year = Ui.text(this, y + "年", 20, sel ? Color.WHITE : Ui.INK, sel ? Typeface.BOLD : Typeface.NORMAL);
            year.setGravity(Gravity.CENTER);
            year.setBackground(Ui.bg(this, sel ? Ui.ACCENT : Ui.CHIP_BG, 999));
            year.setOnClickListener(v -> {
                selectedMonth.set(Calendar.YEAR, fy);
                if (monthText != null) monthText.setText(monthTitle());
                dialog.dismiss();
                refresh();
            });
            years.addView(year, new LinearLayout.LayoutParams(Ui.dp(this, 130), Ui.dp(this, 50)));
        }
        body.addView(years);

        GridLayout months = new GridLayout(this);
        months.setColumnCount(2);
        for (int m = 1; m <= 12; m++) {
            final int fm = m;
            boolean isCurrent = m == selectedMonth.get(Calendar.MONTH) + 1;
            TextView month = Ui.text(this, m + "月", 20, isCurrent ? Ui.ACCENT : Ui.INK, isCurrent ? Typeface.BOLD : Typeface.NORMAL);
            month.setGravity(Gravity.CENTER);
            month.setOnClickListener(v -> {
                selectedMonth.set(Calendar.MONTH, fm - 1);
                if (monthText != null) monthText.setText(monthTitle());
                dialog.dismiss();
                refresh();
            });
            months.addView(month, new ViewGroup.LayoutParams(Ui.dp(this, 100), Ui.dp(this, 52)));
        }
        body.addView(months);
        root.addView(body);

        dialog.setContentView(root);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout((int) (getResources().getDisplayMetrics().widthPixels * 0.84f), ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        dialog.show();
    }

    // ── helpers ──

    private String monthTitle() {
        return new SimpleDateFormat("yyyy年M月", Locale.CHINA).format(selectedMonth.getTime());
    }

    private String billTitle(Transaction tx) {
        if ("adjustment".equals(tx.type)) return "平账";
        if ("transfer".equals(tx.type)) return "转账";
        return safe(tx.category);
    }

    private String billSubTitle(Transaction tx) {
        if ("transfer".equals(tx.type)) return safe(tx.accountName) + " -> " + safe(tx.targetAccountName);
        if (tx.merchant != null && !tx.merchant.isEmpty()) return safe(tx.merchant) + " · " + safe(tx.accountName);
        return safe(tx.accountName);
    }

    private String billAmount(Transaction tx) {
        if ("adjustment".equals(tx.type)) return "调 " + PaymentParser.formatMoney(tx.amountCents);
        if ("transfer".equals(tx.type)) return "转 " + PaymentParser.formatMoney(tx.amountCents);
        return ("income".equals(tx.type) ? "+" : "-") + PaymentParser.formatMoney(tx.amountCents);
    }

    private String billIcon(Transaction tx) {
        if ("income".equals(tx.type)) return "收";
        if ("transfer".equals(tx.type)) return "转";
        if ("adjustment".equals(tx.type)) return "调";
        return "支";
    }

    private int billColor(Transaction tx) {
        if ("income".equals(tx.type)) return Ui.SUCCESS;
        if ("transfer".equals(tx.type)) return Ui.TRANSFER;
        if ("adjustment".equals(tx.type)) return Ui.MUTED;
        return Ui.WARNING;
    }

    private int billBg(Transaction tx) {
        if ("income".equals(tx.type)) return Ui.INCOME_BG;
        if ("transfer".equals(tx.type)) return Ui.TRANSFER_BG;
        if ("adjustment".equals(tx.type)) return Ui.ADJUST_BG;
        return Ui.EXPENSE_BG;
    }

    private String accountInitial(String name) {
        if (name.contains("中国")) return "中";
        if (name.contains("交通")) return "交";
        if (name.contains("招商")) return "招";
        if (name.contains("微信")) return "微";
        if (name.contains("支付宝")) return "支";
        if (name.contains("京东")) return "京";
        return name.isEmpty() ? "账" : name.substring(0, 1);
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
        return value == null || value.isEmpty() ? "未填写" : value;
    }

    private void requestPostNotifications() {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1001);
        }
    }
}
