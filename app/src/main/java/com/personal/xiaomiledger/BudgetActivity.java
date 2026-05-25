package com.personal.xiaomiledger;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class BudgetActivity extends Activity {
    private TransactionStore store;
    private LinearLayout root;
    private Spinner categorySpinner;
    private EditText amountInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.WHITE);
        store = new TransactionStore(this);
        buildShell();
        render();
    }

    private void buildShell() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Ui.PAPER);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(Ui.dp(this, 18), Ui.dp(this, 16), Ui.dp(this, 18), Ui.dp(this, 30));
        scroll.addView(root);
        setContentView(scroll);
    }

    private void render() {
        root.removeAllViews();
        root.addView(topBar());
        root.addView(Ui.spacer(this, 16));

        long[] range = TransactionStore.currentMonthRange();
        long expense = store.sumBetween("expense", range[0], range[1]);
        long income = store.sumBetween("income", range[0], range[1]);
        long totalBudget = findBudget("总预算", range);

        LinearLayout summary = Ui.card(this);
        summary.addView(Ui.text(this, "本月预算", 22, Ui.INK, Typeface.BOLD));
        summary.addView(Ui.spacer(this, 14));
        LinearLayout metrics = Ui.row(this);
        metrics.addView(metric("预算", totalBudget > 0 ? PaymentParser.formatMoney(totalBudget) : "未设置", Ui.INK));
        metrics.addView(metric("已支出", PaymentParser.formatMoney(expense), Ui.WARNING));
        metrics.addView(metric("结余", TransactionStore.formatMoneySigned(income - expense), Ui.ACCENT));
        summary.addView(metrics);
        root.addView(summary);

        LinearLayout form = Ui.card(this);
        form.addView(Ui.text(this, "添加或更新预算", 20, Ui.INK, Typeface.BOLD));
        form.addView(Ui.spacer(this, 10));
        categorySpinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, budgetCategories());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);
        categorySpinner.setBackground(Ui.strokeBg(this, Color.WHITE, 12, Ui.LINE));
        form.addView(label("范围"));
        form.addView(categorySpinner);
        amountInput = new EditText(this);
        amountInput.setHint("例如 1500");
        amountInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        amountInput.setTextColor(Ui.INK);
        amountInput.setHintTextColor(Ui.MUTED);
        amountInput.setBackground(Ui.strokeBg(this, Color.WHITE, 12, Ui.LINE));
        amountInput.setPadding(Ui.dp(this, 12), 0, Ui.dp(this, 12), 0);
        amountInput.setMinHeight(Ui.dp(this, 48));
        form.addView(label("预算金额"));
        form.addView(amountInput);
        form.addView(Ui.spacer(this, 12));
        Button save = new Button(this);
        save.setText("保存预算");
        save.setTextColor(Color.WHITE);
        save.setAllCaps(false);
        save.setBackground(Ui.bg(this, Ui.ACCENT, 14));
        save.setOnClickListener(v -> saveBudget());
        form.addView(save, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 50)));
        root.addView(form);

        LinearLayout list = Ui.card(this);
        list.addView(Ui.text(this, "预算明细", 20, Ui.INK, Typeface.BOLD));
        list.addView(Ui.spacer(this, 8));
        boolean hasBudget = false;
        for (Budget budget : store.budgets()) {
            if (budget.startAt == range[0] && budget.endAt == range[1]) {
                hasBudget = true;
                list.addView(budgetRow(budget, range));
            }
        }
        if (!hasBudget) {
            TextView empty = Ui.text(this, "还没有设置预算。", 16, Ui.MUTED, Typeface.NORMAL);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, Ui.dp(this, 26), 0, Ui.dp(this, 22));
            list.addView(empty);
        }
        root.addView(list);
    }

    private LinearLayout topBar() {
        LinearLayout top = Ui.row(this);
        TextView back = Ui.text(this, "‹", 38, Ui.INK, Typeface.NORMAL);
        back.setGravity(Gravity.CENTER);
        back.setOnClickListener(v -> finish());
        top.addView(back, new LinearLayout.LayoutParams(Ui.dp(this, 42), Ui.dp(this, 42)));
        top.addView(Ui.text(this, "预算管理", 25, Ui.INK, Typeface.BOLD), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        return top;
    }

    private TextView metric(String label, String value, int color) {
        TextView view = Ui.text(this, label + "\n" + value, 17, color, Typeface.BOLD);
        view.setGravity(Gravity.CENTER);
        view.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        return view;
    }

    private TextView label(String text) {
        TextView label = Ui.text(this, text, 13, Ui.MUTED, Typeface.BOLD);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, Ui.dp(this, 10), 0, Ui.dp(this, 4));
        label.setLayoutParams(params);
        return label;
    }

    private LinearLayout budgetRow(Budget budget, long[] range) {
        long spent = "总预算".equals(budget.category)
                ? store.sumBetween("expense", range[0], range[1])
                : store.categoryExpenseBetween(budget.category, range[0], range[1]);
        int percent = budget.amountCents <= 0 ? 0 : (int) Math.min(100, spent * 100 / budget.amountCents);
        boolean over = spent > budget.amountCents;

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 10));

        LinearLayout row = Ui.row(this);
        row.addView(Ui.text(this, budget.category, 17, Ui.INK, Typeface.BOLD), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView amount = Ui.text(this, PaymentParser.formatMoney(spent) + " / " + PaymentParser.formatMoney(budget.amountCents), 16, over ? Ui.WARNING : Ui.MUTED, Typeface.BOLD);
        amount.setGravity(Gravity.END);
        row.addView(amount);
        box.addView(row);
        box.addView(Ui.spacer(this, 7));

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setBackground(Ui.bg(this, Color.rgb(238, 241, 245), 999));
        View filled = new View(this);
        filled.setBackground(Ui.bg(this, over ? Ui.WARNING : Ui.ACCENT, 999));
        bar.addView(filled, new LinearLayout.LayoutParams(0, Ui.dp(this, 8), Math.max(1, percent)));
        View rest = new View(this);
        bar.addView(rest, new LinearLayout.LayoutParams(0, Ui.dp(this, 8), Math.max(0, 100 - percent)));
        box.addView(bar);
        return box;
    }

    private List<String> budgetCategories() {
        ArrayList<String> values = new ArrayList<>();
        values.add("总预算");
        values.addAll(store.categoryNames("expense"));
        return values;
    }

    private long findBudget(String category, long[] range) {
        for (Budget budget : store.budgets()) {
            if (category.equals(budget.category) && budget.startAt == range[0] && budget.endAt == range[1]) {
                return budget.amountCents;
            }
        }
        return 0;
    }

    private void saveBudget() {
        Long cents = parseAmount(amountInput.getText().toString());
        if (cents == null || cents <= 0) {
            Toast.makeText(this, "请输入正确预算金额", Toast.LENGTH_SHORT).show();
            return;
        }
        store.saveMonthlyBudget(String.valueOf(categorySpinner.getSelectedItem()), cents);
        Toast.makeText(this, "已保存预算", Toast.LENGTH_SHORT).show();
        render();
    }

    private Long parseAmount(String value) {
        try {
            return new BigDecimal(value.trim()).movePointRight(2).longValue();
        } catch (RuntimeException ignored) {
            return null;
        }
    }
}
