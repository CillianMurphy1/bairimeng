package com.personal.xiaomiledger;

import android.content.Context;
import android.content.SharedPreferences;
import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class PaymentContextStore {
    private static final String PREFS = "payment_context";
    private static final long MAX_AGE_MS = 180000L;
    private static final long WECHAT_PAYMENT_CONTEXT_MAX_AGE_MS = 1000L;
    private static final Pattern MONEY_WITH_SYMBOL = Pattern.compile(
            "(?:¥|￥|人民币)\\s*([0-9]{1,6}(?:,[0-9]{3})*|[0-9]+)(?:\\.([0-9]{1,2}))?");
    private static final Pattern MONEY_WITH_YUAN = Pattern.compile(
            "([0-9]{1,6}(?:,[0-9]{3})*|[0-9]+)(?:\\.([0-9]{1,2}))?\\s*元");

    private PaymentContextStore() {
    }

    static void rememberIfUseful(Context context, String packageName, String rawText, long occurredAt) {
        if (!isWalletPackage(packageName) || rawText == null || rawText.length() == 0) {
            return;
        }
        String sourceApp = "com.eg.android.AlipayGphone".equals(packageName) ? "支付宝" : "微信";
        String raw = rawText.replace('\n', ' ').replaceAll("\\s+", " ").trim();
        boolean redPacketContext = containsAny(raw, "红包", "微信红包", "发红包", "塞钱进红包", "恭喜发财");
        boolean paymentMethodContext = containsAny(raw, "支付", "付款", "收银台", "支付方式", "储蓄卡", "信用卡", "银行卡")
                && containsAny(raw, "银行", "零钱", "余额", "储蓄卡", "信用卡", "银行卡");
        if (!redPacketContext && !paymentMethodContext) {
            return;
        }
        Long amount = findAmount(raw);
        String account = new TransactionStore(context).inferAccount(raw, sourceApp);
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        long previousCreatedAt = prefs.getLong("created_at", 0);
        boolean hasRecentPrevious = previousCreatedAt > 0 && System.currentTimeMillis() - previousCreatedAt <= MAX_AGE_MS;
        long previousAmount = hasRecentPrevious ? prefs.getLong("amount_cents", 0L) : 0L;
        String previousAccount = hasRecentPrevious ? prefs.getString("account", "") : "";
        String previousCategory = hasRecentPrevious ? prefs.getString("category", "") : "";
        if (amount == null && previousAmount > 0) {
            amount = previousAmount;
        }
        if ("未确认账户".equals(account) && previousAccount.length() > 0) {
            account = previousAccount;
        }
        if (amount == null && "未确认账户".equals(account) && !redPacketContext && previousCategory.length() == 0) {
            return;
        }
        String category = redPacketContext || "发红包".equals(previousCategory)
                ? "发红包"
                : ClassificationRules.inferCategory(raw, sourceApp, "", "expense");
        prefs.edit()
                .putLong("created_at", occurredAt > 0 ? occurredAt : System.currentTimeMillis())
                .putLong("amount_cents", amount == null ? 0L : amount)
                .putString("account", account)
                .putString("category", category)
                .putString("raw", raw)
                .apply();
    }

    static ParsedPayment enrichBankPayment(Context context, ParsedPayment payment) {
        if (payment == null || !isBankSource(payment) || !looksLikeWechatBankPayment(payment.rawText)) {
            return payment;
        }
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        long createdAt = prefs.getLong("created_at", 0);
        if (createdAt <= 0 || System.currentTimeMillis() - createdAt > MAX_AGE_MS) {
            return payment;
        }
        String account = prefs.getString("account", "");
        if (account.length() > 0 && !"未确认账户".equals(account) && !account.equals(payment.sourceApp)) {
            return payment;
        }
        long amount = prefs.getLong("amount_cents", 0L);
        if (payment.amountCents <= 0 && amount > 0) {
            payment.amountCents = amount;
        }
        String raw = prefs.getString("raw", "");
        if (raw.length() > 0 && !payment.rawText.contains(raw)) {
            payment.rawText = payment.rawText + " " + raw;
        }
        return payment;
    }

    static ParsedPayment enrichWechatPayment(Context context, ParsedPayment payment) {
        if (payment == null || !isWalletPackage(payment.sourcePackage) || !"expense".equals(payment.type)) {
            return payment;
        }
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (!isRecentForPayment(prefs, payment, WECHAT_PAYMENT_CONTEXT_MAX_AGE_MS)) {
            return payment;
        }
        long amount = prefs.getLong("amount_cents", 0L);
        if (amount > 0 && payment.amountCents > 0 && amount != payment.amountCents) {
            return payment;
        }
        String raw = prefs.getString("raw", "");
        if (raw.length() > 0 && !payment.rawText.contains(raw)) {
            payment.rawText = payment.rawText + " " + raw;
        }
        return payment;
    }

    static boolean shouldWaitForBankNotification(Context context, ParsedPayment payment) {
        if (payment == null || !isWalletPackage(payment.sourcePackage) || !"expense".equals(payment.type)) {
            return false;
        }
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (!isRecentForPayment(prefs, payment, WECHAT_PAYMENT_CONTEXT_MAX_AGE_MS)) {
            return false;
        }
        long amount = prefs.getLong("amount_cents", 0L);
        if (amount > 0 && payment.amountCents > 0 && amount != payment.amountCents) {
            return false;
        }
        String account = prefs.getString("account", "");
        return account.contains("银行") && !account.contains("微信") && !account.contains("支付宝");
    }

    private static boolean isWalletPackage(String packageName) {
        return "com.tencent.mm".equals(packageName) || "com.eg.android.AlipayGphone".equals(packageName);
    }

    private static boolean isRecentForPayment(SharedPreferences prefs, ParsedPayment payment, long maxAgeMs) {
        long createdAt = prefs.getLong("created_at", 0);
        if (createdAt <= 0) {
            return false;
        }
        long occurredAt = payment != null && payment.occurredAt > 0 ? payment.occurredAt : System.currentTimeMillis();
        return Math.abs(occurredAt - createdAt) <= maxAgeMs;
    }

    static boolean isBankSource(ParsedPayment payment) {
        String source = payment == null || payment.sourceApp == null ? "" : payment.sourceApp;
        return "中国银行".equals(source) || "交通银行".equals(source) || "招商银行".equals(source)
                || source.endsWith("银行");
    }

    static boolean looksLikeWechatBankPayment(String raw) {
        return raw != null && containsAny(raw, "财付通", "微信", "借记卡动账", "动账提醒", "动账");
    }

    private static Long findAmount(String raw) {
        Long amount = findAmount(raw, MONEY_WITH_SYMBOL);
        return amount != null ? amount : findAmount(raw, MONEY_WITH_YUAN);
    }

    private static Long findAmount(String raw, Pattern pattern) {
        Matcher matcher = pattern.matcher(raw);
        if (!matcher.find()) {
            return null;
        }
        String integer = matcher.group(1).replace(",", "");
        String fraction = matcher.group(2) == null ? "00" : matcher.group(2);
        if (fraction.length() == 1) {
            fraction = fraction + "0";
        }
        try {
            return new BigDecimal(integer + "." + fraction).movePointRight(2).longValue();
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static boolean containsAny(String text, String... values) {
        for (String value : values) {
            if (text.contains(value)) {
                return true;
            }
        }
        return false;
    }
}
