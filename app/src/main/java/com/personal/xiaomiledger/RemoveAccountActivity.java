package com.personal.xiaomiledger;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;

public class RemoveAccountActivity extends Activity {
    private TransactionStore store;
    private LinearLayout list;

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

        root.addView(Ui.text(this, "移除支付路径", 30, Ui.INK, Typeface.BOLD));
        root.addView(Ui.text(this, "只会从资金列表和自动匹配里隐藏账户，不删除历史账单。", 14, Ui.MUTED, Typeface.NORMAL));
        root.addView(Ui.spacer(this, 18));

        list = Ui.card(this);
        root.addView(list);
        setContentView(scrollView);
        renderAccounts();
    }

    private void renderAccounts() {
        list.removeAllViews();
        List<Account> accounts = store.accounts();
        if (accounts.isEmpty()) {
            list.addView(Ui.text(this, "没有可移除的账户。", 16, Ui.MUTED, Typeface.NORMAL));
            return;
        }
        for (int i = 0; i < accounts.size(); i++) {
            Account account = accounts.get(i);
            list.addView(accountRow(account));
            if (i < accounts.size() - 1) {
                list.addView(line());
            }
        }
    }

    private LinearLayout accountRow(Account account) {
        LinearLayout row = Ui.row(this);
        row.setPadding(0, Ui.dp(this, 13), 0, Ui.dp(this, 13));
        TextView name = Ui.text(this, account.name + "\n" + PaymentParser.formatMoney(account.balanceCents), 18, Ui.INK, Typeface.NORMAL);
        row.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        TextView remove = Ui.text(this, "-", 28, Color.WHITE, Typeface.BOLD);
        remove.setGravity(Gravity.CENTER);
        remove.setBackground(Ui.bg(this, Ui.WARNING, 999));
        remove.setOnClickListener(v -> confirmRemove(account));
        row.addView(remove, new LinearLayout.LayoutParams(Ui.dp(this, 42), Ui.dp(this, 42)));
        return row;
    }

    private void confirmRemove(Account account) {
        new AlertDialog.Builder(this)
                .setTitle("移除 " + account.name + "？")
                .setMessage("这只会隐藏支付路径，不会删除历史账单。以后重新添加同名账户可以恢复显示。")
                .setNegativeButton("取消", null)
                .setPositiveButton("移除", (dialog, which) -> {
                    if (store.hideAccount(account.name)) {
                        Toast.makeText(this, "已移除支付路径", Toast.LENGTH_SHORT).show();
                        renderAccounts();
                    } else {
                        Toast.makeText(this, "移除失败", Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    private android.view.View line() {
        android.view.View line = new android.view.View(this);
        line.setBackgroundColor(Ui.LINE);
        line.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
        return line;
    }
}
