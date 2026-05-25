package com.personal.xiaomiledger;

import java.util.Locale;

final class ClassificationRules {
    private ClassificationRules() {
    }

    static String inferCategory(String rawText, String sourceApp, String merchant, String type) {
        String text = normalize(rawText + " " + sourceApp + " " + merchant);
        if ("income".equals(type)) {
            if (containsAny(text, "工资", "薪资", "薪水", "工资卡")) return "工资";
            if (containsAny(text, "红包", "转账收款", "收款到账")) return "收红包";
            if (containsAny(text, "基金", "股票", "理财", "收益", "分红")) return "股票基金";
            if (containsAny(text, "兼职", "外快", "稿费")) return "外快";
            return "其它";
        }

        if (containsAny(text, "美团", "外卖", "饿了么", "麦当劳", "肯德基", "瑞幸", "星巴克", "饭", "餐", "小吃", "面", "粉")) {
            return "三餐";
        }
        if (containsAny(text, "滴滴", "打车", "出租", "地铁", "公交", "高德", "停车", "加油", "高速")) {
            return "交通";
        }
        if (containsAny(text, "淘宝", "天猫", "京东", "拼多多", "闲鱼", "超市", "便利店", "闪送")) {
            return "日用品";
        }
        if (containsAny(text, "话费", "移动", "联通", "电信", "宽带", "流量")) {
            return "话费网费";
        }
        if (containsAny(text, "医院", "药", "门诊", "医保", "体检")) {
            return "医疗";
        }
        if (containsAny(text, "电影", "游戏", "会员", "音乐", "视频", "ktv", "KTV")) {
            return "娱乐";
        }
        if (containsAny(text, "水费", "电费", "燃气", "煤气")) {
            return "水电煤";
        }
        if (containsAny(text, "房租", "物业", "住房")) {
            return "住房";
        }
        if (containsAny(text, "衣服", "服饰", "优衣库", "鞋")) {
            return "衣服";
        }
        if (containsAny(text, "酒店", "机票", "火车票", "携程", "去哪儿", "旅行")) {
            return "旅行";
        }
        if (containsAny(text, "红包")) {
            return "发红包";
        }
        return "其它";
    }

    static String inferAccount(String rawText, String sourceApp) {
        String text = normalize(rawText + " " + sourceApp);
        if (containsAny(text, "招商银行", "招行", "cmb")) return "招商银行";
        if (containsAny(text, "中国银行", "中行", "BOC")) return "中国银行";
        if (containsAny(text, "交通银行", "交行")) return "交通银行";
        if (containsAny(text, "微信零钱", "微信支付", "微信")) return "微信零钱";
        if (containsAny(text, "支付宝余额", "余额宝", "支付宝")) return "支付宝余额";
        if (containsAny(text, "京东金融", "京东")) return "京东金融";
        return "未确认账户";
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.CHINA);
    }

    private static boolean containsAny(String text, String... needles) {
        if (text == null || text.length() == 0) {
            return false;
        }
        for (String needle : needles) {
            if (text.contains(needle.toLowerCase(Locale.CHINA))) {
                return true;
            }
        }
        return false;
    }
}
