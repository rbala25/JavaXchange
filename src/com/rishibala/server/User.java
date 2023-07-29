package com.rishibala.server;

public class User {
    private final int botId;
    private int stockAmt;
    private double profit;

    User(int botId, int stockAmt) {
        this.botId = botId;
        this.stockAmt = stockAmt;
        this.profit = 0;
    }

    public User() {
        this(-1, 0);
    }

    void updateStockAmt(int amount) {
        stockAmt += amount;
    }

    public int getStockAmt() {
        return stockAmt;
    }

    public int getBotId() {
        return botId;
    }

    public double getProfit() {
        return profit;
    }

    void updateProfit(double amount) {
        profit += amount;
    }

    @Override
    public String toString() {
        return botId + "-" + stockAmt;
    }

    public static User unString(String str) {
        String[] args = str.split("-");
        return new User(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
    }
}
