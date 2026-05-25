package com.personal.xiaomiledger;

import android.app.Notification;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class PaymentParser {
    private static final Set<String> WATCHED_PACKAGES = new HashSet<>(Arrays.asList(
            "com.tencent.mm",
            "com.eg.android.AlipayGphone",
            "com.taobao.taobao",
            "com.chinamworld.main",
            "com.chinamworld.bocmbci",
            "com.bankcomm.Bankcomm",
            "cmb.pb"
    ));

    private static final Pattern AMOUNT_PATTERN = Pattern.compile(
            "(?:人民币|RMB|CNY|￥|¥)?\\s*([0-9]{1,3}(?:,[0-9]{3})*|[0-9]+)(?:\\.([0-9]{1,2}))?\\s*元?");
    private static final Pattern LABELED_AMOUNT_PATTERN = Pattern.compile(
            "(?:实付|实付款|支付金额|付款金额|订单金额|合计|共计|扣款金额|消费金额|交易金额|支出金额|入账金额|到账金额)[:：\\s]*(?:人民币|RMB|CNY|￥|¥)?\\s*([0-9]{1,3}(?:,[0-9]{3})*|[0-9]+)(?:\\.([0-9]{1,2}))?\\s*元?");
    private static final Pattern BANK_AMOUNT_PATTERN = Pattern.compile(
            "(?:动账|交易|消费|支出|扣款|付款|支付|入账|到账|转入)[^0-9￥¥]{0,24}(?:人民币|RMB|CNY|￥|¥)?\\s*([0-9]{1,3}(?:,[0-9]{3})*|[0-9]+)(?:\\.([0-9]{1,2}))?\\s*元?");
    private static final Pattern MERCHANT_PAY_TO = Pattern.compile("(?:支付给|付款给|转账给|向)([^，,。；;\\n]{2,24})");
    private static final Pattern MERCHANT_LABEL = Pattern.compile("(?:商户|收款方|对方|店铺)[:：\\s]+([^，,。；;\\n]{2,24})");
    private static final Pattern TAOBAO_SUCCESS_TITLE = Pattern.compile("支付成功\\s+(.{2,50}?)(?:\\s+查看订单|\\s+本单奖励|\\s+该宝贝|$)");

    private PaymentParser() {
    }

    static ParsedPayment parse(StatusBarNotification sbn) {
        if (sbn == null || sbn.getNotification() == null) {
            return null;
        }
        String packageName = sbn.getPackageName();
        if (!WATCHED_PACKAGES.contains(packageName)) {
            return null;
        }

        Notification notification = sbn.getNotification();
        Bundle extras = notification.extras;
        String title = valueOf(extras, Notification.EXTRA_TITLE);
        String text = valueOf(extras, Notification.EXTRA_TEXT);
        String bigText = valueOf(extras, Notification.EXTRA_BIG_TEXT);
        String subText = valueOf(extras, Notification.EXTRA_SUB_TEXT);
        String lines = linesOf(extras);
        String raw = normalize(title + " " + text + " " + bigText + " " + subText + " " + lines);
        if (raw.length() == 0) {
            return null;
        }
        String type = isBankPackage(packageName) ? detectBankType(raw) : detectType(raw);
        if (type == null && isBankPackage(packageName) && looksLikeBankMovement(raw)) {
            type = "expense";
        }
        if (type == null) {
            return null;
        }
        Long amountCents = "com.taobao.taobao".equals(packageName)
                ? findLabeledAmount(raw)
                : (isBankPackage(packageName) ? findBankAmount(raw) : findPreferredAmount(raw));
        if (amountCents == null) {
            if (("com.taobao.taobao".equals(packageName) && raw.contains("支付成功"))
                    || (isBankPackage(packageName) && looksLikeBankMovement(raw))) {
                amountCents = 0L;
            } else {
                return null;
            }
        }
        if (amountCents < 0) {
            return null;
        }

        ParsedPayment parsed = new ParsedPayment();
        parsed.type = type;
        parsed.amountCents = amountCents;
        parsed.sourcePackage = packageName;
        parsed.sourceApp = sourceName(packageName);
        parsed.rawText = raw;
        parsed.merchant = findMerchant(raw, title);
        if ("com.taobao.taobao".equals(packageName)) {
            String taobaoTitle = findTaobaoTitle(raw);
            if (taobaoTitle.length() > 0) {
                parsed.merchant = taobaoTitle;
            }
        }
        parsed.occurredAt = sbn.getPostTime() > 0 ? sbn.getPostTime() : System.currentTimeMillis();
        parsed.notificationKey = sbn.getKey() != null && sbn.getKey().length() > 0
                ? sbn.getKey()
                : packageName + ":" + amountCents + ":" + raw.hashCode();
        return parsed;
    }

    static ParsedPayment parseRawText(String packageName, String rawText, long occurredAt) {
        if (!WATCHED_PACKAGES.contains(packageName)) {
            return null;
        }
        String raw = normalize(rawText);
        if (raw.length() == 0) {
            return null;
        }
        String type = isBankPackage(packageName) ? detectBankType(raw) : detectType(raw);
        if (type == null && isBankPackage(packageName) && looksLikeBankMovement(raw)) {
            type = "expense";
        }
        if (type == null) {
            return null;
        }
        Long amountCents = "com.taobao.taobao".equals(packageName)
                ? findLabeledAmount(raw)
                : (isBankPackage(packageName) ? findBankAmount(raw) : findPreferredAmount(raw));
        if (amountCents == null) {
            if ("com.taobao.taobao".equals(packageName) && raw.contains("支付成功")) {
                amountCents = 0L;
            } else {
                return null;
            }
        }
        if (amountCents < 0) {
            return null;
        }
        String merchant = findMerchant(raw, sourceName(packageName));
        if ("com.taobao.taobao".equals(packageName)) {
            String taobaoTitle = findTaobaoTitle(raw);
            if (taobaoTitle.length() > 0) {
                merchant = taobaoTitle;
            }
        }
        ParsedPayment parsed = new ParsedPayment();
        parsed.type = type;
        parsed.amountCents = amountCents;
        parsed.sourcePackage = packageName;
        parsed.sourceApp = sourceName(packageName);
        parsed.rawText = raw;
        parsed.merchant = merchant;
        parsed.occurredAt = occurredAt > 0 ? occurredAt : System.currentTimeMillis();
        long bucket = parsed.occurredAt / 120000L;
        parsed.notificationKey = "access:" + packageName + ":" + type + ":" + amountCents + ":" + bucket + ":" + Math.abs(raw.hashCode());
        return parsed;
    }

    private static String detectType(String raw) {
        if (containsAny(raw, "到账", "入账", "收到转账", "转入", "退款", "收入", "收款到账")) {
            return "income";
        }
        if (containsAny(raw, "支付", "付款", "消费", "扣款", "支出", "已付", "交易成功", "扫码", "动账提醒")) {
            return "expense";
        }
        return null;
    }

    private static String detectBankType(String raw) {
        if (containsAny(raw,
                "入账", "到账", "收款", "收入", "转入", "收到", "贷记", "来账", "存入", "退款", "充值")) {
            return "income";
        }
        if (containsAny(raw,
                "扣款", "消费", "支出", "付款", "支付", "转出", "借记", "快捷支付", "取现", "缴费")) {
            return "expense";
        }
        if (containsAny(raw, "动账提醒", "动账", "账户变动", "交易提醒")) {
            return "expense";
        }
        return null;
    }

    private static boolean containsAny(String text, String... values) {
        for (String value : values) {
            if (text.contains(value)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isBankPackage(String packageName) {
        return "com.chinamworld.main".equals(packageName)
                || "com.chinamworld.bocmbci".equals(packageName)
                || "com.bankcomm.Bankcomm".equals(packageName)
                || "cmb.pb".equals(packageName);
    }

    private static boolean looksLikeBankMovement(String raw) {
        return containsAny(raw, "动账提醒", "动账", "账户变动", "交易提醒", "借记卡", "银行卡");
    }

    private static Long findPreferredAmount(String raw) {
        Long labeled = findLabeledAmount(raw);
        return labeled != null ? labeled : findAmount(raw);
    }

    private static Long findAmount(String raw) {
        Matcher matcher = AMOUNT_PATTERN.matcher(raw);
        while (matcher.find()) {
            String before = raw.substring(Math.max(0, matcher.start() - 8), matcher.start());
            String after = raw.substring(matcher.end(), Math.min(raw.length(), matcher.end() + 8));
            String context = before + matcher.group() + after;
            if (!containsAny(context, "元", "￥", "¥", "人民币", "RMB", "CNY")) {
                continue;
            }
            return centsFromGroups(matcher.group(1), matcher.group(2));
        }
        return null;
    }

    private static Long findLabeledAmount(String raw) {
        Matcher matcher = LABELED_AMOUNT_PATTERN.matcher(raw);
        if (!matcher.find()) {
            return null;
        }
        return centsFromGroups(matcher.group(1), matcher.group(2));
    }

    private static Long findBankAmount(String raw) {
        Long labeled = findLabeledAmount(raw);
        if (labeled != null) {
            return labeled;
        }
        Matcher matcher = BANK_AMOUNT_PATTERN.matcher(raw);
        if (matcher.find()) {
            return centsFromGroups(matcher.group(1), matcher.group(2));
        }
        return findAmount(raw);
    }

    private static Long centsFromGroups(String integerPart, String fractionPart) {
        String integer = integerPart.replace(",", "");
        String fraction = fractionPart == null ? "00" : fractionPart;
        if (fraction.length() == 1) {
            fraction = fraction + "0";
        }
        try {
            BigDecimal yuan = new BigDecimal(integer + "." + fraction);
            return yuan.movePointRight(2).longValue();
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static String findMerchant(String raw, String title) {
        Matcher payTo = MERCHANT_PAY_TO.matcher(raw);
        if (payTo.find()) {
            return cleanMerchant(payTo.group(1));
        }
        Matcher label = MERCHANT_LABEL.matcher(raw);
        if (label.find()) {
            return cleanMerchant(label.group(1));
        }
        if (title != null && title.trim().length() > 0) {
            return cleanMerchant(title);
        }
        return "";
    }

    private static String cleanMerchant(String merchant) {
        return merchant == null ? "" : merchant.replaceAll("\\s+", " ").trim();
    }

    private static String findTaobaoTitle(String raw) {
        Matcher matcher = TAOBAO_SUCCESS_TITLE.matcher(raw);
        if (matcher.find()) {
            return cleanMerchant(matcher.group(1));
        }
        return "";
    }

    private static String sourceName(String packageName) {
        switch (packageName) {
            case "com.tencent.mm":
                return "微信";
            case "com.eg.android.AlipayGphone":
                return "支付宝";
            case "com.taobao.taobao":
                return "淘宝";
            case "com.chinamworld.main":
            case "com.chinamworld.bocmbci":
                return "中国银行";
            case "com.bankcomm.Bankcomm":
                return "交通银行";
            case "cmb.pb":
                return "招商银行";
            default:
                return packageName;
        }
    }

    private static String valueOf(Bundle extras, String key) {
        if (extras == null) {
            return "";
        }
        Object value = extras.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private static String linesOf(Bundle extras) {
        if (extras == null) {
            return "";
        }
        CharSequence[] lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
        if (lines == null || lines.length == 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (CharSequence line : lines) {
            if (line != null) {
                builder.append(' ').append(line);
            }
        }
        return builder.toString();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.replace('\n', ' ').replaceAll("\\s+", " ").trim();
    }

    static String formatMoney(long cents) {
        return String.format(Locale.CHINA, "%.2f", cents / 100.0);
    }
}
