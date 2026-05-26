package com.personal.xiaomiledger;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.math.BigDecimal;

public class EditTransactionActivity extends Activity {
    private static final String EXTRA_TYPE = "type";
    private static final String EXTRA_AMOUNT_CENTS = "amount_cents";
    private static final String EXTRA_SOURCE_APP = "source_app";
    private static final String EXTRA_MERCHANT = "merchant";
    private static final String EXTRA_RAW_TEXT = "raw_text";
    private static final String EXTRA_NOTIFICATION_KEY = "notification_key";
    private static final String EXTRA_OCCURRED_AT = "occurred_at";
    private static final String EXTRA_TITLE = "title";
    private static final String EXTRA_TRANSACTION_ID = "transaction_id";

    private EditText amountInput;
    private EditText merchantInput;
    private EditText noteInput;
    private Spinner typeSpinner;
    private Spinner accountSpinner;
    private Spinner categorySpinner;
    private EditText sourceInput;
    private TransactionStore store;
    private Transaction editingTransaction;

    static Intent intentForPayment(Context context, ParsedPayment payment) {
        Intent intent = new Intent(context, EditTransactionActivity.class);
        intent.putExtra(EXTRA_TYPE, payment.type);
        intent.putExtra(EXTRA_AMOUNT_CENTS, payment.amountCents);
        intent.putExtra(EXTRA_SOURCE_APP, payment.sourceApp);
        intent.putExtra(EXTRA_MERCHANT, payment.merchant);
        intent.putExtra(EXTRA_RAW_TEXT, payment.rawText);
        intent.putExtra(EXTRA_NOTIFICATION_KEY, payment.notificationKey);
        intent.putExtra(EXTRA_OCCURRED_AT, payment.occurredAt);
        return intent;
    }

    static Intent intentForManual(Context context, String type) {
        Intent intent = new Intent(context, EditTransactionActivity.class);
        intent.putExtra(EXTRA_TYPE, type);
        intent.putExtra(EXTRA_SOURCE_APP, "手动");
        intent.putExtra(EXTRA_OCCURRED_AT, System.currentTimeMillis());
        intent.putExtra(EXTRA_TITLE, "income".equals(type) ? "记一笔收入" : "记一笔支出");
        return intent;
    }

    static Intent intentForTransaction(Context context, long transactionId) {
        Intent intent = new Intent(context, EditTransactionActivity.class);
        intent.putExtra(EXTRA_TRANSACTION_ID, transactionId);
        intent.putExtra(EXTRA_TITLE, "修改账单");
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Ui.applyBackground(this);
        store = new TransactionStore(this);
        buildUi();
        fillFromIntent();
    }

    private void buildUi() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(Ui.PAPER);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(Ui.dp(this, 20), Ui.dp(this, 22), Ui.dp(this, 20), Ui.dp(this, 24));
        scrollView.addView(root);

        String title = getIntent().getStringExtra(EXTRA_TITLE);
        if (title == null || title.length() == 0) {
            title = "确认记账";
        }
        root.addView(Ui.text(this, title, 30, Ui.INK, Typeface.BOLD));
        root.addView(Ui.text(this, "金额会同步到所选账户余额，未确认账户不会改资产。", 14, Ui.MUTED, Typeface.NORMAL));
        root.addView(Ui.spacer(this, 18));

        LinearLayout card = Ui.card(this);
        root.addView(card);

        amountInput = editText("金额");
        amountInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        card.addView(label("金额"));
        card.addView(amountInput);

        typeSpinner = spinner(new String[]{"支出", "收入"});
        card.addView(label("类型"));
        card.addView(typeSpinner);

        accountSpinner = spinner(store.accountNames().toArray(new String[0]));
        card.addView(label("扣款/入账账户"));
        card.addView(accountSpinner);

        categorySpinner = spinner(categoryValues("expense"));
        card.addView(label("分类"));
        card.addView(categorySpinner);
        typeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                setCategoryOptions(position == 1 ? "income" : "expense");
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        sourceInput = editText("来源 App");
        card.addView(label("来源"));
        card.addView(sourceInput);

        merchantInput = editText("商户或对象");
        card.addView(label("商户/对象"));
        card.addView(merchantInput);

        noteInput = editText("备注");
        noteInput.setMinLines(3);
        noteInput.setGravity(Gravity.TOP | Gravity.START);
        card.addView(label("备注"));
        card.addView(noteInput);

