package com.personal.xiaomiledger;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class AddBillActivity extends Activity {
    private final String[] expenseCategories = {"三餐", "零食", "衣服", "交通", "旅行", "孩子", "宠物", "话费网费", "烟酒", "学习", "日用品", "住房", "美妆", "医疗", "发红包", "汽车/加油", "娱乐", "请客送礼", "电器数码", "运动", "其它", "水电煤"};
    private final String[] incomeCategories = {"工资", "生活费", "收红包", "外快", "股票基金", "其它"};
    private TransactionStore store;
    private LinearLayout root;
    private LinearLayout categoryArea;
    private LinearLayout transferArea;
    private TextView amountView;
    private EditText noteInput;
    private Spinner accountSpinner;
    private Spinner fromSpinner;
    private Spinner toSpinner;
    private Button saveKeyButton;
    private String type = "expense";
    private String category = "三餐";
    private String amountText = "0";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.WHITE);
        store = new TransactionStore(this);
        String initialMode = getIntent().getStringExtra("mode");
        if ("income".equals(initialMode) || "transfer".equals(initialMode) || "expense".equals(initialMode)) {
            type = initialMode;
            category = defaultCategory(type);
        }
        buildUi();
        renderMode();
    }

    private void buildUi() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);
        setContentView(root);

        LinearLayout top = Ui.row(this);
        top.setPadding(Ui.dp(this, 18), Ui.dp(this, 18), Ui.dp(this, 18), Ui.dp(this, 10));
        TextView close = Ui.text(this, "×", 36, Color.BLACK, Typeface.NORMAL);
        close.setGravity(Gravity.CENTER);
        close.setOnClickListener(v -> finish());
        top.addView(close, new LinearLayout.LayoutParams(Ui.dp(this, 54), Ui.dp(this, 54)));
        top.addView(tab("支出", "expense"), new LinearLayout.LayoutParams(0, Ui.dp(this, 54), 1));
        top.addView(tab("收入", "income"), new LinearLayout.LayoutParams(0, Ui.dp(this, 54), 1));
        top.addView(tab("转账", "transfer"), new LinearLayout.LayoutParams(0, Ui.dp(this, 54), 1));
        TextView add = Ui.text(this, "+", 25, Color.WHITE, Typeface.BOLD);
        add.setGravity(Gravity.CENTER);
        add.setBackground(Ui.bg(this, Color.BLACK, 11));
        top.addView(add, new LinearLayout.LayoutParams(Ui.dp(this, 38), Ui.dp(this, 38)));
        root.addView(top);
        root.addView(line());

        categoryArea = new LinearLayout(this);
        categoryArea.setOrientation(LinearLayout.VERTICAL);
        categoryArea.setPadding(Ui.dp(this, 18), Ui.dp(this, 18), Ui.dp(this, 18), 0);
        root.addView(categoryArea, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        transferArea = new LinearLayout(this);
        transferArea.setOrientation(LinearLayout.VERTICAL);
        transferArea.setPadding(Ui.dp(this, 18), Ui.dp(this, 250), Ui.dp(this, 18), 0);
        root.addView(transferArea, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.VERTICAL);
        bottom.setBackgroundColor(Ui.PAPER);
        bottom.setPadding(Ui.dp(this, 18), Ui.dp(this, 14), Ui.dp(this, 18), Ui.dp(this, 16));
        root.addView(bottom);

        LinearLayout amountRow = Ui.row(this);
        noteInput = new EditText(this);
        noteInput.setHint("点此输入备注...");
        noteInput.setSingleLine(true);
        noteInput.setTextSize(18);
        noteInput.setTextColor(Ui.INK);
        noteInput.setHintTextColor(Ui.MUTED);
        noteInput.setBackgroundColor(Color.TRANSPARENT);
        amountRow.addView(noteInput, new LinearLayout.LayoutParams(0, Ui.dp(this, 54), 1));
        amountView = Ui.text(this, "0.0", 34, Ui.WARNING, Typeface.NORMAL);
        amountView.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        amountRow.addView(amountView);
        bottom.addView(amountRow);

        LinearLayout chips = Ui.row(this);
        accountSpinner = spinner(store.accountNames());
        chips.addView(accountSpinner, new LinearLayout.LayoutParams(0, Ui.dp(this, 46), 1));
        chips.addView(chip("今天 23:04"), new LinearLayout.LayoutParams(0, Ui.dp(this, 46), 1));
        chips.addView(chip("图片"), new LinearLayout.LayoutParams(0, Ui.dp(this, 46), 1));
        chips.addView(chip("⚑"), new LinearLayout.LayoutParams(0, Ui.dp(this, 46), 1));
        bottom.addView(chips);

        GridLayout keypad = new GridLayout(this);
        keypad.setColumnCount(4);
        keypad.setRowCount(4);
        keypad.setPadding(0, Ui.dp(this, 10), 0, 0);
        String[] keys = {"1", "2", "3", "⌫", "4", "5", "6", "-", "7", "8", "9", "+", "再记", "0", ".", "保存"};
        for (String key : keys) {
            keypad.addView(keyButton(key));
        }
        bottom.addView(keypad);
    }

    private TextView tab(String label, String value) {
        TextView tab = Ui.text(this, label, 24, Ui.MUTED, Typeface.NORMAL);
        tab.setGravity(Gravity.CENTER);
        tab.setOnClickListener(v -> {
            type = value;
            category = defaultCategory(type);
            renderMode();
        });
        return tab;
    }

    private void renderMode() {
        refreshTabs(root);
        amountView.setTextColor("income".equals(type) ? Color.rgb(44, 188, 128) : ("transfer".equals(type) ? Ui.ACCENT : Ui.WARNING));
        if (saveKeyButton != null) {
            applySaveColor(saveKeyButton);
        }
        categoryArea.setVisibility("transfer".equals(type) ? View.GONE : View.VISIBLE);
        transferArea.setVisibility("transfer".equals(type) ? View.VISIBLE : View.GONE);
        renderCategories();
        renderTransfer();
        refreshAmount();
    }

    private void refreshTabs(ViewGroup parent) {
        LinearLayout top = (LinearLayout) parent.getChildAt(0);
        for (int i = 1; i <= 3; i++) {
            TextView tab = (TextView) top.getChildAt(i);
            String value = i == 1 ? "expense" : (i == 2 ? "income" : "transfer");
            tab.setTextColor(value.equals(type) ? Color.BLACK : Ui.MUTED);
            tab.setTypeface(Typeface.DEFAULT, value.equals(type) ? Typeface.BOLD : Typeface.NORMAL);
        }
    }

    private void renderCategories() {
        categoryArea.removeAllViews();
        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(5);
        List<String> categories = currentCategories();
        if (!categories.contains(category)) {
            category = categories.isEmpty() ? "其它" : categories.get(0);
        }
        for (String item : categories) {
            grid.addView(categoryCell(item));
        }
        categoryArea.addView(grid);
    }

    private LinearLayout categoryCell(String name) {
        LinearLayout cell = new LinearLayout(this);
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setGravity(Gravity.CENTER);
        cell.setPadding(0, Ui.dp(this, 6), 0, Ui.dp(this, 12));
        cell.setOnClickListener(v -> {
            category = name;
            renderCategories();
        });

        boolean selected = name.equals(category);
        TextView icon = Ui.text(this, iconFor(name), 24, selected ? Ui.ACCENT : Ui.MUTED, Typeface.BOLD);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(Ui.bg(this, selected ? Color.rgb(232, 245, 255) : Color.WHITE, 999));
        cell.addView(icon, new LinearLayout.LayoutParams(Ui.dp(this, 54), Ui.dp(this, 54)));

        TextView label = Ui.text(this, name, 15, selected ? Ui.ACCENT : Color.BLACK, Typeface.NORMAL);
        label.setGravity(Gravity.CENTER);
        cell.addView(label);

        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = getResources().getDisplayMetrics().widthPixels / 5 - Ui.dp(this, 8);
        lp.height = Ui.dp(this, 92);
        cell.setLayoutParams(lp);
        return cell;
    }

    private void renderTransfer() {
        transferArea.removeAllViews();
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackground(Ui.strokeBg(this, Color.WHITE, 12, Ui.LINE));
        List<String> names = new ArrayList<>(store.accountNames());
        names.remove("未确认账户");
        fromSpinner = spinner(names);
        toSpinner = spinner(names);
        box.addView(labelWithSpinner("转出账户", fromSpinner));
        box.addView(line());
        box.addView(labelWithSpinner("转入账户", toSpinner));
        transferArea.addView(box);
    }

    private LinearLayout labelWithSpinner(String label, Spinner spinner) {
        LinearLayout row = Ui.row(this);
        row.setPadding(Ui.dp(this, 16), Ui.dp(this, 8), Ui.dp(this, 16), Ui.dp(this, 8));
        row.addView(Ui.text(this, label, 20, Ui.MUTED, Typeface.NORMAL), new LinearLayout.LayoutParams(0, Ui.dp(this, 54), 1));
        row.addView(spinner, new LinearLayout.LayoutParams(0, Ui.dp(this, 54), 1));
        return row;
    }

    private Spinner spinner(List<String> values) {
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, values);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setBackground(Ui.bg(this, Color.WHITE, 999));
        return spinner;
    }

    private TextView chip(String label) {
        TextView chip = Ui.text(this, label, 16, Color.BLACK, Typeface.NORMAL);
        chip.setGravity(Gravity.CENTER);
        chip.setBackground(Ui.bg(this, Color.WHITE, 999));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, Ui.dp(this, 46), 1);
        lp.setMargins(Ui.dp(this, 4), 0, Ui.dp(this, 4), 0);
        chip.setLayoutParams(lp);
        return chip;
    }

    private Button keyButton(String key) {
        Button button = new Button(this);
        button.setText(key);
        button.setTextSize(22);
        button.setTextColor(Color.BLACK);
        button.setAllCaps(false);
        if ("保存".equals(key)) {
            saveKeyButton = button;
            applySaveColor(button);
        } else {
            button.setBackground(Ui.bg(this, Color.WHITE, 12));
        }
        button.setOnClickListener(v -> handleKey(key));
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = (getResources().getDisplayMetrics().widthPixels - Ui.dp(this, 36)) / 4 - Ui.dp(this, 8);
        lp.height = Ui.dp(this, 62);
        lp.setMargins(Ui.dp(this, 4), Ui.dp(this, 4), Ui.dp(this, 4), Ui.dp(this, 4));
        button.setLayoutParams(lp);
        return button;
    }

    private void applySaveColor(Button button) {
        int color = "income".equals(type) ? Color.rgb(44, 188, 128) : ("transfer".equals(type) ? Ui.ACCENT : Ui.WARNING);
        button.setTextColor(Color.WHITE);
        button.setBackground(Ui.bg(this, color, 12));
    }

    private List<String> currentCategories() {
        List<String> values = new ArrayList<>(store.categoryNames(type));
        if (!values.isEmpty()) {
            return values;
        }
        String[] fallback = "income".equals(type) ? incomeCategories : expenseCategories;
        for (String item : fallback) {
            values.add(item);
        }
        return values;
    }

    private String defaultCategory(String mode) {
        if ("transfer".equals(mode)) {
            return "转账";
        }
        List<String> values = new ArrayList<>(store.categoryNames(mode));
        if (!values.isEmpty()) {
            return values.get(0);
        }
        return "income".equals(mode) ? "工资" : "三餐";
    }

    private void handleKey(String key) {
        if ("保存".equals(key)) {
            save();
        } else if ("⌫".equals(key)) {
            amountText = amountText.length() <= 1 ? "0" : amountText.substring(0, amountText.length() - 1);
            refreshAmount();
        } else if ("再记".equals(key)) {
            amountText = "0";
            noteInput.setText("");
            refreshAmount();
        } else if ("+".equals(key) || "-".equals(key)) {
            return;
        } else {
            if ("0".equals(amountText) && !".".equals(key)) {
                amountText = key;
            } else if (".".equals(key) && amountText.contains(".")) {
                return;
            } else {
                amountText += key;
            }
            refreshAmount();
        }
    }

    private void refreshAmount() {
        amountView.setText(amountText.contains(".") ? amountText : amountText + ".0");
    }

    private void save() {
        Long cents = parseAmount(amountText);
        if (cents == null || cents <= 0) {
            Toast.makeText(this, "请输入正确金额", Toast.LENGTH_SHORT).show();
            return;
        }
        String note = noteInput.getText().toString().trim();
        long result;
        if ("transfer".equals(type)) {
            result = store.transfer(String.valueOf(fromSpinner.getSelectedItem()), String.valueOf(toSpinner.getSelectedItem()), cents, note);
        } else {
            Transaction tx = new Transaction();
            tx.type = type;
            tx.amountCents = cents;
            tx.sourceApp = "手动";
            tx.accountName = String.valueOf(accountSpinner.getSelectedItem());
            tx.category = category;
            tx.merchant = category;
            tx.note = note;
            tx.rawText = "";
            tx.notificationKey = "manual:" + System.currentTimeMillis();
            tx.occurredAt = System.currentTimeMillis();
            tx.createdAt = System.currentTimeMillis();
            result = store.insert(tx);
        }
        if (result == -1) {
            Toast.makeText(this, "保存失败，请检查账户", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private Long parseAmount(String value) {
        try {
            return new BigDecimal(value.trim()).movePointRight(2).longValue();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private String iconFor(String value) {
        if (value.contains("餐")) return "♨";
        if (value.contains("交通")) return "▣";
        if (value.contains("工资")) return "¥";
        if (value.contains("股票")) return "↗";
        if (value.contains("红包")) return "囍";
        if (value.contains("其它")) return "▦";
        return value.substring(0, 1);
    }

    private View line() {
        View line = new View(this);
        line.setBackgroundColor(Ui.LINE);
        line.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 1)));
        return line;
    }
}
