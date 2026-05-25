package com.personal.xiaomiledger;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;

public class CategoryManageActivity extends Activity {
    private TransactionStore store;
    private LinearLayout root;
    private Spinner typeSpinner;
    private EditText nameInput;

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
        root.addView(addForm());
        root.addView(categoryCard("支出分类", "expense"));
        root.addView(categoryCard("收入分类", "income"));
    }

    private LinearLayout topBar() {
        LinearLayout top = Ui.row(this);
        TextView back = Ui.text(this, "‹", 38, Ui.INK, Typeface.NORMAL);
        back.setGravity(Gravity.CENTER);
        back.setOnClickListener(v -> finish());
        top.addView(back, new LinearLayout.LayoutParams(Ui.dp(this, 42), Ui.dp(this, 42)));
        top.addView(Ui.text(this, "分类管理", 25, Ui.INK, Typeface.BOLD), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        return top;
    }

    private LinearLayout addForm() {
        LinearLayout card = Ui.card(this);
        card.addView(Ui.text(this, "新增分类", 20, Ui.INK, Typeface.BOLD));
        card.addView(Ui.spacer(this, 10));
        typeSpinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new String[]{"支出", "收入"});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeSpinner.setAdapter(adapter);
        typeSpinner.setBackground(Ui.strokeBg(this, Color.WHITE, 12, Ui.LINE));
        card.addView(typeSpinner);
        card.addView(Ui.spacer(this, 10));
        nameInput = new EditText(this);
        nameInput.setHint("分类名称");
        nameInput.setSingleLine(true);
        nameInput.setTextColor(Ui.INK);
        nameInput.setHintTextColor(Ui.MUTED);
        nameInput.setBackground(Ui.strokeBg(this, Color.WHITE, 12, Ui.LINE));
        nameInput.setPadding(Ui.dp(this, 12), 0, Ui.dp(this, 12), 0);
        nameInput.setMinHeight(Ui.dp(this, 48));
        card.addView(nameInput);
        card.addView(Ui.spacer(this, 12));
        Button add = new Button(this);
        add.setText("添加");
        add.setAllCaps(false);
        add.setTextColor(Color.WHITE);
        add.setBackground(Ui.bg(this, Ui.ACCENT, 14));
        add.setOnClickListener(v -> addCategory());
        card.addView(add, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 50)));
        return card;
    }

    private LinearLayout categoryCard(String title, String type) {
        LinearLayout card = Ui.card(this);
        card.addView(Ui.text(this, title, 20, Ui.INK, Typeface.BOLD));
        card.addView(Ui.spacer(this, 12));
        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(3);
        List<Category> categories = store.categories(type);
        for (Category category : categories) {
            grid.addView(categoryChip(category.name));
        }
        card.addView(grid);
        return card;
    }

    private TextView categoryChip(String name) {
        TextView chip = Ui.text(this, name, 16, Ui.INK, Typeface.BOLD);
        chip.setGravity(Gravity.CENTER);
        chip.setBackground(Ui.bg(this, Color.rgb(248, 250, 252), 999));
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = (getResources().getDisplayMetrics().widthPixels - Ui.dp(this, 70)) / 3;
        lp.height = Ui.dp(this, 44);
        lp.setMargins(Ui.dp(this, 3), Ui.dp(this, 4), Ui.dp(this, 3), Ui.dp(this, 4));
        chip.setLayoutParams(lp);
        return chip;
    }

    private void addCategory() {
        String name = nameInput.getText().toString().trim();
        if (name.length() == 0) {
            Toast.makeText(this, "请输入分类名称", Toast.LENGTH_SHORT).show();
            return;
        }
        String type = typeSpinner.getSelectedItemPosition() == 1 ? "income" : "expense";
        store.addCategory(name, type);
        Toast.makeText(this, "已添加分类", Toast.LENGTH_SHORT).show();
        render();
    }
}