        Button saveButton = new Button(this);
        saveButton.setText("保存");
        saveButton.setTextColor(Color.WHITE);
        saveButton.setBackground(Ui.bg(this, Ui.ACCENT, 14));
        saveButton.setOnClickListener(v -> save());
        root.addView(saveButton, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                Ui.dp(this, 50)));

        root.addView(Ui.spacer(this, 10));
        Button cancelButton = new Button(this);
        cancelButton.setText("取消");
        cancelButton.setTextColor(Ui.INK);
        cancelButton.setBackground(Ui.strokeBg(this, Color.WHITE, 14, Ui.LINE));
        cancelButton.setOnClickListener(v -> finish());
        root.addView(cancelButton, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                Ui.dp(this, 48)));

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

    private EditText editText(String hint) {
        EditText editText = new EditText(this);
        editText.setHint(hint);
        editText.setSingleLine(false);
        editText.setTextColor(Ui.INK);
        editText.setHintTextColor(Ui.MUTED);
        editText.setBackground(Ui.strokeBg(this, Color.WHITE, 12, Ui.LINE));
        editText.setPadding(Ui.dp(this, 12), 0, Ui.dp(this, 12), 0);
        editText.setMinHeight(Ui.dp(this, 48));
        return editText;
    }

    private Spinner spinner(String[] values) {
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, values);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setBackground(Ui.strokeBg(this, Color.WHITE, 12, Ui.LINE));
        return spinner;
    }

    private void fillFromIntent() {
        Intent intent = getIntent();
        long transactionId = intent.getLongExtra(EXTRA_TRANSACTION_ID, 0);
        if (transactionId > 0) {
            editingTransaction = store.transactionById(transactionId);
            if (editingTransaction == null) {
                Toast.makeText(this, "账单不存在", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            fillFromTransaction(editingTransaction);
            return;
        }
        long amountCents = intent.getLongExtra(EXTRA_AMOUNT_CENTS, 0);
        if (amountCents > 0) {
            amountInput.setText(PaymentParser.formatMoney(amountCents));
        }
        String type = intent.getStringExtra(EXTRA_TYPE);
        typeSpinner.setSelection("income".equals(type) ? 1 : 0);
        setCategoryOptions("income".equals(type) ? "income" : "expense");
        String sourceApp = intent.getStringExtra(EXTRA_SOURCE_APP);
        sourceInput.setText(sourceApp == null ? "" : sourceApp);
        String merchant = intent.getStringExtra(EXTRA_MERCHANT);
        merchantInput.setText(merchant == null ? "" : merchant);
        String rawText = intent.getStringExtra(EXTRA_RAW_TEXT);
        noteInput.setText(rawText == null ? "" : rawText);
        String account = store.inferAccount(rawText, sourceApp);
        if ("未确认账户".equals(account)) {
            account = store.inferAccount(merchant, sourceApp);
        }
        selectSpinner(accountSpinner, account);
        selectSpinner(categorySpinner, ClassificationRules.inferCategory(rawText, sourceApp, merchant, "income".equals(type) ? "income" : "expense"));
    }

    private void fillFromTransaction(Transaction transaction) {
        amountInput.setText(PaymentParser.formatMoney(transaction.amountCents));
        boolean income = "income".equals(transaction.type);
        typeSpinner.setSelection(income ? 1 : 0);
        setCategoryOptions(income ? "income" : "expense");
        sourceInput.setText(transaction.sourceApp == null ? "" : transaction.sourceApp);
        merchantInput.setText(transaction.merchant == null ? "" : transaction.merchant);
        noteInput.setText(transaction.note == null ? "" : transaction.note);
        selectSpinner(accountSpinner, transaction.accountName);
        selectSpinner(categorySpinner, transaction.category);
    }

    private void save() {
        Long cents = parseAmount(amountInput.getText().toString());
        if (cents == null || cents <= 0) {
            Toast.makeText(this, "请输入正确金额", Toast.LENGTH_SHORT).show();
            return;
        }

        Transaction transaction = new Transaction();
        transaction.id = editingTransaction == null ? 0 : editingTransaction.id;
        transaction.type = typeSpinner.getSelectedItemPosition() == 1 ? "income" : "expense";
        transaction.amountCents = cents;
        transaction.sourceApp = sourceInput.getText().toString().trim();
        transaction.accountName = String.valueOf(accountSpinner.getSelectedItem());
        transaction.category = String.valueOf(categorySpinner.getSelectedItem());
        transaction.merchant = merchantInput.getText().toString().trim();
        transaction.note = noteInput.getText().toString().trim();
        transaction.rawText = editingTransaction == null ? getIntent().getStringExtra(EXTRA_RAW_TEXT) : editingTransaction.rawText;
        transaction.notificationKey = editingTransaction == null ? getIntent().getStringExtra(EXTRA_NOTIFICATION_KEY) : editingTransaction.notificationKey;
        transaction.occurredAt = editingTransaction == null
                ? getIntent().getLongExtra(EXTRA_OCCURRED_AT, System.currentTimeMillis())
                : editingTransaction.occurredAt;
        transaction.createdAt = System.currentTimeMillis();

        if (editingTransaction != null) {
            if (store.update(transaction)) {
                Toast.makeText(this, "已修改，账户余额已同步", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "修改失败", Toast.LENGTH_SHORT).show();
            }
        } else {
            long result = store.insert(transaction);
            if (result == -1) {
                Toast.makeText(this, "这条通知已经保存过了", Toast.LENGTH_SHORT).show();
            } else {
                store.logAutoRecord("saved", transaction.sourceApp, transaction.rawText,
                        "已保存：" + transaction.category + " / " + transaction.accountName, transaction.amountCents);
                Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show();
            }
        }
        finish();
    }

    private void selectSpinner(Spinner spinner, String value) {
        for (int i = 0; i < spinner.getCount(); i++) {
            if (value.equals(String.valueOf(spinner.getItemAtPosition(i)))) {
                spinner.setSelection(i);
                return;
            }
        }
    }

    private void setCategoryOptions(String type) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categoryValues(type));
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);
    }

    private String[] categoryValues(String type) {
        java.util.List<String> values = store.categoryNames(type);
        if (values.isEmpty()) {
            return "income".equals(type)
                    ? new String[]{"工资", "生活费", "收红包", "外快", "股票基金", "其它"}
                    : new String[]{"三餐", "零食", "衣服", "交通", "旅行", "日用品", "医疗", "娱乐", "其它"};
        }
        return values.toArray(new String[0]);
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
