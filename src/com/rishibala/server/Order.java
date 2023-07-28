package com.rishibala.server;

import java.net.Socket;

record Order(int botId, Type type, double price, int quantity, int orderId) {
    enum Type {BUY, SELL};
    private static int placeholder = 1;

    Order() {
        this(-1, null, 0, 0, placeholder++);
    }

    Order(int botId, Type type, double price, int quantity) {
        this(botId, type, price, quantity, placeholder++);
    }

    @Override
    public String toString() {
        return botId + ", " + type + ", " + price + ", " +  quantity + ", " + orderId;
    }

    static Order toOrder(String str) {
        String[] args = str.split(",");

        for(int i=0; i<args.length; i++) {
            args[i] = args[i].trim();
        }

        Type type = (args[1].equals("BUY")) ? Type.BUY : Type.SELL;

        return new Order(Integer.parseInt(args[0]), type, Double.parseDouble(args[2]),
                Integer.parseInt(args[3]));
    }

    static Order unserialize(String str, Socket socket) {
        String updated = StockExchange.getId(socket) + ", " + str;

        return toOrder(updated);
    }

    @Override
    public boolean equals(Object obj) {
        Order order = (Order) obj;
        return (order.orderId == this.orderId);
    }
}
