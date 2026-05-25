package com.personal.xiaomiledger;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

final class TransactionStore extends SQLiteOpenHelper {
    private static final String DB_NAME = "personal_ledger.db";
    private static final int DB_VERSION = 3;

    TransactionStore(Context context) {
        super(context.getApplicationContext(), DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createAll(db);
        seedDefaultBook(db);
        seedDefaultAccounts(db);
        seedDefaultCategories(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            createAll(db);
            seedDefaultBook(db);
            seedDefaultAccounts(db);
            seedDefaultCategories(db);
        }
        if (oldVersion < 3) {
            createAll(db);
            safeAddColumn(db, "transactions", "target_account TEXT");
            safeAddColumn(db, "transactions", "tags TEXT");
            safeAddColumn(db, "transactions", "book_id INTEGER NOT NULL DEFAULT 1");
            safeAddColumn(db, "transactions", "is_reimbursement INTEGER NOT NULL DEFAULT 0");
            safeAddColumn(db, "transactions", "refund_original_id INTEGER NOT NULL DEFAULT 0");
            safeAddColumn(db, "transactions", "updated_at INTEGER NOT NULL DEFAULT 0");
            safeAddColumn(db, "accounts", "account_type TEXT NOT NULL DEFAULT 'asset'");
            safeAddColumn(db, "accounts", "currency TEXT NOT NULL DEFAULT 'CNY'");
            safeAddColumn(db, "accounts", "include_in_total INTEGER NOT NULL DEFAULT 1");
            safeAddColumn(db, "accounts", "credit_limit_cents INTEGER NOT NULL DEFAULT 0");
            safeAddColumn(db, "accounts", "bill_date INTEGER NOT NULL DEFAULT 0");
            safeAddColumn(db, "accounts", "due_date INTEGER NOT NULL DEFAULT 0");
            seedDefaultBook(db);
            seedDefaultCategories(db);
        }
    }

    private void createAll(SQLiteDatabase db) {
        createTransactions(db);
        createAccounts(db);
        createBooks(db);
        createCategories(db);
        createBudgets(db);
        createAutoLogs(db);
    }

    private void createTransactions(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS transactions (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "type TEXT NOT NULL," +
                "amount_cents INTEGER NOT NULL," +
                "source_app TEXT," +
                "account_name TEXT," +
                "target_account TEXT," +
                "category TEXT," +
                "merchant TEXT," +
                "note TEXT," +
                "tags TEXT," +
                "raw_text TEXT," +
                "notification_key TEXT UNIQUE," +
                "book_id INTEGER NOT NULL DEFAULT 1," +
                "is_reimbursement INTEGER NOT NULL DEFAULT 0," +
                "refund_original_id INTEGER NOT NULL DEFAULT 0," +
                "occurred_at INTEGER NOT NULL," +
                "created_at INTEGER NOT NULL," +
                "updated_at INTEGER NOT NULL DEFAULT 0" +
                ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_transactions_occurred_at ON transactions(occurred_at DESC)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_transactions_category ON transactions(category)");
    }

    private void createAccounts(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS accounts (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT NOT NULL UNIQUE," +
                "kind TEXT NOT NULL," +
                "account_type TEXT NOT NULL DEFAULT 'asset'," +
                "currency TEXT NOT NULL DEFAULT 'CNY'," +
                "include_in_total INTEGER NOT NULL DEFAULT 1," +
                "balance_cents INTEGER NOT NULL DEFAULT 0," +
                "credit_limit_cents INTEGER NOT NULL DEFAULT 0," +
                "bill_date INTEGER NOT NULL DEFAULT 0," +
                "due_date INTEGER NOT NULL DEFAULT 0," +
                "sort_order INTEGER NOT NULL DEFAULT 0," +
                "created_at INTEGER NOT NULL" +
                ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_accounts_sort ON accounts(sort_order ASC, id ASC)");
    }

    private void createBooks(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS books (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT NOT NULL UNIQUE," +
                "is_default INTEGER NOT NULL DEFAULT 0," +
                "created_at INTEGER NOT NULL" +
                ")");
    }

    private void createCategories(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS categories (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT NOT NULL," +
                "type TEXT NOT NULL," +
                "parent_name TEXT," +
                "icon TEXT," +
                "sort_order INTEGER NOT NULL DEFAULT 0," +
                "is_active INTEGER NOT NULL DEFAULT 1," +
                "UNIQUE(name, type)" +
                ")");
    }

    private void createBudgets(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS budgets (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "period TEXT NOT NULL," +
                "category TEXT," +
                "amount_cents INTEGER NOT NULL," +
                "start_at INTEGER NOT NULL," +
                "end_at INTEGER NOT NULL," +
                "alert_threshold INTEGER NOT NULL DEFAULT 80," +
                "UNIQUE(period, category, start_at, end_at)" +
                ")");
    }

    private void createAutoLogs(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS auto_record_logs (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "status TEXT NOT NULL," +
                "source_app TEXT," +
                "raw_text TEXT," +
                "message TEXT," +
                "amount_cents INTEGER NOT NULL DEFAULT 0," +
                "created_at INTEGER NOT NULL" +
                ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_auto_logs_created ON auto_record_logs(created_at DESC)");
    }

    private void safeAddColumn(SQLiteDatabase db, String table, String columnSql) {
        try {
            db.execSQL("ALTER TABLE " + table + " ADD COLUMN " + columnSql);
        } catch (RuntimeException ignored) {
        }
    }

    private void seedDefaultBook(SQLiteDatabase db) {
        ContentValues values = new ContentValues();
        values.put("name", "日常账本");
        values.put("is_default", 1);
        values.put("created_at", System.currentTimeMillis());
        db.insertWithOnConflict("books", null, values, SQLiteDatabase.CONFLICT_IGNORE);
    }

    private void seedDefaultAccounts(SQLiteDatabase db) {
        String[][] rows = new String[][]{
                {"中国银行", "银行卡"},
                {"交通银行", "银行卡"},
                {"招商银行", "银行卡"},
                {"微信零钱", "虚拟账户"},
                {"支付宝余额", "虚拟账户"},
                {"京东金融", "投资账户"}
        };
        for (int i = 0; i < rows.length; i++) {
            ContentValues values = new ContentValues();
            values.put("name", rows[i][0]);
            values.put("kind", "asset");
            values.put("account_type", rows[i][1]);
            values.put("currency", "CNY");
            values.put("include_in_total", 1);
            values.put("balance_cents", 0);
            values.put("sort_order", i + 1);
            values.put("created_at", System.currentTimeMillis());
            db.insertWithOnConflict("accounts", null, values, SQLiteDatabase.CONFLICT_IGNORE);
        }
    }

    private void seedDefaultCategories(SQLiteDatabase db) {
        String[] expense = new String[]{"三餐", "零食", "衣服", "交通", "旅行", "孩子", "宠物", "话费网费", "烟酒", "学习", "日用品", "住房", "美妆", "医疗", "发红包", "汽车/加油", "娱乐", "请客送礼", "电器数码", "运动", "其它", "水电煤"};
        String[] income = new String[]{"工资", "生活费", "收红包", "外快", "股票基金", "其它"};
        for (int i = 0; i < expense.length; i++) {
            insertCategory(db, expense[i], "expense", "", String.valueOf(expense[i].charAt(0)), i + 1);
        }
        for (int i = 0; i < income.length; i++) {
            insertCategory(db, income[i], "income", "", String.valueOf(income[i].charAt(0)), i + 1);
        }
    }

    private void insertCategory(SQLiteDatabase db, String name, String type, String parent, String icon, int order) {
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("type", type);
        values.put("parent_name", parent);
        values.put("icon", icon);
        values.put("sort_order", order);
        values.put("is_active", 1);
        db.insertWithOnConflict("categories", null, values, SQLiteDatabase.CONFLICT_IGNORE);
    }

    long insert(Transaction transaction) {
        SQLiteDatabase db = getWritableDatabase();
        normalizeTransaction(transaction);
        long result = db.insertWithOnConflict("transactions", null, transactionValues(transaction), SQLiteDatabase.CONFLICT_IGNORE);
        if (result != -1) {
            applyTransactionToAccount(db, transaction);
        }
        return result;
    }

    long setAccountBalance(String accountName, long newBalanceCents) {
        SQLiteDatabase db = getWritableDatabase();
        ensureAccount(db, accountName);
        long oldBalance = accountBalance(db, accountName);
        ContentValues values = new ContentValues();
        values.put("balance_cents", newBalanceCents);
        db.update("accounts", values, "name=?", new String[]{accountName});

        Transaction transaction = new Transaction();
        transaction.type = "adjustment";
        transaction.amountCents = Math.abs(newBalanceCents - oldBalance);
        transaction.sourceApp = "手动";
        transaction.accountName = accountName;
        transaction.category = "平账";
        transaction.merchant = accountName;
        transaction.note = "平账：" + formatMoneySigned(oldBalance) + " -> " + formatMoneySigned(newBalanceCents);
        transaction.notificationKey = "adjust:" + accountName + ":" + System.currentTimeMillis();
        transaction.occurredAt = System.currentTimeMillis();
        insert(transaction);
        return newBalanceCents - oldBalance;
    }

    long transfer(String fromAccount, String toAccount, long amountCents, String note) {
        if (fromAccount == null || toAccount == null
                || fromAccount.length() == 0 || toAccount.length() == 0
                || "未确认账户".equals(fromAccount) || "未确认账户".equals(toAccount)
                || fromAccount.equals(toAccount) || amountCents <= 0) {
            return -1;
        }
        Transaction transaction = new Transaction();
        transaction.type = "transfer";
        transaction.amountCents = amountCents;
        transaction.sourceApp = "手动";
        transaction.accountName = fromAccount;
        transaction.targetAccountName = toAccount;
        transaction.category = "转账";
        transaction.merchant = toAccount;
        transaction.note = note == null ? "" : note;
        transaction.notificationKey = "transfer:" + fromAccount + ":" + toAccount + ":" + System.currentTimeMillis();
        transaction.occurredAt = System.currentTimeMillis();
        return insert(transaction);
    }

    boolean hasNotificationKey(String notificationKey) {
        if (notificationKey == null || notificationKey.length() == 0) {
            return false;
        }
        try (Cursor cursor = getReadableDatabase().query(
                "transactions", new String[]{"id"}, "notification_key=?",
                new String[]{notificationKey}, null, null, null, "1")) {
            return cursor.moveToFirst();
        }
    }

    List<Transaction> recent(int limit) {
        ArrayList<Transaction> results = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query(
                "transactions", null, null, null, null, null,
                "occurred_at DESC", String.valueOf(limit))) {
            while (cursor.moveToNext()) {
                results.add(fromCursor(cursor));
            }
        }
        return results;
    }

    List<Transaction> search(String query, int limit) {
        String value = query == null ? "" : query.trim();
        if (value.length() == 0) {
            return recent(limit);
        }
        String like = "%" + value + "%";
        ArrayList<Transaction> results = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query(
                "transactions", null,
                "category LIKE ? OR merchant LIKE ? OR note LIKE ? OR account_name LIKE ? OR target_account LIKE ? OR tags LIKE ? OR source_app LIKE ? OR CAST(amount_cents AS TEXT) LIKE ?",
                new String[]{like, like, like, like, like, like, like, like},
                null, null, "occurred_at DESC", String.valueOf(limit))) {
            while (cursor.moveToNext()) {
                results.add(fromCursor(cursor));
            }
        }
        return results;
    }

    List<Account> accounts() {
        ArrayList<Account> results = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query(
                "accounts", null, null, null, null, null, "sort_order ASC, id ASC")) {
            while (cursor.moveToNext()) {
                Account account = new Account();
                account.id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                account.name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                account.kind = cursor.getString(cursor.getColumnIndexOrThrow("kind"));
                account.accountType = cursor.getString(cursor.getColumnIndexOrThrow("account_type"));
                account.currency = cursor.getString(cursor.getColumnIndexOrThrow("currency"));
                account.includeInTotal = cursor.getInt(cursor.getColumnIndexOrThrow("include_in_total")) == 1;
                account.balanceCents = cursor.getLong(cursor.getColumnIndexOrThrow("balance_cents"));
                account.creditLimitCents = cursor.getLong(cursor.getColumnIndexOrThrow("credit_limit_cents"));
                account.billDate = cursor.getInt(cursor.getColumnIndexOrThrow("bill_date"));
                account.dueDate = cursor.getInt(cursor.getColumnIndexOrThrow("due_date"));
                account.sortOrder = cursor.getInt(cursor.getColumnIndexOrThrow("sort_order"));
                results.add(account);
            }
        }
        return results;
    }

    List<String> accountNames() {
        ArrayList<String> names = new ArrayList<>();
        names.add("未确认账户");
        for (Account account : accounts()) {
            names.add(account.name);
        }
        return names;
    }

    List<Category> categories(String type) {
        ArrayList<Category> results = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query(
                "categories", null, "type=? AND is_active=1", new String[]{type},
                null, null, "sort_order ASC, id ASC")) {
            while (cursor.moveToNext()) {
                Category category = new Category();
                category.id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                category.name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                category.type = cursor.getString(cursor.getColumnIndexOrThrow("type"));
                category.parentName = cursor.getString(cursor.getColumnIndexOrThrow("parent_name"));
                category.icon = cursor.getString(cursor.getColumnIndexOrThrow("icon"));
                category.sortOrder = cursor.getInt(cursor.getColumnIndexOrThrow("sort_order"));
                category.active = cursor.getInt(cursor.getColumnIndexOrThrow("is_active")) == 1;
                results.add(category);
            }
        }
        return results;
    }

