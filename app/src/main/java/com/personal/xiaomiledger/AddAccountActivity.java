package com.personal.xiaomiledger;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class AddAccountActivity extends Activity {
    private EditText nameInput;
    private Spinner typeSpinner;
    private TransactionStore store;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Ui.applyBackground(this);
        store = new TransactionStore(this);
        buildUi();
    }

    private void buildUi() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(Ui.PAPER);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(Ui.dp(this, 20), Ui.dp(this, 22), Ui.dp(this, 20), Ui.dp(this, 24));
        scrollView.addView(root);

        root.addView(Ui.text(this, "添加资金账户", 30, Ui.INK, Typeface.BOLD));
        root.addView(Ui.text(this, "名称建议和银行通知里的名字一致，例如建设银行、邮储银行。", 14, Ui.MUTED, Typeface.NORMAL));
        root.addView(Ui.spacer(this, 18));

        LinearLayout card = Ui.card(this);
        root.addView(card);

        card.addView(label("账户名称"));
        nameInput = new EditText(this);
        nameInput.setHint("例如 建设银行");
        nameInput.setTextColor(Ui.INK);
        nameInput.setHintTextColor(Ui.MUTED);
        nameInput.setSingleLine(true);
        nameInput.setBackground(Ui.strokeBg(this, Color.WHITE, 12, Ui.LINE));
        nameInput.setPadding(Ui.dp(this, 12), 0, Ui.dp(this, 12), 0);
        nameInput.setMinHeight(Ui.dp(this, 48));
        card.addView(nameInput);

        card.addView(label("账户类型"));
        typeSpinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                new String[]{"银行卡", "虚拟账户", "投资账户", "现金"});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeSpinner.setAdapter(adapter);
        typeSpinner.setBackground(Ui.strokeBg(this, Color.WHITE, 12, Ui.LINE));
        card.addView(typeSpinner);

        Button save = new Button(this);
        save.setText("保存账户");
        save.setTextColor(Color.WHITE);
        save.setBackground(Ui.bg(this, Ui.ACCENT, 14));
        save.setOnClickListener(v -> save());
        root.addView(save, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 50)));

        setContentView(scrollView);
    }

    private TextView label(String text) {
        TextView label = Ui.text(this, text, 13, Ui.MUTED, Typeface.BOLD);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, Ui.dp(this, 12), 0, Ui.dp(this, 4));
        label.setLayoutParams(params);
        return label;
    }

    private void save() {
        String name = nameInput.getText().toString().trim();
        if (name.length() == 0) {
            Toast.makeText(this, "请输入账户名称", Toast.LENGTH_SHORT).show();
            return;
        }
        store.addAccount(name, String.valueOf(typeSpinner.getSelectedItem()));
        Toast.makeText(this, "已添加账户", Toast.LENGTH_SHORT).show();
        finish();
    }
}
