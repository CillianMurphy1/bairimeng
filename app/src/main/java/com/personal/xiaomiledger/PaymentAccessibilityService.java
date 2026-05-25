package com.personal.xiaomiledger;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class PaymentAccessibilityService extends AccessibilityService {
    private static final Set<String> TARGET_PACKAGES = new HashSet<>(Arrays.asList(
            "com.tencent.mm",
            "com.eg.android.AlipayGphone",
            "com.taobao.taobao"
    ));
    private long lastLaunchAt;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private WindowManager windowManager;
    private View overlayView;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getPackageName() == null) {
            return;
        }
        int type = event.getEventType();
        if (type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && type != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            return;
        }
        String packageName = event.getPackageName().toString();
        if (!TARGET_PACKAGES.contains(packageName)) {
            return;
        }
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            return;
        }
        String raw = collectText(root);
        if (!looksLikePaymentResult(packageName, raw)) {
            return;
        }
        ParsedPayment payment = PaymentParser.parseRawText(packageName, raw,
                event.getEventTime() > 0 ? event.getEventTime() : System.currentTimeMillis());
        if (payment == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastLaunchAt < 2500L) {
            return;
        }
        TransactionStore store = new TransactionStore(this);
        if (store.hasNotificationKey(payment.notificationKey) || RecentPaymentGate.shouldSkipAndRemember(this, payment)) {
            store.logAutoRecord("duplicate", payment.sourceApp, payment.rawText, "无障碍重复识别，已忽略", payment.amountCents);
            return;
        }

        String category = ClassificationRules.inferCategory(payment.rawText, payment.sourceApp, payment.merchant, payment.type);
        String account = store.inferAccount(payment.rawText, payment.sourceApp);
        store.logAutoRecord("recognized", payment.sourceApp, payment.rawText,
                "无障碍识别：" + category + " / " + account, payment.amountCents);
        lastLaunchAt = now;
        if (AutoSaveManager.tryAutoSave(this, store, payment)) {
            return;
        }
        showPaymentOverlay(payment);
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
    }

    @Override
    public void onInterrupt() {
    }

    private void tryLaunchEditor(ParsedPayment payment) {
        try {
            removeOverlay();
            Intent intent = EditTransactionActivity.intentForPayment(this, payment);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        } catch (RuntimeException ignored) {
            // The high-priority notification remains available when Android blocks activity launch.
        }
    }

    private void showPaymentOverlay(ParsedPayment payment) {
        mainHandler.post(() -> {
            try {
                removeOverlay();
                if (windowManager == null) {
                    windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
                }
                if (windowManager == null) {
                    NotificationHelper.showPending(this, payment);
                    tryLaunchEditor(payment);
                    return;
                }
                overlayView = buildOverlay(payment);
                WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                        PixelFormat.TRANSLUCENT);
                params.gravity = Gravity.BOTTOM;
                params.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;
                windowManager.addView(overlayView, params);
                mainHandler.postDelayed(() -> {
                    if (overlayView != null) {
                        NotificationHelper.showPending(this, payment);
                    }
                }, 35000L);
            } catch (RuntimeException ignored) {
                NotificationHelper.showPending(this, payment);
                tryLaunchEditor(payment);
            }
        });
    }

    private View buildOverlay(ParsedPayment payment) {
        String category = ClassificationRules.inferCategory(payment.rawText, payment.sourceApp, payment.merchant, payment.type);
        TransactionStore store = new TransactionStore(this);
        String account = store.inferAccount(payment.rawText, payment.sourceApp);
        int actionColor = "income".equals(payment.type) ? Color.rgb(44, 188, 128) : Ui.WARNING;

        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setPadding(Ui.dp(this, 14), Ui.dp(this, 8), Ui.dp(this, 14), Ui.dp(this, 14));
        shell.setBackgroundColor(Color.TRANSPARENT);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(Ui.bg(this, Color.WHITE, 22));
        card.setPadding(Ui.dp(this, 18), Ui.dp(this, 16), Ui.dp(this, 18), Ui.dp(this, 16));
        shell.addView(card, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout top = Ui.row(this);
        TextView title = Ui.text(this, "识别到一笔" + ("income".equals(payment.type) ? "收入" : "支出"), 20, Ui.INK, Typeface.BOLD);
        top.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView close = Ui.text(this, "×", 28, Ui.MUTED, Typeface.NORMAL);
        close.setGravity(Gravity.CENTER);
        close.setOnClickListener(v -> removeOverlay());
        top.addView(close, new LinearLayout.LayoutParams(Ui.dp(this, 42), Ui.dp(this, 42)));
        card.addView(top);

        TextView amount = Ui.text(this,
                payment.amountCents > 0 ? PaymentParser.formatMoney(payment.amountCents) : "金额待填写",
                36, actionColor, Typeface.BOLD);
        amount.setGravity(Gravity.CENTER_HORIZONTAL);
        card.addView(amount);
        card.addView(Ui.spacer(this, 8));

        card.addView(detailRow("来源", safe(payment.sourceApp)));
        card.addView(detailRow("分类", category));
        card.addView(detailRow("账户", account));
        card.addView(detailRow("商户", safe(payment.merchant)));

        LinearLayout actions = Ui.row(this);
        actions.setPadding(0, Ui.dp(this, 14), 0, 0);
        Button edit = actionButton("编辑", Ui.INK, Color.WHITE);
        edit.setBackground(Ui.strokeBg(this, Color.WHITE, 16, Ui.LINE));
        edit.setOnClickListener(v -> tryLaunchEditor(payment));
        actions.addView(edit, new LinearLayout.LayoutParams(0, Ui.dp(this, 50), 1));

        Button save = actionButton(payment.amountCents > 0 ? "确认保存" : "补全记录", Color.WHITE, actionColor);
        save.setOnClickListener(v -> {
            if (payment.amountCents <= 0) {
                tryLaunchEditor(payment);
                return;
            }
            if (savePayment(payment, category, account)) {
                Toast.makeText(this, "已记账", Toast.LENGTH_SHORT).show();
                removeOverlay();
            } else {
                tryLaunchEditor(payment);
            }
        });
        LinearLayout.LayoutParams saveLp = new LinearLayout.LayoutParams(0, Ui.dp(this, 50), 1);
        saveLp.setMargins(Ui.dp(this, 10), 0, 0, 0);
        actions.addView(save, saveLp);
        card.addView(actions);
        return shell;
    }

    private LinearLayout detailRow(String label, String value) {
        LinearLayout row = Ui.row(this);
        row.setPadding(0, Ui.dp(this, 4), 0, Ui.dp(this, 4));
        row.addView(Ui.text(this, label, 15, Ui.MUTED, Typeface.BOLD), new LinearLayout.LayoutParams(Ui.dp(this, 58), ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView text = Ui.text(this, value, 16, Ui.INK, Typeface.NORMAL);
        text.setSingleLine(false);
        row.addView(text, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        return row;
    }

    private Button actionButton(String label, int textColor, int bgColor) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextSize(16);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(textColor);
        button.setBackground(Ui.bg(this, bgColor, 16));
        return button;
    }

    private boolean savePayment(ParsedPayment payment, String category, String account) {
        Transaction transaction = new Transaction();
        transaction.type = payment.type;
        transaction.amountCents = payment.amountCents;
        transaction.sourceApp = payment.sourceApp;
        transaction.accountName = account;
        transaction.category = category;
        transaction.merchant = payment.merchant == null || payment.merchant.length() == 0 ? category : payment.merchant;
        transaction.note = payment.rawText;
        transaction.rawText = payment.rawText;
        transaction.notificationKey = payment.notificationKey;
        transaction.occurredAt = payment.occurredAt;
        transaction.createdAt = System.currentTimeMillis();
        TransactionStore store = new TransactionStore(this);
        long result = store.insert(transaction);
        if (result != -1) {
            store.logAutoRecord("saved", payment.sourceApp, payment.rawText,
                    "浮层保存：" + category + " / " + account, payment.amountCents);
        }
        return result != -1;
    }

    private void removeOverlay() {
        mainHandler.post(() -> {
            if (overlayView != null && windowManager != null) {
                try {
                    windowManager.removeView(overlayView);
                } catch (RuntimeException ignored) {
                }
            }
            overlayView = null;
        });
    }

    private String safe(String value) {
        return value == null || value.length() == 0 ? "未识别" : value;
    }

    private String collectText(AccessibilityNodeInfo root) {
        StringBuilder builder = new StringBuilder();
        appendNodeText(root, builder, 0);
        return builder.toString().replace('\n', ' ').replaceAll("\\s+", " ").trim();
    }

    private void appendNodeText(AccessibilityNodeInfo node, StringBuilder builder, int depth) {
        if (node == null || depth > 12 || builder.length() > 5000) {
            return;
        }
        CharSequence text = node.getText();
        if (text != null && text.length() > 0) {
            builder.append(' ').append(text);
        }
        CharSequence desc = node.getContentDescription();
        if (desc != null && desc.length() > 0) {
            builder.append(' ').append(desc);
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            appendNodeText(node.getChild(i), builder, depth + 1);
        }
    }

    private boolean looksLikePaymentResult(String packageName, String raw) {
        if (raw == null || raw.length() == 0) {
            return false;
        }
        boolean hasResult = containsAny(raw, "支付成功", "付款成功", "交易成功", "已支付", "扣款成功", "收款到账", "到账成功", "转账成功");
        boolean hasAmount = containsAny(raw, "¥", "￥", "元", "人民币", "RMB", "CNY");
        boolean noisy = containsAny(raw, "验证码", "登录", "密码", "银行卡号");
        boolean allowMissingAmount = "com.taobao.taobao".equals(packageName);
        return hasResult && (hasAmount || allowMissingAmount) && !noisy;
    }

    private boolean containsAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }
}
