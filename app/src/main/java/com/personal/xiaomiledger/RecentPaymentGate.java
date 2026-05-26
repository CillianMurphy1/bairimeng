package com.personal.xiaomiledger;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.LinkedHashSet;

final class RecentPaymentGate {
    private static final String PREFS = "recent_payment_gate";
    private static final String KEYS = "keys";
    private static final String BANK_EXPENSES = "bank_expenses";
    private static final int MAX_KEYS = 120;
    private static final int MAX_BANK_EXPENSES = 40;
    private static final long CROSS_SOURCE_WINDOW_MS = 2000L;

    private RecentPaymentGate() {
    }

    static boolean shouldSkipAndRemember(Context context, ParsedPayment payment) {
        String fingerprint = fingerprint(payment);
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        LinkedHashSet<String> keys = new LinkedHashSet<>(prefs.getStringSet(KEYS, new LinkedHashSet<>()));
        if (keys.contains(fingerprint)) {
            return true;
        }
        keys.add(fingerprint);
        while (keys.size() > MAX_KEYS) {
            keys.remove(keys.iterator().next());
        }
        prefs.edit().putStringSet(KEYS, keys).apply();
        return false;
    }

    static boolean shouldSkipWechatAfterRecentBank(Context context, ParsedPayment payment) {
        if (!isWechatExpense(payment) || payment.amountCents <= 0) {
            return false;
        }
        if (PaymentContextStore.isExplicitWechatWalletPayment(payment)) {
            return false;
        }
        if (PaymentContextStore.hasRecentWechatWalletPaymentContext(context, payment)) {
            return false;
        }
        long now = System.currentTimeMillis();
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        LinkedHashSet<String> records = new LinkedHashSet<>(prefs.getStringSet(BANK_EXPENSES, new LinkedHashSet<>()));
        boolean matched = false;
        boolean changed = false;
        for (String record : new LinkedHashSet<>(records)) {
            BankExpense expense = BankExpense.parse(record);
            if (expense == null || now - expense.seenAt > CROSS_SOURCE_WINDOW_MS) {
                records.remove(record);
                changed = true;
                continue;
            }
            if (expense.amountCents == payment.amountCents) {
                matched = true;
            }
        }
        if (changed) {
            prefs.edit().putStringSet(BANK_EXPENSES, records).apply();
        }
        return matched;
    }

    static boolean shouldWaitForPossibleBankPayment(Context context, ParsedPayment payment) {
        if (!isWechatExpense(payment) || payment.amountCents <= 0) {
            return false;
        }
        if (PaymentContextStore.isExplicitWechatWalletPayment(payment)) {
            return false;
        }
        return !PaymentContextStore.hasRecentWechatWalletPaymentContext(context, payment);
    }

    static long crossSourceWindowMs() {
        return CROSS_SOURCE_WINDOW_MS;
    }

    static void rememberBankExpense(Context context, ParsedPayment payment) {
        if (!isBankExpense(payment) || payment.amountCents <= 0) {
            return;
        }
        long now = System.currentTimeMillis();
        if (payment.occurredAt > 0 && now - payment.occurredAt > CROSS_SOURCE_WINDOW_MS) {
            return;
        }
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        LinkedHashSet<String> records = new LinkedHashSet<>(prefs.getStringSet(BANK_EXPENSES, new LinkedHashSet<>()));
        for (String record : new LinkedHashSet<>(records)) {
            BankExpense expense = BankExpense.parse(record);
            if (expense == null || now - expense.seenAt > CROSS_SOURCE_WINDOW_MS) {
                records.remove(record);
            }
        }
        records.add(payment.amountCents + "|" + now);
        while (records.size() > MAX_BANK_EXPENSES) {
            records.remove(records.iterator().next());
        }
        prefs.edit().putStringSet(BANK_EXPENSES, records).apply();
    }

    private static String fingerprint(ParsedPayment payment) {
        long bucket = payment.occurredAt / 120000L;
        if (PaymentContextStore.isBankSource(payment)) {
            int rawHash = payment.rawText == null ? 0 : payment.rawText.hashCode();
            return payment.sourceApp + "|" + payment.type + "|" + payment.amountCents + "|" + payment.occurredAt + "|" + rawHash;
        }
        if (isPaymentAppSource(payment)) {
            int rawHash = payment.rawText == null ? 0 : payment.rawText.hashCode();
            return payment.sourceApp + "|" + payment.type + "|" + payment.amountCents + "|" + payment.occurredAt + "|" + rawHash;
        }
        String extra = payment.amountCents <= 0 && payment.rawText != null ? "|" + payment.rawText.hashCode() : "";
        return payment.sourceApp + "|" + payment.type + "|" + payment.amountCents + "|" + bucket + extra;
    }

    private static boolean isWechatExpense(ParsedPayment payment) {
        if (payment == null || !"expense".equals(payment.type)) {
            return false;
        }
        String source = payment.sourceApp == null ? "" : payment.sourceApp;
        String pkg = payment.sourcePackage == null ? "" : payment.sourcePackage;
        return "微信".equals(source) || "com.tencent.mm".equals(pkg);
    }

    private static boolean isBankExpense(ParsedPayment payment) {
        return payment != null
                && "expense".equals(payment.type)
                && PaymentContextStore.isBankSource(payment);
    }

    private static boolean isPaymentAppSource(ParsedPayment payment) {
        if (payment == null) {
            return false;
        }
        String source = payment.sourceApp == null ? "" : payment.sourceApp;
        String pkg = payment.sourcePackage == null ? "" : payment.sourcePackage;
        return "微信".equals(source)
                || "支付宝".equals(source)
                || "淘宝".equals(source)
                || "com.tencent.mm".equals(pkg)
                || "com.eg.android.AlipayGphone".equals(pkg)
                || "com.taobao.taobao".equals(pkg);
    }

    private static final class BankExpense {
        final long amountCents;
        final long seenAt;

        BankExpense(long amountCents, long seenAt) {
            this.amountCents = amountCents;
            this.seenAt = seenAt;
        }

        static BankExpense parse(String value) {
            if (value == null) {
                return null;
            }
            String[] parts = value.split("\\|");
            if (parts.length != 2) {
                return null;
            }
            try {
                return new BankExpense(Long.parseLong(parts[0]), Long.parseLong(parts[1]));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
    }
}
