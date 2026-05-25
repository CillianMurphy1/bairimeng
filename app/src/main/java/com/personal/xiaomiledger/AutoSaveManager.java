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
        if (!isAutoSaveEnabled(context) && !isBankPayment(payment)) {
            return false;
        }
        if (payment.amountCents <= 0) {
            return false;
        }
        String category = ClassificationRules.inferCategory(payment.rawText, payment.sourceApp, payment.merchant, payment.type);
        String account = ClassificationRules.inferAccount(payment.rawText, payment.sourceApp);
        if ("未确认账户".equals(account)) {
            return false;
        }
        if (isBankPayment(payment)) {
            category = bankCategory(payment.rawText, payment.type);
        } else if ("其它".equals(category)) {
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

    private static String bankCategory(String rawText, String type) {
        if ("income".equals(type)) {
            return ClassificationRules.inferCategory(rawText, "", "", type);
        }
        String text = rawText == null ? "" : rawText;
        if (text.contains("红包")) return "发红包";
        if (text.contains("微信") || text.contains("财付通")) return "其它";
        if (text.contains("支付宝") || text.contains("淘宝") || text.contains("天猫")) return "日用品";
        if (text.contains("美团") || text.contains("饿了么") || text.contains("外卖")) return "三餐";
        return ClassificationRules.inferCategory(rawText, "", "", type);
    }
}
