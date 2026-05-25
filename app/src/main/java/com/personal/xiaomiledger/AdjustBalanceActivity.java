package com.personal.xiaomiledger;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
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
import java.util.List;

public class AdjustBalanceActivity extends Activity {
    private Spinner accountSpinner;
    private EditText balanceInput;
    private TransactionStore store;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Ui.PAPER);
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

        root.addView(Ui.text(this, "调整账户余额", 30, Ui.INK, Typeface.BOLD));
        root.addView(Ui.text(this, "用于录入当前真实余额，例如银行卡、微信零钱、京东金融余额。", 14, Ui.MUTED, Typeface.NORMAL));
        root.addView(Ui.spacer(this, 18));

        LinearLayout card = Ui.card(this);
        root.addView(card);

        card.addView(label("账户"));
        List<String> accountNames = store.accountNames();
        accountNames.remove("未确认账户");
        accountSpinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, accountNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        accountSpinner.setAdapter(adapter);
        accountSpinner.setBackground(Ui.strokeBg(this, Color.WHITE, 12, Ui.LINE));
        card.addView(accountSpinner);

        card.addView(label("当前真实余额"));
        balanceInput = new EditText(this);
        balanceInput.setHint("例如 827.47");
        balanceInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
        balanceInput.setTextColor(Ui.INK);
        balanceInput.setHintTextColor(Ui.MUTED);
        balanceInput.setBackground(Ui.strokeBg(this, Color.WHITE, 12, Ui.LINE));
        balanceInput.setPadding(Ui.dp(this, 12), 0, Ui.dp(this, 12), 0);
        balanceInput.setMinHeight(Ui.dp(this, 48));
        card.addView(balanceInput);

        Button save = new Button(this);
        save.setText("保存余额");
        save.setTextColor(Color.WHITE);
        save.setBackground(Ui.bg(this, Ui.ACCENT, 14));
        save.setOnClickListener(v -> save());
        root.addView(save, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 50)));

        root.addView(Ui.spacer(this, 10));
        Button cancel = new Button(this);
        cancel.setText("取消");
        cancel.setTextColor(Ui.INK);
        cancel.setBackground(Ui.strokeBg(this, Color.WHITE, 14, Ui.LINE));
        cancel.setOnClickListener(v -> finish());
        root.addView(cancel, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 48)));

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
        Long cents = parseAmount(balanceInput.getText().toString());
        if (cents == null) {
            Toast.makeText(this, "请输入正确余额", Toast.LENGTH_SHORT).show();
            return;
        }
        String account = String.valueOf(accountSpinner.getSelectedItem());
        long delta = store.setAccountBalance(account, cents);
        Toast.makeText(this, "已平账，变动 " + TransactionStore.formatMoneySigned(delta) + " 元", Toast.LENGTH_SHORT).show();
        finish();
    }

    private Long parseAmount(String value) {
        try {
            BigDecimal yuan = new BigDecimal(value.trim());
            return yuan.movePointRight(2).longValue();
        } catch (RuntimeException ignored) {
            return null;
        }
    }
}
