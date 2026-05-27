package com.personal.xiaomiledger;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class LedgerWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int widgetId : appWidgetIds) {
            updateWidget(context, appWidgetManager, widgetId);
        }
    }

    static void updateAllWidgets(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(
                new android.content.ComponentName(context, LedgerWidgetProvider.class));
        for (int id : ids) {
            updateWidget(context, manager, id);
        }
    }

    private static void updateWidget(Context context, AppWidgetManager manager, int widgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_ledger);

        TransactionStore store = new TransactionStore(context);
        long[] range = TransactionStore.currentMonthRange();
        long income = store.sumBetween("income", range[0], range[1]);
        long expense = store.sumBetween("expense", range[0], range[1]);

        String monthLabel = new SimpleDateFormat("yyyy年M月", Locale.CHINA).format(Calendar.getInstance().getTime());
        views.setTextViewText(R.id.widget_month, monthLabel);
        views.setTextViewText(R.id.widget_income, PaymentParser.formatMoney(income));
        views.setTextViewText(R.id.widget_expense, PaymentParser.formatMoney(expense));
        views.setTextViewText(R.id.widget_balance, TransactionStore.formatMoneySigned(income - expense));

        // Click to open app
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pending = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_title, pending);

        manager.updateAppWidget(widgetId, views);
    }
}
