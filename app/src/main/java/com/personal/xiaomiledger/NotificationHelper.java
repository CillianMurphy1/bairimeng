package com.personal.xiaomiledger;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

final class NotificationHelper {
    private static final String CHANNEL_ID = "anjin_ledger_reminder_v1";

    private NotificationHelper() {
    }

    static void showPending(Context context, ParsedPayment payment) {
        NotificationManager manager = ensureManager(context);
        if (manager == null) {
            return;
        }

        Intent intent = EditTransactionActivity.intentForPayment(context, payment);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                payment.notificationKey.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(context, CHANNEL_ID)
                : new Notification.Builder(context);
        String verb = "income".equals(payment.type) ? "到账" : "支付";
        String amountText = payment.amountCents > 0
                ? PaymentParser.formatMoney(payment.amountCents) + " 元"
                : "金额待填写";
        builder.setSmallIcon(R.drawable.ic_wallet_24)
                .setContentTitle(PrefsManager.getNotifyName(context))
                .setContentText("发现一笔" + verb + "，待确认 · " + payment.sourceApp + " · " + amountText)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_REMINDER)
                .setPriority(Notification.PRIORITY_HIGH)
                .setDefaults(Notification.DEFAULT_ALL);
        builder.setFullScreenIntent(pendingIntent, true);
        manager.notify(payment.notificationKey.hashCode(), builder.build());
    }

    static void showAutoSaved(Context context, Transaction transaction) {
        NotificationManager manager = ensureManager(context);
        if (manager == null) {
            return;
        }

        Intent intent = new Intent(context, SearchActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                ("saved:" + transaction.notificationKey).hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(context, CHANNEL_ID)
                : new Notification.Builder(context);
        String verb = "transfer".equals(transaction.type)
                ? "转账"
                : ("income".equals(transaction.type) ? "收入" : "支出");
        String accountText = transaction.accountName;
        if ("transfer".equals(transaction.type)
                && transaction.targetAccountName != null
                && transaction.targetAccountName.length() > 0) {
            accountText = transaction.accountName + " -> " + transaction.targetAccountName;
        }
        builder.setSmallIcon(R.drawable.ic_wallet_24)
                .setContentTitle(PrefsManager.getNotifyName(context))
                .setContentText("已自动记账：" + verb + " " + PaymentParser.formatMoney(transaction.amountCents)
                        + " 元 · " + accountText + " · " + transaction.category)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_STATUS)
                .setPriority(Notification.PRIORITY_HIGH);
        manager.notify(("saved:" + transaction.notificationKey).hashCode(), builder.build());
    }

    private static NotificationManager ensureManager(Context context) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) {
            return null;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelName = PrefsManager.getNotifyName(context);
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    channelName,
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("白日夢自动记账提醒");
            manager.createNotificationChannel(channel);
        }
        return manager;
    }
}
