package com.personal.xiaomiledger;

import android.content.Intent;
import android.content.SharedPreferences;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import java.util.LinkedHashSet;
import java.util.Set;

public class PaymentNotificationListener extends NotificationListenerService {
    private static final String PREFS = "listener_state";
    private static final String RECENT_KEYS = "recent_keys";
    private static final int MAX_RECENT = 80;

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        ParsedPayment payment = PaymentParser.parse(sbn);
        if (payment == null) {
            return;
        }
        TransactionStore store = new TransactionStore(this);
        if (store.hasNotificationKey(payment.notificationKey) || isRecentlySeen(payment.notificationKey)) {
            store.logAutoRecord("duplicate", payment.sourceApp, payment.rawText, "重复通知，已忽略", payment.amountCents);
            return;
        }
        if (RecentPaymentGate.shouldSkipAndRemember(this, payment)) {
            store.logAutoRecord("duplicate", payment.sourceApp, payment.rawText, "近期已由其他方式识别，已忽略", payment.amountCents);
            return;
        }
        remember(payment.notificationKey);
        String category = ClassificationRules.inferCategory(payment.rawText, payment.sourceApp, payment.merchant, payment.type);
        String account = ClassificationRules.inferAccount(payment.rawText, payment.sourceApp);
        store.logAutoRecord("recognized", payment.sourceApp, payment.rawText,
                "已识别：" + category + " / " + account, payment.amountCents);
        if (AutoSaveManager.tryAutoSave(this, store, payment)) {
            return;
        }
        NotificationHelper.showPending(this, payment);
        tryLaunchEditor(payment);
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
