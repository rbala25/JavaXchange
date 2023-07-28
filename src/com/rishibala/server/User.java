package com.rishibala.server;

public class User {
    private final int botId;
    private int stockAmt;

    User(int botId, int stockAmt) {
        this.botId = botId;
        this.stockAmt = stockAmt;
    }

    void updateStockAmt(int amount) {
        stockAmt += amount;
    }

    public int getStockAmt() {
        return stockAmt;
    }
}
