package com.personal.xiaomiledger;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
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
        Ui.applyBackground(this);
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
        root.setBackgroundColor(Ui.PAPER);
        setContentView(root);

        // ── top bar ──
        LinearLayout top = Ui.row(this);
        top.setBackgroundColor(Color.WHITE);
        top.setPadding(Ui.dp(this, 12), Ui.dp(this, 10), Ui.dp(this, 12), Ui.dp(this, 8));
        if (Build.VERSION.SDK_INT >= 21) {
            top.setElevation(Ui.dp(this, 1));
        }

        TextView close = Ui.text(this, "×", 32, Ui.MUTED, Typeface.NORMAL);
        close.setGravity(Gravity.CENTER);
        close.setOnClickListener(v -> finish());
        top.addView(close, new LinearLayout.LayoutParams(Ui.dp(this, 44), Ui.dp(this, 44)));

        top.addView(tab("支出", "expense"), new LinearLayout.LayoutParams(0, Ui.dp(this, 44), 1));
        top.addView(tab("收入", "income"), new LinearLayout.LayoutParams(0, Ui.dp(this, 44), 1));
        top.addView(tab("转账", "transfer"), new LinearLayout.LayoutParams(0, Ui.dp(this, 44), 1));

        root.addView(top);

        // ── category / transfer area ──
        categoryArea = new LinearLayout(this);
        categoryArea.setOrientation(LinearLayout.VERTICAL);
        categoryArea.setPadding(Ui.dp(this, 18), Ui.dp(this, 14), Ui.dp(this, 18), 0);
        root.addView(categoryArea, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        transferArea = new LinearLayout(this);
        transferArea.setOrientation(LinearLayout.VERTICAL);
        transferArea.setPadding(Ui.dp(this, 18), Ui.dp(this, 200), Ui.dp(this, 18), 0);
        root.addView(transferArea, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        // ── bottom panel ──
        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.VERTICAL);
        bottom.setBackgroundColor(Color.WHITE);
        bottom.setPadding(Ui.dp(this, 18), Ui.dp(this, 10), Ui.dp(this, 18), Ui.dp(this, 12));
        if (Build.VERSION.SDK_INT >= 21) {
            bottom.setElevation(Ui.dp(this, 4));
        }
        root.addView(bottom);

        // amount + note row
        LinearLayout amountRow = Ui.row(this);
        noteInput = new EditText(this);
        noteInput.setHint("备注...");
        noteInput.setSingleLine(true);
        noteInput.setTextSize(16);
        noteInput.setTextColor(Ui.INK);
        noteInput.setHintTextColor(Ui.MUTED);
        noteInput.setBackgroundColor(Color.TRANSPARENT);
        amountRow.addView(noteInput, new LinearLayout.LayoutParams(0, Ui.dp(this, 48), 1));

        amountView = Ui.text(this, "0.0", 30, Ui.WARNING, Typeface.NORMAL);
        amountView.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        amountRow.addView(amountView);
        bottom.addView(amountRow);

        // chips row
        LinearLayout chips = Ui.row(this);
        chips.setPadding(0, Ui.dp(this, 6), 0, Ui.dp(this, 6));
        accountSpinner = spinner(store.accountNames());
        chips.addView(accountSpinner, new LinearLayout.LayoutParams(0, Ui.dp(this, 42), 1));
        chips.addView(chipBtn("日期"), new LinearLayout.LayoutParams(0, Ui.dp(this, 42), 1));
        chips.addView(chipBtn("图片"), new LinearLayout.LayoutParams(0, Ui.dp(this, 42), 1));
        bottom.addView(chips);

        // keypad
        GridLayout keypad = new GridLayout(this);
        keypad.setColumnCount(4);
        keypad.setRowCount(4);
        String[] keys = {"1", "2", "3", "⌫", "4", "5", "6", "再记", "7", "8", "9", ".", "保存", "0", "00", "+"};
        for (String key : keys) {
            keypad.addView(keyButton(key));
        }
        bottom.addView(keypad);
    }

    private TextView tab(String label, String value) {
        TextView tab = Ui.text(this, label, 18, Ui.MUTED, Typeface.NORMAL);
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
        int color = "income".equals(type) ? Ui.SUCCESS : ("transfer".equals(type) ? Ui.TRANSFER : Ui.WARNING);
        amountView.setTextColor(color);
        if (saveKeyButton != null) applySaveColor(saveKeyButton);
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
            boolean active = value.equals(type);
            tab.setTextColor(active ? Ui.INK : Ui.MUTED);
            tab.setTypeface(Typeface.DEFAULT, active ? Typeface.BOLD : Typeface.NORMAL);
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
        cell.setPadding(0, Ui.dp(this, 4), 0, Ui.dp(this, 8));
        cell.setOnClickListener(v -> {
            category = name;
            renderCategories();
        });

        boolean selected = name.equals(category);
        int accent = "income".equals(type) ? Ui.SUCCESS : ("transfer".equals(type) ? Ui.TRANSFER : Ui.WARNING);
        int selColor = selected ? accent : Ui.MUTED;
        int selBg = selected ? Color.argb(30, Color.red(accent), Color.green(accent), Color.blue(accent)) : Color.TRANSPARENT;

        TextView icon = Ui.text(this, iconFor(name), 22, selColor, Typeface.BOLD);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(Ui.bg(this, selBg, 10));
        cell.addView(icon, new LinearLayout.LayoutParams(Ui.dp(this, 50), Ui.dp(this, 50)));

        TextView label = Ui.text(this, name, 13, selected ? accent : Ui.INK, selected ? Typeface.BOLD : Typeface.NORMAL);
        label.setGravity(Gravity.CENTER);
        cell.addView(label);

        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = getResources().getDisplayMetrics().widthPixels / 5 - Ui.dp(this, 8);
        lp.height = Ui.dp(this, 82);
        cell.setLayoutParams(lp);
        return cell;
    }

    private void renderTransfer() {
        transferArea.removeAllViews();
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackground(Ui.strokeBg(this, Color.WHITE, 12, Ui.LINE));
        box.setPadding(Ui.dp(this, 16), Ui.dp(this, 8), Ui.dp(this, 16), Ui.dp(this, 8));
        List<String> names = new ArrayList<>(store.accountNames());
        names.remove("未确认账户");
        fromSpinner = spinner(names);
        toSpinner = spinner(names);
        box.addView(labelWithSpinner("转出账户", fromSpinner));
        box.addView(Ui.line(this));
        box.addView(labelWithSpinner("转入账户", toSpinner));
        transferArea.addView(box);
    }

    private LinearLayout labelWithSpinner(String label, Spinner spinner) {
        LinearLayout row = Ui.row(this);
        row.setPadding(0, Ui.dp(this, 6), 0, Ui.dp(this, 6));
        row.addView(Ui.text(this, label, 16, Ui.MUTED, Typeface.NORMAL), new LinearLayout.LayoutParams(0, Ui.dp(this, 48), 1));
        row.addView(spinner, new LinearLayout.LayoutParams(0, Ui.dp(this, 48), 1));
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

    private TextView chipBtn(String label) {
        TextView chip = Ui.text(this, label, 14, Ui.MUTED, Typeface.NORMAL);
        chip.setGravity(Gravity.CENTER);
        chip.setBackground(Ui.bg(this, Ui.CHIP_BG, 8));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, Ui.dp(this, 42), 1);
        lp.setMargins(Ui.dp(this, 4), 0, Ui.dp(this, 4), 0);
        chip.setLayoutParams(lp);
        return chip;
    }

    private Button keyButton(String key) {
        Button button = new Button(this);
        button.setText(key);
        button.setTextSize(20);
        button.setTextColor(Ui.INK);
        button.setAllCaps(false);

        if ("保存".equals(key)) {
            saveKeyButton = button;
            applySaveColor(button);
        } else if ("⌫".equals(key) || "再记".equals(key)) {
            button.setTextColor(Ui.MUTED);
            button.setBackground(Ui.bg(this, Ui.CHIP_BG, 10));
        } else {
            button.setBackground(Ui.bg(this, Color.WHITE, 10));
        }
        button.setOnClickListener(v -> handleKey(key));

        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        int cols = 4;
        int margin = Ui.dp(this, 3);
        lp.width = (getResources().getDisplayMetrics().widthPixels - Ui.dp(this, 36)) / cols - margin * 2;
        lp.height = Ui.dp(this, 54);
        lp.setMargins(margin, margin, margin, margin);
        button.setLayoutParams(lp);
        return button;
    }

    private void applySaveColor(Button button) {
        int color = "income".equals(type) ? Ui.SUCCESS : ("transfer".equals(type) ? Ui.TRANSFER : Ui.WARNING);
        button.setTextColor(Color.WHITE);
        button.setBackground(Ui.bg(this, color, 10));
    }

    private List<String> currentCategories() {
        List<String> values = new ArrayList<>(store.categoryNames(type));
        if (!values.isEmpty()) return values;
        String[] fallback = "income".equals(type) ? incomeCategories : expenseCategories;
        for (String item : fallback) values.add(item);
        return values;
    }

    private String defaultCategory(String mode) {
        if ("transfer".equals(mode)) return "转账";
        List<String> values = new ArrayList<>(store.categoryNames(mode));
        if (!values.isEmpty()) return values.get(0);
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
        } else if ("+".equals(key)) {
            return;
        } else {
            if ("0".equals(amountText) && !".".equals(key) && !"00".equals(key)) {
                amountText = key;
            } else if ("00".equals(key) && "0".equals(amountText)) {
                return;
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
        if (value.contains("三餐") || value.contains("餐")) return "🍚";
        if (value.contains("零食")) return "🍪";
        if (value.contains("衣服") || value.contains("美妆")) return "👗";
        if (value.contains("交通") || value.contains("汽车") || value.contains("加油")) return "🚗";
        if (value.contains("旅行")) return "✈️";
        if (value.contains("孩子")) return "👶";
        if (value.contains("宠物")) return "🐶";
        if (value.contains("话费") || value.contains("网费")) return "📱";
        if (value.contains("烟酒")) return "🍷";
        if (value.contains("学习")) return "📚";
        if (value.contains("日用")) return "🧴";
        if (value.contains("住房") || value.contains("水电煤")) return "🏠";
        if (value.contains("医疗")) return "💊";
        if (value.contains("红包")) return "🧧";
        if (value.contains("娱乐")) return "🎮";
        if (value.contains("请客") || value.contains("送礼")) return "🎁";
        if (value.contains("电器") || value.contains("数码")) return "🖥️";
        if (value.contains("运动")) return "⚽";
        if (value.contains("工资")) return "💰";
        if (value.contains("生活费")) return "🏦";
        if (value.contains("外快")) return "💸";
        if (value.contains("股票") || value.contains("基金")) return "📈";
        if (value.contains("其它")) return "🗂️";
        return value.substring(0, 1);
    }
}
