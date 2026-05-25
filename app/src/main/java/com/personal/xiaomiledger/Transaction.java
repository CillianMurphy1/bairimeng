package com.personal.xiaomiledger;

final class Transaction {
    long id;
    String type;
    long amountCents;
    String sourceApp;
    String accountName;
    String targetAccountName;
    String category;
    String merchant;
    String note;
    String tags;
    String rawText;
    String notificationKey;
    long bookId;
    boolean reimbursement;
    long refundOriginalId;
    long occurredAt;
    long createdAt;
    long updatedAt;
}
