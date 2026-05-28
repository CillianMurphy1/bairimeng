package com.personal.xiaomiledger;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

final class PrefsManager {
    private static final String PREFS = "customization";
    private static final String KEY_DISPLAY_NAME = "display_name";
    private static final String KEY_SIGNATURE = "signature";
    private static final String KEY_NOTIFY_NAME = "notify_name";
    private static final String KEY_BG_PATH = "bg_path";
    private static final String KEY_AVATAR_PATH = "avatar_path";

    private static final String DEFAULT_DISPLAY = "白日夢";
    private static final String DEFAULT_SIGNATURE = "本地私用记账 · 油屋风格";
    private static final String DEFAULT_NOTIFY = "安乃近提醒";

    private PrefsManager() {}

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static String getDisplayName(Context ctx) {
        return prefs(ctx).getString(KEY_DISPLAY_NAME, DEFAULT_DISPLAY);
    }

    static void setDisplayName(Context ctx, String name) {
        prefs(ctx).edit().putString(KEY_DISPLAY_NAME, name).apply();
    }

    static String getSignature(Context ctx) {
        return prefs(ctx).getString(KEY_SIGNATURE, DEFAULT_SIGNATURE);
    }

    static void setSignature(Context ctx, String sig) {
        prefs(ctx).edit().putString(KEY_SIGNATURE, sig).apply();
    }

    static String getNotifyName(Context ctx) {
        return prefs(ctx).getString(KEY_NOTIFY_NAME, DEFAULT_NOTIFY);
    }

    static void setNotifyName(Context ctx, String name) {
        prefs(ctx).edit().putString(KEY_NOTIFY_NAME, name).apply();
    }

    static String getBackgroundPath(Context ctx) {
        return prefs(ctx).getString(KEY_BG_PATH, "");
    }

    static void setBackgroundPath(Context ctx, String path) {
        prefs(ctx).edit().putString(KEY_BG_PATH, path).apply();
    }

    static String getAvatarPath(Context ctx) {
        return prefs(ctx).getString(KEY_AVATAR_PATH, "");
    }

    static void setAvatarPath(Context ctx, String path) {
        prefs(ctx).edit().putString(KEY_AVATAR_PATH, path).apply();
    }

    static String saveImageToPrivate(Context ctx, Uri uri, String fileName) {
        try {
            InputStream in = ctx.getContentResolver().openInputStream(uri);
            if (in == null) return null;
            Bitmap bitmap = BitmapFactory.decodeStream(in);
            in.close();
            if (bitmap == null) return null;
            int max = 1080;
            if (bitmap.getWidth() > max || bitmap.getHeight() > max) {
                float scale = Math.min((float) max / bitmap.getWidth(), (float) max / bitmap.getHeight());
                int w = Math.round(bitmap.getWidth() * scale);
                int h = Math.round(bitmap.getHeight() * scale);
                bitmap = Bitmap.createScaledBitmap(bitmap, w, h, true);
            }
            File dir = new File(ctx.getFilesDir(), "custom");
            if (!dir.exists()) dir.mkdirs();
            File out = new File(dir, fileName);
            FileOutputStream fos = new FileOutputStream(out);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos);
            fos.close();
            bitmap.recycle();
            return out.getAbsolutePath();
        } catch (Exception e) {
            return null;
        }
    }

    static Drawable loadBackgroundDrawable(Context ctx) {
        String path = getBackgroundPath(ctx);
        File file = path.isEmpty() ? null : new File(path);
        if (file != null && file.exists()) {
            return Drawable.createFromPath(path);
        }
        return ctx.getDrawable(R.drawable.back_ground);
    }

    static Drawable loadAvatarDrawable(Context ctx, int sizeDp) {
        String path = getAvatarPath(ctx);
        File file = path.isEmpty() ? null : new File(path);
        if (file != null && file.exists()) {
            return Drawable.createFromPath(path);
        }
        return null;
    }

    static boolean hasCustomBackground(Context ctx) {
        String path = getBackgroundPath(ctx);
        return !path.isEmpty() && new File(path).exists();
    }

    static void clearBackground(Context ctx) {
        String path = getBackgroundPath(ctx);
        if (!path.isEmpty()) {
            File file = new File(path);
            if (file.exists()) file.delete();
        }
        prefs(ctx).edit().remove(KEY_BG_PATH).apply();
    }

    static void clearAvatar(Context ctx) {
        String path = getAvatarPath(ctx);
        if (!path.isEmpty()) {
            File file = new File(path);
            if (file.exists()) file.delete();
        }
        prefs(ctx).edit().remove(KEY_AVATAR_PATH).apply();
    }

    // ── balance visibility ──
    private static final String KEY_HIDDEN_BALANCES = "hidden_balances";

    static Set<String> getHiddenBalanceAccounts(Context ctx) {
        return prefs(ctx).getStringSet(KEY_HIDDEN_BALANCES, Collections.<String>emptySet());
    }

    static boolean isAccountBalanceHidden(Context ctx, String accountName) {
        return getHiddenBalanceAccounts(ctx).contains(accountName);
    }

    static void toggleAccountBalanceHidden(Context ctx, String accountName) {
        Set<String> set = new HashSet<>(getHiddenBalanceAccounts(ctx));
        if (set.contains(accountName)) {
            set.remove(accountName);
        } else {
            set.add(accountName);
        }
        prefs(ctx).edit().putStringSet(KEY_HIDDEN_BALANCES, set).apply();
    }

    static void hideAllBalances(Context ctx, java.util.List<Account> accounts) {
        Set<String> set = new HashSet<>();
        for (Account a : accounts) {
            if (a.active) set.add(a.name);
        }
        prefs(ctx).edit().putStringSet(KEY_HIDDEN_BALANCES, set).apply();
    }

    static void showAllBalances(Context ctx) {
        prefs(ctx).edit().putStringSet(KEY_HIDDEN_BALANCES, Collections.<String>emptySet()).apply();
    }

    static boolean areAllBalancesHidden(Context ctx, java.util.List<Account> accounts) {
        Set<String> hidden = getHiddenBalanceAccounts(ctx);
        if (hidden.isEmpty()) return false;
        for (Account a : accounts) {
            if (a.active && !hidden.contains(a.name)) return false;
        }
        return true;
    }

    static String formatBalanceIfVisible(Context ctx, String accountName, long cents, java.util.function.Function<Long, String> formatter) {
        if (isAccountBalanceHidden(ctx, accountName)) {
            return "****";
        }
        return formatter.apply(cents);
    }
}
