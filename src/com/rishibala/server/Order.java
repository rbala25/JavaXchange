package com.rishibala.server;

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

        Type type = (args[1].equalsIgnoreCase("BUY")) ? Type.BUY : Type.SELL;

        if(args.length == 5) {
            return new Order(Integer.parseInt(args[0]), type, Double.parseDouble(args[2]),
                    Integer.parseInt(args[3]), Integer.parseInt(args[4]));
        }

        return new Order(Integer.parseInt(args[0]), type, Double.parseDouble(args[2]),
                Integer.parseInt(args[3]));
    }

//    static Order unserialize(String str, Socket socket) {
//        String updated = StockExchange.getId(socket) + ", " + str;
//
//        return toOrder(updated);
//    }

    @Override
    public boolean equals(Object obj) {
        Order order = (Order) obj;
        return (order.orderId == this.orderId);
    }

    public String cleanFormat() {
        return String.format("Type: %s, Price: %f, Quantity: %o, OrderID: %o", type, price, quantity, orderId);
    }
}
