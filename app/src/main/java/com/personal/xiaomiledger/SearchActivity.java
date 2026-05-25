package com.personal.xiaomiledger;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.List;

public class SearchActivity extends Activity {
    private TransactionStore store;
    private LinearLayout list;
    private LinearLayout summary;
    private EditText searchInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Ui.PAPER);
        store = new TransactionStore(this);
        buildUi();
        refresh("");
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Ui.PAPER);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(Ui.dp(this, 18), Ui.dp(this, 16), Ui.dp(this, 18), Ui.dp(this, 28));
        scroll.addView(root);

        LinearLayout top = Ui.row(this);
        TextView back = Ui.text(this, "‹", 38, Ui.INK, Typeface.NORMAL);
        back.setGravity(Gravity.CENTER);
        back.setOnClickListener(v -> finish());
        top.addView(back, new LinearLayout.LayoutParams(Ui.dp(this, 42), Ui.dp(this, 42)));
        TextView title = Ui.text(this, "搜索账单", 25, Ui.INK, Typeface.BOLD);
        top.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        root.addView(top);
        root.addView(Ui.spacer(this, 12));

        searchInput = new EditText(this);
        searchInput.setSingleLine(true);
        searchInput.setHint("搜索：分类、备注、账户、金额");
        searchInput.setTextSize(17);
        searchInput.setTextColor(Ui.INK);
        searchInput.setHintTextColor(Ui.MUTED);
        searchInput.setInputType(InputType.TYPE_CLASS_TEXT);
        searchInput.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        searchInput.setPadding(Ui.dp(this, 16), 0, Ui.dp(this, 16), 0);
        searchInput.setBackground(Ui.bg(this, Color.WHITE, 16));
        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            refresh(searchInput.getText().toString());
            return true;
        });
        root.addView(searchInput, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 56)));
        root.addView(Ui.spacer(this, 10));

        Button searchButton = new Button(this);
        searchButton.setText("搜索");
        searchButton.setTextColor(Color.WHITE);
        searchButton.setBackground(Ui.bg(this, Ui.ACCENT, 14));
        searchButton.setOnClickListener(v -> refresh(searchInput.getText().toString()));
        root.addView(searchButton, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 48)));
        root.addView(Ui.spacer(this, 12));

        summary = Ui.card(this);
        root.addView(summary);

        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        root.addView(list);
        setContentView(scroll);
    }

    private void refresh(String query) {
        List<Transaction> transactions = store.search(query, 200);
        renderSummary(transactions);
        renderList(transactions);
    }

    private void renderSummary(List<Transaction> transactions) {
        summary.removeAllViews();
        LinearLayout header = Ui.row(this);
        header.addView(Ui.text(this, "搜索汇总", 21, Ui.INK, Typeface.BOLD), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView count = Ui.text(this, transactions.size() + " 笔账单", 15, Ui.MUTED, Typeface.NORMAL);
        count.setGravity(Gravity.END);
        header.addView(count);
        summary.addView(header);
        summary.addView(Ui.spacer(this, 12));

        long expense = 0;
        long income = 0;
        long adjustment = 0;
        long transfer = 0;
        for (Transaction tx : transactions) {
            if ("income".equals(tx.type)) {
                income += tx.amountCents;
            } else if ("expense".equals(tx.type)) {
                expense += tx.amountCents;
            } else if ("adjustment".equals(tx.type)) {
                adjustment += tx.amountCents;
            } else if ("transfer".equals(tx.type)) {
                transfer += tx.amountCents;
            }
        }

        LinearLayout row = Ui.row(this);
        row.addView(metric("总支出", PaymentParser.formatMoney(expense), Ui.WARNING));
        row.addView(metric("总收入", PaymentParser.formatMoney(income), Ui.ACCENT));
        row.addView(metric("结余", TransactionStore.formatMoneySigned(income - expense), Ui.INK));
        summary.addView(row);
        if (adjustment > 0) {
            summary.addView(Ui.spacer(this, 8));
            summary.addView(Ui.text(this, "平账/调整：" + PaymentParser.formatMoney(adjustment), 15, Ui.MUTED, Typeface.NORMAL));
        }
        if (transfer > 0) {
            summary.addView(Ui.text(this, "转账：" + PaymentParser.formatMoney(transfer), 15, Ui.MUTED, Typeface.NORMAL));
        }
    }

    private TextView metric(String label, String value, int color) {
        TextView view = Ui.text(this, label + "\n" + value, 17, color, Typeface.BOLD);
        view.setGravity(Gravity.CENTER);
        view.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        return view;
    }

    private void renderList(List<Transaction> transactions) {
        list.removeAllViews();
        if (transactions.isEmpty()) {
            LinearLayout empty = Ui.card(this);
            empty.addView(Ui.text(this, "没有找到匹配账单。", 16, Ui.MUTED, Typeface.NORMAL));
            list.addView(empty);
            return;
        }

        String currentDate = "";
        LinearLayout dayCard = null;
        long dayExpense = 0;
        for (int i = 0; i < transactions.size(); i++) {
            Transaction tx = transactions.get(i);
            String date = TransactionStore.formatDate(tx.occurredAt);
            if (!date.equals(currentDate)) {
                currentDate = date;
                dayExpense = dayExpense(transactions, date);
                dayCard = Ui.card(this);
                LinearLayout header = Ui.row(this);
                header.addView(Ui.text(this, date, 20, Ui.INK, Typeface.BOLD), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
                TextView sum = Ui.text(this, dayExpense > 0 ? "支 ¥" + PaymentParser.formatMoney(dayExpense) : "", 17, Ui.INK, Typeface.BOLD);
                sum.setGravity(Gravity.END);
                header.addView(sum);
                dayCard.addView(header);
                dayCard.addView(Ui.spacer(this, 8));
                list.addView(dayCard);
            }
            if (dayCard != null) {
                dayCard.addView(billRow(tx));
                if (i < transactions.size() - 1 && TransactionStore.formatDate(transactions.get(i + 1).occurredAt).equals(currentDate)) {
                    dayCard.addView(line());
                }
            }
        }
    }

    private long dayExpense(List<Transaction> transactions, String date) {
        long sum = 0;
        for (Transaction tx : transactions) {
            if (date.equals(TransactionStore.formatDate(tx.occurredAt)) && "expense".equals(tx.type)) {
                sum += tx.amountCents;
            }
        }
        return sum;
    }

    private LinearLayout billRow(Transaction tx) {
        LinearLayout row = Ui.row(this);
        row.setPadding(0, Ui.dp(this, 11), 0, Ui.dp(this, 11));
        row.setOnClickListener(v -> openTransaction(tx));
        TextView icon = Ui.text(this, billIcon(tx), 19, billColor(tx), Typeface.BOLD);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(Ui.bg(this, billBg(tx), 999));
        row.addView(icon, new LinearLayout.LayoutParams(Ui.dp(this, 44), Ui.dp(this, 44)));

        TextView info = Ui.text(this, title(tx) + "\n" + subTitle(tx), 16, Ui.INK, Typeface.NORMAL);
        LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        infoLp.setMargins(Ui.dp(this, 14), 0, Ui.dp(this, 8), 0);
        row.addView(info, infoLp);

        TextView amount = Ui.text(this, amount(tx), 20, billColor(tx), Typeface.BOLD);
        amount.setGravity(Gravity.END);
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

    private String title(Transaction tx) {
        if ("adjustment".equals(tx.type)) return "平账";
        if ("transfer".equals(tx.type)) return "转账";
        return safe(tx.category);
    }

    private String subTitle(Transaction tx) {
        if ("transfer".equals(tx.type)) {
            return safe(tx.accountName) + " → " + safe(tx.targetAccountName);
        }
        return safe(tx.merchant) + " · " + safe(tx.accountName);
    }

    private String amount(Transaction tx) {
        if ("adjustment".equals(tx.type)) {
            return "调 " + PaymentParser.formatMoney(tx.amountCents);
        }
        if ("transfer".equals(tx.type)) {
            return "转 " + PaymentParser.formatMoney(tx.amountCents);
        }
        return ("income".equals(tx.type) ? "+" : "-") + PaymentParser.formatMoney(tx.amountCents);
    }

    private String billIcon(Transaction tx) {
        if ("income".equals(tx.type)) return "入";
        if ("transfer".equals(tx.type)) return "转";
        if ("adjustment".equals(tx.type)) return "平";
        return "支";
    }

    private int billColor(Transaction tx) {
        if ("income".equals(tx.type)) return Ui.ACCENT;
        if ("transfer".equals(tx.type)) return Ui.ACCENT;
        if ("adjustment".equals(tx.type)) return Color.rgb(92, 112, 140);
        return Ui.WARNING;
    }

    private int billBg(Transaction tx) {
        if ("income".equals(tx.type)) return Color.rgb(232, 245, 255);
        if ("transfer".equals(tx.type)) return Color.rgb(232, 245, 255);
        if ("adjustment".equals(tx.type)) return Color.rgb(238, 241, 245);
        return Color.rgb(255, 238, 241);
    }

    private View line() {
        View line = new View(this);
        line.setBackgroundColor(Ui.LINE);
        line.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 1)));
        return line;
    }

    private String safe(String value) {
        return value == null || value.length() == 0 ? "未填写" : value;
    }
}
