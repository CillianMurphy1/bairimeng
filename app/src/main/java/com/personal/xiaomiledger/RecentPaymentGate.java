package com.personal.xiaomiledger;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.LinkedHashSet;

final class RecentPaymentGate {
    private static final String PREFS = "recent_payment_gate";
    private static final String KEYS = "keys";
    private static final int MAX_KEYS = 120;

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

    private static String fingerprint(ParsedPayment payment) {
        long bucket = payment.occurredAt / 120000L;
        String extra = payment.amountCents <= 0 && payment.rawText != null ? "|" + payment.rawText.hashCode() : "";
        return payment.sourceApp + "|" + payment.type + "|" + payment.amountCents + "|" + bucket + extra;
    }
}
