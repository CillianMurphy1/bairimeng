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
            "cmb.pb",
            "com.czbank.mbank"
    ));

    private static final Pattern AMOUNT_PATTERN = Pattern.compile(
            "(?:人民币|RMB|CNY|￥|¥)?\\s*([0-9]{1,3}(?:,[0-9]{3})+|[0-9]+)(?:\\.([0-9]{1,2}))?(?![0-9,.])\\s*元?");
    private static final Pattern LABELED_AMOUNT_PATTERN = Pattern.compile(
            "(?:实付|实付款|支付金额|付款金额|订单金额|合计|共计|扣款金额|消费金额|交易金额|支出金额|入账金额|到账金额|退款金额)[:：\\s]*(?:人民币|RMB|CNY|￥|¥)?\\s*([0-9]{1,3}(?:,[0-9]{3})+|[0-9]+)(?:\\.([0-9]{1,2}))?(?![0-9,.])\\s*元?");
    private static final Pattern BANK_AMOUNT_PATTERN = Pattern.compile(
            "(?:动账|交易|消费|支出|扣款|付款|支付|入账|到账|转入|增加)[^0-9￥¥]{0,24}(?:人民币|RMB|CNY|￥|¥)?\\s*([0-9]{1,3}(?:,[0-9]{3})+|[0-9]+)(?:\\.([0-9]{1,2}))?(?![0-9,.])\\s*元?");
    private static final Pattern MERCHANT_PAY_TO = Pattern.compile("(?:支付给|付款给|转账给|向)([^，,。；;\\n]{2,24})");
    private static final Pattern MERCHANT_LABEL = Pattern.compile("(?:商户|收款方|对方|店铺)[:：\\s]+([^，,。；;\\n]{2,24})");
    private static final Pattern TAOBAO_SUCCESS_TITLE = Pattern.compile("支付成功\\s+(.{2,50}?)(?:\\s+查看订单|\\s+本单奖励|\\s+该宝贝|$)");
    private static final Pattern OBSERVED_THIRD_PARTY_FINANCE_PATTERN = Pattern.compile(
            "(?:你|您)\\s*关注的\\s*@?[^，,。；;\\n]{0,60}(?:有\\s*(?:1|一)\\s*笔|黄金交易|买入|卖出|买金|卖金|支付|付款|收入|支出|到账)");

    private PaymentParser() {
    }

    static ParsedPayment parse(StatusBarNotification sbn) {
        if (sbn == null || sbn.getNotification() == null) {
            return null;
        }
        String packageName = sbn.getPackageName();

        String raw = rawText(sbn);
        if (raw.length() == 0) {
            return null;
        }
        if (isObservedThirdPartyFinanceNotice(raw)) {
            return null;
        }
        if (isPromotionalOrQuotaNotice(raw) && !isGoldTradeNotice(raw) && !isTransitCardNotification(raw)) {
            return null;
        }
        boolean watchedPackage = WATCHED_PACKAGES.contains(packageName);
        boolean bankMovementPackage = !watchedPackage && looksLikeBankMovement(raw);
        boolean transitCardPackage = isTransitCardNotification(raw);
        if (!watchedPackage && !bankMovementPackage && !transitCardPackage) {
            return null;
        }
        String type = transitCardPackage ? detectTransitType(raw) : (isBankPackage(packageName) ? detectBankType(raw) : detectType(raw));
        if (type == null && (isBankPackage(packageName) || bankMovementPackage) && looksLikeBankMovement(raw)) {
            type = "expense";
        }
        if (type == null) {
            return null;
        }
        Long amountCents = "com.taobao.taobao".equals(packageName)
                ? findLabeledAmount(raw)
                : (transitCardPackage ? findAmount(raw) : ((isBankPackage(packageName) || bankMovementPackage) ? findBankAmount(raw) : findPreferredAmount(raw)));
        if (amountCents == null) {
            if (("com.taobao.taobao".equals(packageName) && raw.contains("支付成功"))
                    || (isWechatPackage(packageName) && isWechatIncomeReceipt(raw))
                    || ((isBankPackage(packageName) || bankMovementPackage) && looksLikeBankMovement(raw))) {
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
        parsed.sourceApp = sourceName(packageName, raw);
        parsed.rawText = raw;
        parsed.merchant = transitCardPackage ? findTransitMerchant(raw) : findMerchant(raw, sourceName(packageName, raw));
        if ("com.taobao.taobao".equals(packageName)) {
            String taobaoTitle = findTaobaoTitle(raw);
            if (taobaoTitle.length() > 0) {
                parsed.merchant = taobaoTitle;
            }
        }
        parsed.occurredAt = sbn.getPostTime() > 0 ? sbn.getPostTime() : System.currentTimeMillis();
        parsed.notificationKey = notificationKey(packageName, type, amountCents, raw, parsed.occurredAt);
        return parsed;
    }

    static ParsedPayment parseRawText(String packageName, String rawText, long occurredAt) {
        boolean watchedPackage = WATCHED_PACKAGES.contains(packageName);
        boolean bankMovementPackage = !watchedPackage && looksLikeBankMovement(rawText == null ? "" : rawText);
        boolean transitCardPackage = isTransitCardNotification(rawText == null ? "" : rawText);
        if (!watchedPackage && !bankMovementPackage && !transitCardPackage) {
            return null;
        }
        String raw = normalize(rawText);
        if (raw.length() == 0) {
            return null;
        }
        if (isObservedThirdPartyFinanceNotice(raw)) {
            return null;
        }
        if (isPromotionalOrQuotaNotice(raw) && !isGoldTradeNotice(raw) && !isTransitCardNotification(raw)) {
            return null;
        }
        String type = transitCardPackage ? detectTransitType(raw) : ((isBankPackage(packageName) || bankMovementPackage) ? detectBankType(raw) : detectType(raw));
        if (type == null && (isBankPackage(packageName) || bankMovementPackage) && looksLikeBankMovement(raw)) {
            type = "expense";
        }
        if (type == null) {
            return null;
        }
        Long amountCents = "com.taobao.taobao".equals(packageName)
                ? findLabeledAmount(raw)
                : (transitCardPackage ? findAmount(raw) : ((isBankPackage(packageName) || bankMovementPackage) ? findBankAmount(raw) : findPreferredAmount(raw)));
        if (amountCents == null) {
            if (("com.taobao.taobao".equals(packageName) && raw.contains("支付成功"))
                    || (isWechatPackage(packageName) && isWechatIncomeReceipt(raw))) {
                amountCents = 0L;
            } else {
                return null;
            }
        }
        if (amountCents < 0) {
            return null;
        }
        String merchant = transitCardPackage ? findTransitMerchant(raw) : findMerchant(raw, sourceName(packageName, raw));
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
        parsed.sourceApp = sourceName(packageName, raw);
        parsed.rawText = raw;
        parsed.merchant = merchant;
        parsed.occurredAt = occurredAt > 0 ? occurredAt : System.currentTimeMillis();
        long bucket = parsed.occurredAt / 120000L;
        parsed.notificationKey = "access:" + packageName + ":" + type + ":" + amountCents + ":" + bucket + ":" + Math.abs(raw.hashCode());
        return parsed;
    }

    private static String notificationKey(String packageName, String type, long amountCents, String raw, long occurredAt) {
        long time = occurredAt > 0 ? occurredAt : System.currentTimeMillis();
        if (isBankPackage(packageName) || isPaymentAppPackage(packageName) || looksLikeBankMovement(raw) || isTransitCardNotification(raw)) {
            return "notify:" + packageName + ":" + type + ":" + amountCents + ":" + time + ":" + Math.abs(raw.hashCode());
        }
        long bucket = time / 120000L;
        return "notify:" + packageName + ":" + type + ":" + amountCents + ":" + bucket + ":" + Math.abs(raw.hashCode());
    }

    private static String detectType(String raw) {
        String goldType = detectGoldTradeType(raw);
        if (goldType != null) {
            return goldType;
        }
        if (containsAny(raw, "到账", "入账", "收到转账", "转账已收款", "已收款", "转入", "退款", "收入", "收款到账", "退款到账", "退回零钱", "红包已到账")) {
            return "income";
        }
        if (containsAny(raw, "支付", "付款", "消费", "扣款", "支出", "已付", "交易成功", "扫码", "动账提醒")) {
            return "expense";
        }
        return null;
    }

    private static String detectBankType(String raw) {
        String goldType = detectGoldTradeType(raw);
        if (goldType != null) {
            return goldType;
        }
        if (containsAny(raw,
                "入账", "到账", "收款", "收入", "转入", "收到", "贷记", "来账", "存入", "退款", "充值", "增加")) {
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
                || "cmb.pb".equals(packageName)
                || "com.czbank.mbank".equals(packageName);
    }

    private static boolean isWechatPackage(String packageName) {
        return "com.tencent.mm".equals(packageName);
    }

    private static boolean isPaymentAppPackage(String packageName) {
        return "com.tencent.mm".equals(packageName)
                || "com.eg.android.AlipayGphone".equals(packageName)
                || "com.taobao.taobao".equals(packageName);
    }

    private static boolean isWechatIncomeReceipt(String raw) {
        return containsAny(raw, "红包已到账", "微信红包已到账", "收到红包", "转账已收款",
                "收到转账", "收款到账", "已收款", "退款到账", "退回零钱");
    }

    private static boolean isTransitCardNotification(String raw) {
        return raw != null
                && containsAny(raw, "长安通", "互联互通卡")
                && containsAny(raw, "地铁", "公交", "充值", "扣费", "消费", "出站", "进站");
    }

    private static String detectTransitType(String raw) {
        if (containsAny(raw, "充值", "充值成功", "充值到账", "已充值", "充卡")) {
            return "income";
        }
        if (containsAny(raw, "地铁", "公交", "扣费", "消费", "出站", "进站")) {
            return "expense";
        }
        return null;
    }

    private static boolean looksLikeBankMovement(String raw) {
        if (isObservedThirdPartyFinanceNotice(raw)) {
            return false;
        }
        if (isPromotionalOrQuotaNotice(raw) && !isGoldTradeNotice(raw) && !isTransitCardNotification(raw)) {
            return false;
        }
        return isGoldTradeNotice(raw) || containsAny(raw, "动账提醒", "动账", "账户变动", "交易提醒", "借记卡", "银行卡",
                "扣款", "入账", "到账", "支出", "收入", "增加", "交易金额", "消费金额", "快捷支付");
    }

    private static String detectGoldTradeType(String raw) {
        if (!isGoldTradeNotice(raw)) {
            return null;
        }
        if (containsAny(raw, "买金", "买入", "购金", "购买黄金")) {
            return "expense";
        }
        if (containsAny(raw, "卖金", "卖出", "赎回", "卖出黄金")) {
            return "income";
        }
        return null;
    }

    private static boolean isGoldTradeNotice(String raw) {
        return raw != null
                && !isObservedThirdPartyFinanceNotice(raw)
                && containsAny(raw, "黄金", "买金", "卖金")
                && containsAny(raw, "成功", "确认", "成交", "买入", "卖出", "赎回");
    }

    private static boolean isObservedThirdPartyFinanceNotice(String raw) {
        if (raw == null) {
            return false;
        }
        return OBSERVED_THIRD_PARTY_FINANCE_PATTERN.matcher(raw).find()
                || (containsAny(raw, "你关注的", "您关注的", "关注的@", "关注的人", "关注用户")
                && containsAny(raw, "有1笔", "有一笔", "支付", "付款", "收入", "支出", "到账", "黄金", "黄金交易", "买入", "卖出", "买金", "卖金"));
    }

    private static boolean isPromotionalOrQuotaNotice(String raw) {
        if (raw == null || raw.length() == 0) {
            return false;
        }
        if (containsAny(raw, "验证码", "登录", "密码")) {
            return true;
        }
        if ((containsAny(raw, "中国移动", "10086")
                && containsAny(raw, "卡券", "券到账", "充值券", "话费券")
                && containsAny(raw, "到账提醒", "发放成功", "已发放", "查看使用", "本月有效", "中国移动APP"))
                || (containsAny(raw, "卡券到账提醒", "充值券已发放", "充值券发放", "话费券到账")
                && containsAny(raw, "查看使用", "本月有效", "发放成功", "已发放"))) {
            return true;
        }
        if (containsAny(raw, "额度", "预估额度", "授信", "借款额度", "贷款额度", "可借", "可申请")
                && containsAny(raw, "领取", "查收", "查看", "避免失效", "失效", "获", "最高")) {
            return true;
        }
        if (containsAny(raw, "优惠", "特惠", "权益", "活动", "卡券", "充值券", "话费券", "流量", "获赠", "赠送", "礼包", "红包雨", "抽奖")
                && containsAny(raw, "领取", "点击", "链接", "http", "回复", "退订", "用券", "可享", "参与", "规则", "到期")) {
            return true;
        }
        return containsAny(raw, "充值30元到账35元", "充30元到账35元", "充值可享", "充值优惠", "用券充值");
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

    private static String findTransitMerchant(String raw) {
        if (raw == null) {
            return "";
        }
        if (raw.contains("地铁")) return "地铁";
        if (raw.contains("公交")) return "公交";
        if (raw.contains("充值")) return "地铁卡充值";
        return "长安通";
    }

    private static String sourceName(String packageName) {
        return sourceName(packageName, "");
    }

    private static String sourceName(String packageName, String raw) {
        if (isTransitCardNotification(raw)) {
            return "长安通互联互通卡";
        }
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
            case "com.czbank.mbank":
                return "浙商银行";
            default:
                if (isGoldTradeNotice(raw)) {
                    return "浙商银行";
                }
                return packageName;
        }
    }

    static boolean isWatchedOrBankLike(String packageName, String rawText) {
        String raw = rawText == null ? "" : rawText;
        if (isObservedThirdPartyFinanceNotice(raw)) {
            return false;
        }
        if (isPromotionalOrQuotaNotice(raw) && !isGoldTradeNotice(raw) && !isTransitCardNotification(raw)) {
            return false;
        }
        return WATCHED_PACKAGES.contains(packageName) || looksLikeBankMovement(raw) || isTransitCardNotification(raw);
    }

    static String sourceNameForPackage(String packageName) {
        return sourceName(packageName);
    }

    static String rawText(StatusBarNotification sbn) {
        if (sbn == null || sbn.getNotification() == null) {
            return "";
        }
        Notification notification = sbn.getNotification();
        Bundle extras = notification.extras;
        String title = valueOf(extras, Notification.EXTRA_TITLE);
        String text = valueOf(extras, Notification.EXTRA_TEXT);
        String bigText = valueOf(extras, Notification.EXTRA_BIG_TEXT);
        String subText = valueOf(extras, Notification.EXTRA_SUB_TEXT);
        String lines = linesOf(extras);
        return normalize(title + " " + text + " " + bigText + " " + subText + " " + lines);
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