    List<String> categoryNames(String type) {
        ArrayList<String> names = new ArrayList<>();
        for (Category category : categories(type)) {
            names.add(category.name);
        }
        return names;
    }

    void addCategory(String name, String type) {
        if (name == null || name.trim().length() == 0) {
            return;
        }
        insertCategory(getWritableDatabase(), name.trim(), type, "", name.trim().substring(0, 1), 99);
    }

    long totalByKind(String kind) {
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT COALESCE(SUM(balance_cents), 0) FROM accounts WHERE kind=? AND include_in_total=1",
                new String[]{kind})) {
            if (cursor.moveToFirst()) {
                return cursor.getLong(0);
            }
        }
        return 0;
    }

    long sumSince(String type, long sinceMillis) {
        return sumBetween(type, sinceMillis, Long.MAX_VALUE);
    }

    long sumBetween(String type, long startMillis, long endMillis) {
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT COALESCE(SUM(amount_cents), 0) FROM transactions WHERE type=? AND occurred_at>=? AND occurred_at<?",
                new String[]{type, String.valueOf(startMillis), String.valueOf(endMillis)})) {
            if (cursor.moveToFirst()) {
                return cursor.getLong(0);
            }
        }
        return 0;
    }

    long categoryExpenseBetween(String category, long startMillis, long endMillis) {
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT COALESCE(SUM(amount_cents), 0) FROM transactions WHERE type='expense' AND category=? AND occurred_at>=? AND occurred_at<?",
                new String[]{category, String.valueOf(startMillis), String.valueOf(endMillis)})) {
            if (cursor.moveToFirst()) {
                return cursor.getLong(0);
            }
        }
        return 0;
    }

    List<Budget> budgets() {
        ArrayList<Budget> results = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query(
                "budgets", null, null, null, null, null, "start_at DESC, category ASC")) {
            while (cursor.moveToNext()) {
                Budget budget = new Budget();
                budget.id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                budget.period = cursor.getString(cursor.getColumnIndexOrThrow("period"));
                budget.category = cursor.getString(cursor.getColumnIndexOrThrow("category"));
                budget.amountCents = cursor.getLong(cursor.getColumnIndexOrThrow("amount_cents"));
                budget.startAt = cursor.getLong(cursor.getColumnIndexOrThrow("start_at"));
                budget.endAt = cursor.getLong(cursor.getColumnIndexOrThrow("end_at"));
                budget.alertThreshold = cursor.getInt(cursor.getColumnIndexOrThrow("alert_threshold"));
                results.add(budget);
            }
        }
        return results;
    }

    void saveMonthlyBudget(String category, long amountCents) {
        long[] range = currentMonthRange();
        ContentValues values = new ContentValues();
        values.put("period", "month");
        values.put("category", category == null || category.length() == 0 ? "总预算" : category);
        values.put("amount_cents", amountCents);
        values.put("start_at", range[0]);
        values.put("end_at", range[1]);
        values.put("alert_threshold", 80);
        getWritableDatabase().insertWithOnConflict("budgets", null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    List<AutoRecordLog> autoLogs(int limit) {
        ArrayList<AutoRecordLog> results = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query(
                "auto_record_logs", null, null, null, null, null, "created_at DESC", String.valueOf(limit))) {
            while (cursor.moveToNext()) {
                AutoRecordLog log = new AutoRecordLog();
                log.id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                log.status = cursor.getString(cursor.getColumnIndexOrThrow("status"));
                log.sourceApp = cursor.getString(cursor.getColumnIndexOrThrow("source_app"));
                log.rawText = cursor.getString(cursor.getColumnIndexOrThrow("raw_text"));
                log.message = cursor.getString(cursor.getColumnIndexOrThrow("message"));
                log.amountCents = cursor.getLong(cursor.getColumnIndexOrThrow("amount_cents"));
                log.createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at"));
                results.add(log);
            }
        }
        return results;
    }

    void logAutoRecord(String status, String sourceApp, String rawText, String message, long amountCents) {
        ContentValues values = new ContentValues();
        values.put("status", status);
        values.put("source_app", sourceApp);
        values.put("raw_text", rawText);
        values.put("message", message);
        values.put("amount_cents", amountCents);
        values.put("created_at", System.currentTimeMillis());
        getWritableDatabase().insert("auto_record_logs", null, values);
    }

    private void normalizeTransaction(Transaction transaction) {
        long now = System.currentTimeMillis();
        if (transaction.sourceApp == null) transaction.sourceApp = "";
        if (transaction.accountName == null) transaction.accountName = "";
        if (transaction.targetAccountName == null) transaction.targetAccountName = "";
        if (transaction.category == null) transaction.category = "";
        if (transaction.merchant == null) transaction.merchant = "";
        if (transaction.note == null) transaction.note = "";
        if (transaction.tags == null) transaction.tags = "";
        if (transaction.rawText == null) transaction.rawText = "";
        if (transaction.notificationKey == null || transaction.notificationKey.length() == 0) {
            transaction.notificationKey = "manual:" + now;
        }
        if (transaction.bookId <= 0) transaction.bookId = 1;
        if (transaction.occurredAt <= 0) transaction.occurredAt = now;
        if (transaction.createdAt <= 0) transaction.createdAt = now;
        if (transaction.updatedAt <= 0) transaction.updatedAt = now;
    }

    private ContentValues transactionValues(Transaction transaction) {
        ContentValues values = new ContentValues();
        values.put("type", transaction.type);
        values.put("amount_cents", transaction.amountCents);
        values.put("source_app", transaction.sourceApp);
        values.put("account_name", transaction.accountName);
        values.put("target_account", transaction.targetAccountName);
        values.put("category", transaction.category);
        values.put("merchant", transaction.merchant);
        values.put("note", transaction.note);
        values.put("tags", transaction.tags);
        values.put("raw_text", transaction.rawText);
        values.put("notification_key", transaction.notificationKey);
        values.put("book_id", transaction.bookId);
        values.put("is_reimbursement", transaction.reimbursement ? 1 : 0);
        values.put("refund_original_id", transaction.refundOriginalId);
        values.put("occurred_at", transaction.occurredAt);
        values.put("created_at", transaction.createdAt);
        values.put("updated_at", transaction.updatedAt);
        return values;
    }

    private void applyTransactionToAccount(SQLiteDatabase db, Transaction transaction) {
        if (transaction.accountName == null
                || transaction.accountName.length() == 0
                || "未确认账户".equals(transaction.accountName)
                || "adjustment".equals(transaction.type)) {
            return;
        }
        ensureAccount(db, transaction.accountName);
        if ("transfer".equals(transaction.type)) {
            String toAccount = transaction.targetAccountName.length() > 0 ? transaction.targetAccountName : transaction.merchant;
            if (toAccount != null && toAccount.length() > 0 && !transaction.accountName.equals(toAccount)) {
                ensureAccount(db, toAccount);
                db.execSQL("UPDATE accounts SET balance_cents=balance_cents-? WHERE name=?",
                        new Object[]{transaction.amountCents, transaction.accountName});
                db.execSQL("UPDATE accounts SET balance_cents=balance_cents+? WHERE name=?",
                        new Object[]{transaction.amountCents, toAccount});
            }
            return;
        }
        long delta = 0;
        if ("income".equals(transaction.type)) {
            delta = transaction.amountCents;
        } else if ("expense".equals(transaction.type)) {
            delta = -transaction.amountCents;
        } else if ("refund".equals(transaction.type)) {
            delta = transaction.amountCents;
        }
        if (delta != 0) {
            db.execSQL("UPDATE accounts SET balance_cents=balance_cents+? WHERE name=?",
                    new Object[]{delta, transaction.accountName});
        }
    }

    private void ensureAccount(SQLiteDatabase db, String accountName) {
        if (accountName == null || accountName.length() == 0 || "未确认账户".equals(accountName)) {
            return;
        }
        ContentValues values = new ContentValues();
        values.put("name", accountName);
        values.put("kind", "asset");
        values.put("account_type", "asset");
        values.put("currency", "CNY");
        values.put("include_in_total", 1);
        values.put("balance_cents", 0);
        values.put("sort_order", 99);
        values.put("created_at", System.currentTimeMillis());
        db.insertWithOnConflict("accounts", null, values, SQLiteDatabase.CONFLICT_IGNORE);
    }

    private long accountBalance(SQLiteDatabase db, String accountName) {
        try (Cursor cursor = db.query(
                "accounts", new String[]{"balance_cents"}, "name=?",
                new String[]{accountName}, null, null, null, "1")) {
            if (cursor.moveToFirst()) {
                return cursor.getLong(0);
            }
        }
        return 0;
    }

    private static Transaction fromCursor(Cursor cursor) {
        Transaction transaction = new Transaction();
        transaction.id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
        transaction.type = cursor.getString(cursor.getColumnIndexOrThrow("type"));
        transaction.amountCents = cursor.getLong(cursor.getColumnIndexOrThrow("amount_cents"));
        transaction.sourceApp = cursor.getString(cursor.getColumnIndexOrThrow("source_app"));
        transaction.accountName = cursor.getString(cursor.getColumnIndexOrThrow("account_name"));
        transaction.targetAccountName = cursor.getString(cursor.getColumnIndexOrThrow("target_account"));
        transaction.category = cursor.getString(cursor.getColumnIndexOrThrow("category"));
        transaction.merchant = cursor.getString(cursor.getColumnIndexOrThrow("merchant"));
        transaction.note = cursor.getString(cursor.getColumnIndexOrThrow("note"));
        transaction.tags = cursor.getString(cursor.getColumnIndexOrThrow("tags"));
        transaction.rawText = cursor.getString(cursor.getColumnIndexOrThrow("raw_text"));
        transaction.notificationKey = cursor.getString(cursor.getColumnIndexOrThrow("notification_key"));
        transaction.bookId = cursor.getLong(cursor.getColumnIndexOrThrow("book_id"));
        transaction.reimbursement = cursor.getInt(cursor.getColumnIndexOrThrow("is_reimbursement")) == 1;
        transaction.refundOriginalId = cursor.getLong(cursor.getColumnIndexOrThrow("refund_original_id"));
        transaction.occurredAt = cursor.getLong(cursor.getColumnIndexOrThrow("occurred_at"));
        transaction.createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at"));
        transaction.updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow("updated_at"));
        return transaction;
    }

    static long[] currentMonthRange() {
        Calendar start = Calendar.getInstance();
        start.set(Calendar.DAY_OF_MONTH, 1);
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);
        Calendar end = (Calendar) start.clone();
        end.add(Calendar.MONTH, 1);
        return new long[]{start.getTimeInMillis(), end.getTimeInMillis()};
    }

    static String formatDateTime(long millis) {
        return new SimpleDateFormat("MM-dd HH:mm", Locale.CHINA).format(new Date(millis));
    }

    static String formatDate(long millis) {
        return new SimpleDateFormat("yyyy.MM.dd E", Locale.CHINA).format(new Date(millis));
    }

    static String formatMoneySigned(long cents) {
        String sign = cents < 0 ? "-" : "";
        return sign + PaymentParser.formatMoney(Math.abs(cents));
    }
}
