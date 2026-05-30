package com.personal.xiaomiledger;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import java.util.LinkedHashSet;
import java.util.Set;

public class PaymentNotificationListener extends NotificationListenerService {
    private static final String PREFS = "listener_state";
    private static final String RECENT_KEYS = "recent_keys";
    private static final int MAX_RECENT = 80;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Set<String> delayedWechatKeys = new LinkedHashSet<>();

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        StatusBarNotification[] activeNotifications = getActiveNotifications();
        if (activeNotifications == null) {
            return;
        }
        for (StatusBarNotification sbn : activeNotifications) {
            handleNotification(sbn, true);
        }
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        handleNotification(sbn, false);
    }

    private void handleNotification(StatusBarNotification sbn, boolean fromActiveScan) {
        String raw = PaymentParser.rawText(sbn);
        String packageName = sbn == null ? "" : sbn.getPackageName();
        if (getPackageName().equals(packageName)) {
            return;
        }
        boolean interesting = PaymentParser.isWatchedOrBankLike(packageName, raw);
        TransactionStore store = interesting ? new TransactionStore(this) : null;
        if (interesting) {
            store.logAutoRecord("seen", PaymentParser.sourceNameForPackage(packageName), raw,
                    fromActiveScan ? "监听器补扫收到通知" : "监听器收到通知", 0);
        }
        ParsedPayment payment = PaymentParser.parse(sbn);
        if (payment == null) {
            if (interesting) {
                store.logAutoRecord("ignored", PaymentParser.sourceNameForPackage(packageName), raw,
                        "解析失败：没有识别到明确金额、收支方向或动账关键词", 0);
            }
            return;
        }
        payment = PaymentContextStore.enrichBankPayment(this, payment);
        if (store == null) {
            store = new TransactionStore(this);
        }
        if (store.hasNotificationKey(payment.notificationKey)
                || isRecentlySeen(payment.notificationKey)) {
            store.logAutoRecord("duplicate", payment.sourceApp, payment.rawText, "重复通知，已忽略", payment.amountCents);
            return;
        }
        if (PaymentContextStore.shouldDeferWechatCardPaymentToBank(payment)) {
            store.logAutoRecord("ignored", payment.sourceApp, payment.rawText,
                    "微信银行卡支付通知，等待银行动账通知入账，避免重复记录", payment.amountCents);
            return;
        }
        if (RecentPaymentGate.shouldSkipWechatAfterRecentBank(this, payment)) {
            store.logAutoRecord("duplicate", payment.sourceApp, payment.rawText,
                    "近期已有同金额银行支出，微信通知已忽略，避免重复记录", payment.amountCents);
            return;
        }
        if (!fromActiveScan && RecentPaymentGate.shouldWaitForPossibleBankPayment(this, payment)) {
            delayWechatPayment(payment);
            return;
        }
        continuePayment(payment, store, fromActiveScan);
    }

    private void continuePayment(ParsedPayment payment, TransactionStore store, boolean fromActiveScan) {
        if (store.hasNotificationKey(payment.notificationKey)
                || isRecentlySeen(payment.notificationKey)) {
            store.logAutoRecord("duplicate", payment.sourceApp, payment.rawText, "重复通知，已忽略", payment.amountCents);
            return;
        }
        if (RecentPaymentGate.shouldSkipWechatAfterRecentBank(this, payment)) {
            store.logAutoRecord("duplicate", payment.sourceApp, payment.rawText,
                    "近期已有同金额银行支出，微信通知已忽略，避免重复记录", payment.amountCents);
            return;
        }
        if (RecentPaymentGate.shouldSkipAndRemember(this, payment)) {
            store.logAutoRecord("duplicate", payment.sourceApp, payment.rawText, "近期已由其他方式识别，已忽略", payment.amountCents);
            return;
        }
        remember(payment.notificationKey);
        RecentPaymentGate.rememberBankExpense(this, payment);
        RecentPaymentGate.rememberBankIncome(this, payment);
        String category = ClassificationRules.inferCategory(payment.rawText, payment.sourceApp, payment.merchant, payment.type);
        String account = store.inferAccount(payment.rawText, payment.sourceApp);
        store.logAutoRecord("recognized", payment.sourceApp, payment.rawText,
                (fromActiveScan ? "补扫识别：" : "已识别：") + category + " / " + account, payment.amountCents);
        if (payment.amountCents <= 0 && PaymentContextStore.isBankSource(payment)) {
            store.logAutoRecord("ignored", payment.sourceApp, payment.rawText,
                    "银行通知缺少金额，且没有匹配到支付页面上下文，转入确认页补全", payment.amountCents);
            NotificationHelper.showPending(this, payment);
            tryLaunchEditor(payment);
            return;
        }
        if (AutoSaveManager.tryAutoSave(this, store, payment)) {
            return;
        }
        NotificationHelper.showPending(this, payment);
        tryLaunchEditor(payment);
    }

    private void delayWechatPayment(ParsedPayment payment) {
        String key = payment.notificationKey == null || payment.notificationKey.length() == 0
                ? String.valueOf(System.currentTimeMillis())
                : payment.notificationKey;
        synchronized (delayedWechatKeys) {
            if (delayedWechatKeys.contains(key)) {
                return;
            }
            delayedWechatKeys.add(key);
        }
        TransactionStore store = new TransactionStore(this);
        store.logAutoRecord("seen", payment.sourceApp, payment.rawText,
                "微信支付通知等待银行动账 " + (RecentPaymentGate.crossSourceWindowMs() / 1000L) + " 秒", payment.amountCents);
        mainHandler.postDelayed(() -> {
            synchronized (delayedWechatKeys) {
                delayedWechatKeys.remove(key);
            }
            TransactionStore delayedStore = new TransactionStore(this);
            continuePayment(payment, delayedStore, false);
        }, RecentPaymentGate.crossSourceWindowMs());
    }

    private void tryLaunchEditor(ParsedPayment payment) {
        try {
            Intent intent = EditTransactionActivity.intentForPayment(this, payment);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        } catch (RuntimeException ignored) {
            // The high-priority notification remains as the reliable fallback on newer Android versions.
        }
    }

    private boolean isRecentlySeen(String key) {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        return prefs.getStringSet(RECENT_KEYS, new LinkedHashSet<>()).contains(key);
    }

    private void remember(String key) {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        LinkedHashSet<String> keys = new LinkedHashSet<>(prefs.getStringSet(RECENT_KEYS, new LinkedHashSet<>()));
        keys.add(key);
        while (keys.size() > MAX_RECENT) {
            String first = keys.iterator().next();
            keys.remove(first);
        }
        prefs.edit().putStringSet(RECENT_KEYS, keys).apply();
    }
}
