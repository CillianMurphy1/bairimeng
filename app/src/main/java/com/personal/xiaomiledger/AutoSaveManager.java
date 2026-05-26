package com.personal.xiaomiledger;

import android.content.Context;
import android.content.SharedPreferences;

final class AutoSaveManager {
    private static final String PREFS = "automation_settings";
    private static final String AUTO_SAVE = "auto_save_high_confidence";

    private AutoSaveManager() {
    }

    static boolean isAutoSaveEnabled(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(AUTO_SAVE, false);
    }

    static void setAutoSaveEnabled(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(AUTO_SAVE, enabled).apply();
    }

    static boolean tryAutoSave(Context context, TransactionStore store, ParsedPayment payment) {
        boolean trustedAutoSource = isBankPayment(payment)
                || isBankMovement(payment)
                || isTransitCard(payment)
                || isExplicitWalletPayment(payment)
                || isWechatIncomeReceipt(payment);
        if (!isAutoSaveEnabled(context) && !trustedAutoSource) {
            return false;
        }
        if (payment.amountCents <= 0) {
            return false;
        }
        String category = ClassificationRules.inferCategory(payment.rawText, payment.sourceApp, payment.merchant, payment.type);
        String account = store.inferAccount(payment.rawText, payment.sourceApp);
        if ("未确认账户".equals(account)) {
            return false;
        }
        if (isTransitCard(payment)) {
            category = "income".equals(payment.type) ? "其它" : "交通";
        } else if (isBankPayment(payment) || isBankMovement(payment)) {
            category = bankCategory(payment.rawText, payment.type);
        } else if (!isExplicitWalletPayment(payment) && !isWechatIncomeReceipt(payment) && "其它".equals(category)) {
            return false;
        }

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

        long result = store.insert(transaction);
        if (result != -1) {
            store.logAutoRecord("saved", payment.sourceApp, payment.rawText,
                    "自动保存：" + category + " / " + account, payment.amountCents);
            NotificationHelper.showAutoSaved(context, transaction);
        }
        return result != -1;
    }

    private static boolean isBankPayment(ParsedPayment payment) {
        String source = payment.sourceApp == null ? "" : payment.sourceApp;
        return "中国银行".equals(source) || "交通银行".equals(source) || "招商银行".equals(source);
    }

    private static boolean isBankMovement(ParsedPayment payment) {
        String pkg = payment.sourcePackage == null ? "" : payment.sourcePackage;
        String raw = payment.rawText == null ? "" : payment.rawText;
        if ("com.tencent.mm".equals(pkg) || "com.eg.android.AlipayGphone".equals(pkg) || "com.taobao.taobao".equals(pkg)) {
            return false;
        }
        return raw.contains("动账") || raw.contains("账户") || raw.contains("入账")
                || raw.contains("到账") || raw.contains("支出") || raw.contains("扣款")
                || raw.contains("收入") || raw.contains("交易");
    }

    private static boolean isTransitCard(ParsedPayment payment) {
        String source = payment.sourceApp == null ? "" : payment.sourceApp;
        String raw = payment.rawText == null ? "" : payment.rawText;
        return "长安通互联互通卡".equals(source)
                || (raw.contains("长安通") || raw.contains("互联互通卡"))
                && (raw.contains("地铁") || raw.contains("公交") || raw.contains("充值"));
    }

    private static boolean isExplicitWalletPayment(ParsedPayment payment) {
        String source = payment.sourceApp == null ? "" : payment.sourceApp;
        String raw = payment.rawText == null ? "" : payment.rawText;
        if ("微信".equals(source)) {
            return raw.contains("退回零钱") || raw.contains("退款方式 退回零钱")
                    || raw.contains("零钱") || raw.contains("零钱通") || raw.contains("微信零钱");
        }
        if ("支付宝".equals(source)) {
            return raw.contains("支付宝余额") || raw.contains("余额宝") || raw.contains("余额支付");
        }
        return false;
    }

    private static boolean isWechatIncomeReceipt(ParsedPayment payment) {
        String source = payment.sourceApp == null ? "" : payment.sourceApp;
        String raw = payment.rawText == null ? "" : payment.rawText;
        return "income".equals(payment.type)
                && "微信".equals(source)
                && (raw.contains("红包已到账")
                || raw.contains("微信红包已到账")
                || raw.contains("收到红包")
                || raw.contains("转账已收款")
                || raw.contains("收到转账")
                || raw.contains("收款到账")
                || raw.contains("已收款")
                || raw.contains("退款到账")
                || raw.contains("退回零钱"));
    }

    private static String bankCategory(String rawText, String type) {
        if ("income".equals(type)) {
            return ClassificationRules.inferCategory(rawText, "", "", type);
        }
        String text = rawText == null ? "" : rawText;
        if (text.contains("红包") || text.contains("塞钱进红包") || text.contains("发红包")) return "发红包";
        if (text.contains("微信") || text.contains("财付通")) return "其它";
        if (text.contains("支付宝") || text.contains("淘宝") || text.contains("天猫")) return "日用品";
        if (text.contains("美团") || text.contains("饿了么") || text.contains("外卖")) return "三餐";
        return ClassificationRules.inferCategory(rawText, "", "", type);
    }
}
