package com.personal.xiaomiledger;

import android.content.Context;
import android.content.SharedPreferences;
import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class PaymentContextStore {
    private static final String PREFS = "payment_context";
    private static final long MAX_AGE_MS = 180000L;
    private static final Pattern MONEY_WITH_SYMBOL = Pattern.compile(
            "(?:¥|￥|人民币)\\s*([0-9]{1,6}(?:,[0-9]{3})*|[0-9]+)(?:\\.([0-9]{1,2}))?");
    private static final Pattern MONEY_WITH_YUAN = Pattern.compile(
            "([0-9]{1,6}(?:,[0-9]{3})*|[0-9]+)(?:\\.([0-9]{1,2}))?\\s*元");

    private PaymentContextStore() {
    }

    static void rememberIfUseful(Context context, String packageName, String rawText, long occurredAt) {
        if (!"com.tencent.mm".equals(packageName) || rawText == null || rawText.length() == 0) {
            return;
        }
        String raw = rawText.replace('\n', ' ').replaceAll("\\s+", " ").trim();
        boolean redPacketContext = containsAny(raw, "红包", "微信红包", "发红包", "塞钱进红包", "恭喜发财");
        boolean paymentMethodContext = containsAny(raw, "支付", "付款", "收银台", "支付方式", "储蓄卡", "信用卡", "银行卡")
                && containsAny(raw, "银行", "零钱", "余额", "储蓄卡", "信用卡", "银行卡");
        if (!redPacketContext && !paymentMethodContext) {
            return;
        }
        Long amount = findAmount(raw);
        String account = new TransactionStore(context).inferAccount(raw, "微信");
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
                : ClassificationRules.inferCategory(raw, "微信", "", "expense");
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

    static boolean isBankSource(ParsedPayment payment) {
        String source = payment == null || payment.sourceApp == null ? "" : payment.sourceApp;
        return "中国银行".equals(source) || "交通银行".equals(source) || "招商银行".equals(source)
                || source.endsWith("银行");
    }

    static boolean looksLikeWechatBankPayment(String raw) {
        return raw != null && containsAny(raw, "财付通", "微信", "借记卡动账", "动账提醒", "动账");
    }

    static boolean shouldDeferWechatCardPaymentToBank(ParsedPayment payment) {
        if (payment == null || !"expense".equals(payment.type)) {
            return false;
        }
        String source = payment.sourceApp == null ? "" : payment.sourceApp;
        String pkg = payment.sourcePackage == null ? "" : payment.sourcePackage;
        if (!"微信".equals(source) && !"com.tencent.mm".equals(pkg)) {
            return false;
        }
        String raw = payment.rawText == null ? "" : payment.rawText;
        if (raw.length() == 0 || hasSelectedWechatWallet(raw)) {
            return false;
        }
        return hasSelectedBankCard(raw) || containsAny(raw,
                "银行卡支付", "储蓄卡支付", "信用卡支付", "借记卡支付",
                "银行卡", "储蓄卡", "信用卡", "借记卡",
                "中国银行", "交通银行", "招商银行", "建设银行", "农业银行",
                "工商银行", "邮储银行", "邮政储蓄", "浦发银行", "民生银行",
                "平安银行", "兴业银行", "广发银行", "中信银行", "光大银行");
    }

    static boolean isExplicitWechatWalletPayment(ParsedPayment payment) {
        if (payment == null || !"expense".equals(payment.type)) {
            return false;
        }
        String source = payment.sourceApp == null ? "" : payment.sourceApp;
        String pkg = payment.sourcePackage == null ? "" : payment.sourcePackage;
        if (!"微信".equals(source) && !"com.tencent.mm".equals(pkg)) {
            return false;
        }
        return hasSelectedWechatWallet(payment.rawText == null ? "" : payment.rawText);
    }

    static boolean hasRecentWechatWalletPaymentContext(Context context, ParsedPayment payment) {
        if (payment == null || !"expense".equals(payment.type)) {
            return false;
        }
        String source = payment.sourceApp == null ? "" : payment.sourceApp;
        String pkg = payment.sourcePackage == null ? "" : payment.sourcePackage;
        if (!"微信".equals(source) && !"com.tencent.mm".equals(pkg)) {
            return false;
        }
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        long createdAt = prefs.getLong("created_at", 0);
        if (createdAt <= 0 || System.currentTimeMillis() - createdAt > MAX_AGE_MS) {
            return false;
        }
        long amount = prefs.getLong("amount_cents", 0L);
        if (amount > 0 && amount != payment.amountCents) {
            return false;
        }
        String account = prefs.getString("account", "");
        String raw = prefs.getString("raw", "");
        return "微信零钱".equals(account) || hasSelectedWechatWallet(raw);
    }

    private static boolean hasSelectedWechatWallet(String raw) {
        return Pattern.compile("(?:支付方式|付款方式)[:：\\s]*(?:微信零钱|零钱通|零钱)")
                .matcher(raw)
                .find()
                || containsAny(raw, "微信零钱支付", "零钱支付", "零钱通支付");
    }

    private static boolean hasSelectedBankCard(String raw) {
        return Pattern.compile("(?:支付方式|付款方式)[:：\\s]*[^，,。；;\\n]{0,28}(?:银行|银行卡|储蓄卡|信用卡|借记卡)")
                .matcher(raw)
                .find();
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
