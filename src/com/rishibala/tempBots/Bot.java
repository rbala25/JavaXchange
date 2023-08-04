package com.rishibala.tempBots;

import com.rishibala.server.Order;
import com.rishibala.server.OrderBook;

import java.util.ArrayList;
import java.util.List;

public abstract class Bot {

    protected final List<Double> means = new ArrayList<>();
    protected Order lastBuy;
    protected Order lastSell;
    protected boolean firstCheck = true;
    protected OrderBook last = new OrderBook();
    protected int shares = 0;
    protected double pnl = 0;
    protected boolean over = false;

    protected abstract void checkEnd(int botId);
    protected abstract void getUser(int botId);
    protected abstract OrderBook getBook(int botId);
    protected abstract void calculate();

}
