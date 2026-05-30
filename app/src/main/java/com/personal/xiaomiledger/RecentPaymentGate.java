package com.personal.xiaomiledger;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.LinkedHashSet;

final class RecentPaymentGate {
    private static final String PREFS = "recent_payment_gate";
    private static final String KEYS = "keys";
    private static final String BANK_EXPENSES = "bank_expenses";
    private static final String BANK_INCOMES = "bank_incomes";
    private static final String LAST_BANK_SEEN = "last_bank_seen";
    private static final int MAX_KEYS = 120;
    private static final int MAX_BANK_EVENTS = 40;
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
        if (payment == null || payment.amountCents <= 0) {
            return false;
        }
        if (isWechatExpense(payment)) {
            if (isWechatWalletExpense(payment, context)) return false;
            return recentBankMatch(context, payment, BANK_EXPENSES);
        }
        if (isWechatRefundIncome(payment)) {
            if (isWechatWalletRefund(payment)) return false;
            return recentBankMatch(context, payment, BANK_INCOMES);
        }
        return false;
    }

    static boolean shouldWaitForPossibleBankPayment(Context context, ParsedPayment payment) {
        if (payment == null || payment.amountCents <= 0) return false;
        if (isWechatExpense(payment)) {
            if (PaymentContextStore.isExplicitWechatWalletPayment(payment)) return false;
            return !PaymentContextStore.hasRecentWechatWalletPaymentContext(context, payment);
        }
        if (isWechatRefundIncome(payment)) {
            return !isWechatWalletRefund(payment);
        }
        return false;
    }

    static long crossSourceWindowMs() {
        return CROSS_SOURCE_WINDOW_MS;
    }

    static void rememberBankExpense(Context context, ParsedPayment payment) {
        rememberBankEvent(context, payment, BANK_EXPENSES, isBankExpense(payment));
    }

    static void rememberBankIncome(Context context, ParsedPayment payment) {
        rememberBankEvent(context, payment, BANK_INCOMES, isBankIncome(payment));
    }

    private static void rememberBankEvent(Context context, ParsedPayment payment, String prefKey, boolean eligible) {
        if (!eligible || payment.amountCents <= 0) {
            return;
        }
        long now = System.currentTimeMillis();
        if (payment.occurredAt > 0 && now - payment.occurredAt > CROSS_SOURCE_WINDOW_MS) {
            return;
        }
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        LinkedHashSet<String> records = new LinkedHashSet<>(prefs.getStringSet(prefKey, new LinkedHashSet<>()));
        for (String record : new LinkedHashSet<>(records)) {
            BankEvent event = BankEvent.parse(record);
            if (event == null || now - event.seenAt > CROSS_SOURCE_WINDOW_MS) {
                records.remove(record);
            }
        }
        records.add(payment.amountCents + "|" + now);
        while (records.size() > MAX_BANK_EVENTS) {
            records.remove(records.iterator().next());
        }
        prefs.edit().putStringSet(prefKey, records).apply();
    }

    private static boolean recentBankMatch(Context context, ParsedPayment payment, String prefKey) {
        long now = System.currentTimeMillis();
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        LinkedHashSet<String> records = new LinkedHashSet<>(prefs.getStringSet(prefKey, new LinkedHashSet<>()));
        boolean matched = false;
        boolean changed = false;
        for (String record : new LinkedHashSet<>(records)) {
            BankEvent event = BankEvent.parse(record);
            if (event == null || now - event.seenAt > CROSS_SOURCE_WINDOW_MS) {
                records.remove(record);
                changed = true;
                continue;
            }
            if (event.amountCents == payment.amountCents) {
                matched = true;
            }
        }
        if (changed) {
            prefs.edit().putStringSet(prefKey, records).apply();
        }
        return matched;
    }

    private static boolean isWechatWalletExpense(ParsedPayment payment, Context context) {
        if (PaymentContextStore.isExplicitWechatWalletPayment(payment)) return true;
        return PaymentContextStore.hasRecentWechatWalletPaymentContext(context, payment);
    }

    private static boolean isWechatWalletRefund(ParsedPayment payment) {
        String raw = payment.rawText == null ? "" : payment.rawText;
        return raw.contains("退回零钱");
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

    static boolean isWechatExpense(ParsedPayment payment) {
        if (payment == null || !"expense".equals(payment.type)) {
            return false;
        }
        String source = payment.sourceApp == null ? "" : payment.sourceApp;
        String pkg = payment.sourcePackage == null ? "" : payment.sourcePackage;
        return "微信".equals(source) || "com.tencent.mm".equals(pkg);
    }

    static boolean isWechatRefundIncome(ParsedPayment payment) {
        if (payment == null || !"income".equals(payment.type)) {
            return false;
        }
        String source = payment.sourceApp == null ? "" : payment.sourceApp;
        String pkg = payment.sourcePackage == null ? "" : payment.sourcePackage;
        if (!"微信".equals(source) && !"com.tencent.mm".equals(pkg)) {
            return false;
        }
        String raw = payment.rawText == null ? "" : payment.rawText;
        return raw.contains("退款") || raw.contains("退回");
    }

    private static boolean isBankExpense(ParsedPayment payment) {
        return payment != null
                && "expense".equals(payment.type)
                && PaymentContextStore.isBankSource(payment);
    }

    static boolean isBankIncome(ParsedPayment payment) {
        return payment != null
                && "income".equals(payment.type)
                && PaymentContextStore.isBankSource(payment);
    }

    static void rememberBankSeen(Context context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putLong(LAST_BANK_SEEN, System.currentTimeMillis()).apply();
    }

    static boolean wasBankRecentlySeen(Context context) {
        long lastSeen = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getLong(LAST_BANK_SEEN, 0);
        return lastSeen > 0 && System.currentTimeMillis() - lastSeen < CROSS_SOURCE_WINDOW_MS * 3;
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

    private static final class BankEvent {
        final long amountCents;
        final long seenAt;

        BankEvent(long amountCents, long seenAt) {
            this.amountCents = amountCents;
            this.seenAt = seenAt;
        }

        static BankEvent parse(String value) {
            if (value == null) {
                return null;
            }
            String[] parts = value.split("\\|");
            if (parts.length != 2) {
                return null;
            }
            try {
                return new BankEvent(Long.parseLong(parts[0]), Long.parseLong(parts[1]));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
    }
}
