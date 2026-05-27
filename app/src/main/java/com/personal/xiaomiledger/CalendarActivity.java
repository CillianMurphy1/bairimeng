package com.personal.xiaomiledger;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map;

public class CalendarActivity extends Activity {
    private TransactionStore store;
    private Calendar viewMonth = Calendar.getInstance();
    private TextView monthTitle;
    private LinearLayout dayList;
    private GridLayout grid;
    private ScrollView scroll;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Ui.applyBackground(this);
        store = new TransactionStore(this);
        buildUi();
    }

    private void buildUi() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(Ui.PAPER);

        page.addView(topBar());

        scroll = new ScrollView(this);
        scroll.setClipToPadding(false);

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(Ui.dp(this, 14), Ui.dp(this, 8), Ui.dp(this, 14), Ui.dp(this, 16));

        body.addView(monthNav());
        body.addView(Ui.spacer(this, 4));
        body.addView(weekdayHeader());
        body.addView(Ui.spacer(this, 4));
        grid = new GridLayout(this);
        grid.setColumnCount(7);
        body.addView(grid);

        body.addView(Ui.spacer(this, 16));
        body.addView(Ui.line(this));
        body.addView(Ui.spacer(this, 8));

        TextView detailTitle = Ui.text(this, "当日明细", 16, Ui.MUTED, Typeface.BOLD);
        body.addView(detailTitle);
        body.addView(Ui.spacer(this, 8));

        dayList = new LinearLayout(this);
        dayList.setOrientation(LinearLayout.VERTICAL);
        body.addView(dayList);

        scroll.addView(body);
        page.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        setContentView(page);

        refresh();
    }

    private LinearLayout topBar() {
        LinearLayout bar = Ui.row(this);
        bar.setBackgroundColor(Color.WHITE);
        bar.setPadding(Ui.dp(this, 14), Ui.dp(this, 14), Ui.dp(this, 18), Ui.dp(this, 10));
        bar.setMinimumHeight(Ui.dp(this, 68));

        TextView back = Ui.text(this, "←", 24, Ui.INK, Typeface.NORMAL);
        back.setGravity(Gravity.CENTER);
        back.setOnClickListener(v -> finish());
        bar.addView(back, new LinearLayout.LayoutParams(Ui.dp(this, 48), Ui.dp(this, 48)));

        monthTitle = Ui.text(this, "", 20, Ui.INK, Typeface.BOLD);
        monthTitle.setGravity(Gravity.CENTER);
        bar.addView(monthTitle, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        TextView todayBtn = Ui.text(this, "今天", 15, Ui.ACCENT, Typeface.BOLD);
        todayBtn.setGravity(Gravity.CENTER);
        todayBtn.setPadding(Ui.dp(this, 10), 0, 0, 0);
        todayBtn.setOnClickListener(v -> {
            viewMonth = Calendar.getInstance();
            refresh();
        });
        bar.addView(todayBtn);
        return bar;
    }

    private LinearLayout monthNav() {
        LinearLayout row = Ui.row(this);
        row.setPadding(Ui.dp(this, 6), 0, Ui.dp(this, 6), 0);

        TextView prev = Ui.text(this, "〈", 22, Ui.ACCENT, Typeface.BOLD);
        prev.setGravity(Gravity.CENTER);
        prev.setOnClickListener(v -> { viewMonth.add(Calendar.MONTH, -1); refresh(); });
        row.addView(prev, new LinearLayout.LayoutParams(Ui.dp(this, 40), Ui.dp(this, 40)));

        TextView title = Ui.text(this, yearMonthTitle(), 20, Ui.INK, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        row.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        TextView next = Ui.text(this, "〉", 22, Ui.ACCENT, Typeface.BOLD);
        next.setGravity(Gravity.CENTER);
        next.setOnClickListener(v -> { viewMonth.add(Calendar.MONTH, 1); refresh(); });
        row.addView(next, new LinearLayout.LayoutParams(Ui.dp(this, 40), Ui.dp(this, 40)));

        return row;
    }

    private LinearLayout weekdayHeader() {
        LinearLayout row = Ui.row(this);
        String[] days = {"日", "一", "二", "三", "四", "五", "六"};
        for (String d : days) {
            TextView tv = Ui.text(this, d, 13, Ui.MUTED, Typeface.BOLD);
            tv.setGravity(Gravity.CENTER);
            row.addView(tv, new LinearLayout.LayoutParams(0, Ui.dp(this, 32), 1));
        }
        return row;
    }

    private void refresh() {
        monthTitle.setText(yearMonthTitle());
        buildCalendarGrid();
        showDayDetail(null);
    }

    private String yearMonthTitle() {
        return new SimpleDateFormat("yyyy年M月", Locale.CHINA).format(viewMonth.getTime());
    }

    private void buildCalendarGrid() {
        grid.removeAllViews();

        Calendar cal = (Calendar) viewMonth.clone();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1; // Sunday=0
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        Calendar today = Calendar.getInstance();
        long monthStart = cal.getTimeInMillis();
        cal.add(Calendar.MONTH, 1);
        long monthEnd = cal.getTimeInMillis();

        List<Transaction> txns = store.between(monthStart, monthEnd);
        Map<Integer, long[]> daySums = new HashMap<>(); // day -> [income, expense]
        for (Transaction tx : txns) {
            Calendar tc = Calendar.getInstance();
            tc.setTimeInMillis(tx.occurredAt);
            int day = tc.get(Calendar.DAY_OF_MONTH);
            long[] sums = daySums.get(day);
            if (sums == null) {
                sums = new long[2];
                daySums.put(day, sums);
            }
            if ("income".equals(tx.type)) sums[0] += tx.amountCents;
            else if ("expense".equals(tx.type)) sums[1] += tx.amountCents;
        }

        int totalCells = firstDayOfWeek + daysInMonth;
        int rows = (int) Math.ceil(totalCells / 7.0);
        grid.setRowCount(rows);

        // empty cells before first day
        for (int i = 0; i < firstDayOfWeek; i++) {
            grid.addView(emptyCell());
        }

        for (int day = 1; day <= daysInMonth; day++) {
            final int d = day;
            boolean isToday = day == today.get(Calendar.DAY_OF_MONTH)
                    && viewMonth.get(Calendar.MONTH) == today.get(Calendar.MONTH)
                    && viewMonth.get(Calendar.YEAR) == today.get(Calendar.YEAR);

            LinearLayout cell = new LinearLayout(this);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER_HORIZONTAL);
            cell.setPadding(0, Ui.dp(this, 2), 0, Ui.dp(this, 2));

            TextView dayNum = Ui.text(this, String.valueOf(day), 15, Ui.INK, isToday ? Typeface.BOLD : Typeface.NORMAL);
            dayNum.setGravity(Gravity.CENTER);
            if (isToday) {
                dayNum.setBackground(Ui.bg(this, Ui.ACCENT_GOLD, 999));
                dayNum.setTextColor(Color.WHITE);
            }
            cell.addView(dayNum, new LinearLayout.LayoutParams(Ui.dp(this, 36), Ui.dp(this, 36)));

            long[] sums = daySums.get(day);
            LinearLayout dots = new LinearLayout(this);
            dots.setOrientation(LinearLayout.HORIZONTAL);
            dots.setGravity(Gravity.CENTER);
            if (sums != null) {
                if (sums[0] > 0) dots.addView(dot(Ui.SUCCESS));
                if (sums[1] > 0) dots.addView(dot(Ui.WARNING));
            }
            cell.addView(dots, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, Ui.dp(this, 10)));

            cell.setOnClickListener(v -> showDayDetail(d));
            cell.setClickable(true);
            cell.setBackgroundColor(Ui.PANEL);

            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = 0;
            lp.height = Ui.dp(this, 54);
            lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            cell.setLayoutParams(lp);
            grid.addView(cell);
        }

        // remaining empty cells
        int remaining = rows * 7 - totalCells;
        for (int i = 0; i < remaining; i++) {
            grid.addView(emptyCell());
        }
    }

    private View emptyCell() {
        View v = new View(this);
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = 0;
        lp.height = Ui.dp(this, 54);
        lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        v.setLayoutParams(lp);
        return v;
    }

    private View dot(int color) {
        View d = new View(this);
        d.setBackground(Ui.bg(this, color, 999));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.dp(this, 6), Ui.dp(this, 6));
        lp.setMargins(Ui.dp(this, 1), 0, Ui.dp(this, 1), 0);
        d.setLayoutParams(lp);
        return d;
    }

    private void showDayDetail(Integer day) {
        dayList.removeAllViews();
        if (day == null) {
            TextView hint = Ui.text(this, "点击日期查看当天账单", 14, Ui.MUTED, Typeface.NORMAL);
            hint.setGravity(Gravity.CENTER);
            hint.setPadding(0, Ui.dp(this, 20), 0, 0);
            dayList.addView(hint);
            return;
        }

        Calendar cal = (Calendar) viewMonth.clone();
        cal.set(Calendar.DAY_OF_MONTH, day);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long dayStart = cal.getTimeInMillis();
        cal.add(Calendar.DAY_OF_MONTH, 1);
        long dayEnd = cal.getTimeInMillis();

        List<Transaction> txns = store.between(dayStart, dayEnd);
        if (txns.isEmpty()) {
            TextView empty = Ui.text(this, day + "日没有账单", 14, Ui.MUTED, Typeface.NORMAL);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, Ui.dp(this, 12), 0, 0);
            dayList.addView(empty);
            return;
        }

        long incomeSum = 0, expenseSum = 0;
        for (Transaction tx : txns) {
            if ("income".equals(tx.type)) incomeSum += tx.amountCents;
            else if ("expense".equals(tx.type)) expenseSum += tx.amountCents;
        }

        LinearLayout summary = Ui.row(this);
        summary.setPadding(0, Ui.dp(this, 4), 0, Ui.dp(this, 8));
        summary.addView(Ui.text(this, "收 " + PaymentParser.formatMoney(incomeSum), 15, Ui.SUCCESS, Typeface.BOLD));
        summary.addView(Ui.text(this, "  支 " + PaymentParser.formatMoney(expenseSum), 15, Ui.WARNING, Typeface.BOLD));
        TextView editBtn = Ui.text(this, "记一笔", 14, Ui.ACCENT, Typeface.BOLD);
        editBtn.setGravity(Gravity.CENTER);
        editBtn.setOnClickListener(v -> startActivity(new Intent(this, AddBillActivity.class)));
        LinearLayout.LayoutParams elp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        elp.gravity = Gravity.END | Gravity.CENTER_VERTICAL;
        summary.addView(editBtn, elp);
        dayList.addView(summary);

        for (Transaction tx : txns) {
            dayList.addView(dayBillRow(tx));
        }
    }

    private LinearLayout dayBillRow(Transaction tx) {
        LinearLayout row = Ui.row(this);
        row.setPadding(0, Ui.dp(this, 9), 0, Ui.dp(this, 9));

        TextView icon = Ui.text(this, billIcon(tx), 14, billColor(tx), Typeface.BOLD);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(Ui.bg(this, billBg(tx), 6));
        row.addView(icon, new LinearLayout.LayoutParams(Ui.dp(this, 32), Ui.dp(this, 32)));

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        TextView cat = Ui.text(this, billTitle(tx), 15, Ui.INK, Typeface.BOLD);
        info.addView(cat);
        String sub = billSub(tx);
        if (!sub.isEmpty()) info.addView(Ui.text(this, sub, 13, Ui.MUTED, Typeface.NORMAL));
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        ilp.setMargins(Ui.dp(this, 10), 0, Ui.dp(this, 8), 0);
        row.addView(info, ilp);

        row.addView(Ui.text(this, billAmount(tx), 16, billColor(tx), Typeface.BOLD));

        row.setOnClickListener(v -> {
            if ("income".equals(tx.type) || "expense".equals(tx.type)) {
                startActivity(EditTransactionActivity.intentForTransaction(this, tx.id));
            }
        });
        return row;
    }

    private String billTitle(Transaction tx) {
        if ("transfer".equals(tx.type)) return "转账";
        if ("adjustment".equals(tx.type)) return "平账";
        return safe(tx.category);
    }

    private String billSub(Transaction tx) {
        if ("transfer".equals(tx.type)) return safe(tx.accountName) + " → " + safe(tx.targetAccountName);
        String sub = safe(tx.merchant);
        if (!sub.isEmpty() && !sub.equals(tx.category)) return sub + " · " + safe(tx.accountName);
        return safe(tx.accountName);
    }

    private String billAmount(Transaction tx) {
        if ("transfer".equals(tx.type)) return "转 " + PaymentParser.formatMoney(tx.amountCents);
        if ("adjustment".equals(tx.type)) return "调 " + PaymentParser.formatMoney(tx.amountCents);
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

    private String safe(String value) {
        return value == null || value.isEmpty() ? "未填写" : value;
    }
}
