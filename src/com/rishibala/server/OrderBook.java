package com.rishibala.server;

import java.util.*;

public class OrderBook {
    private final SortedMap<Double, List<Order>> buyOrders;
    private final SortedMap<Double, List<Order>> sellOrders;

    public OrderBook() {
        buyOrders = new TreeMap<>(Comparator.reverseOrder());
        sellOrders = new TreeMap<>(Comparator.reverseOrder());
    }

    void addOrder(Order order) {
        synchronized (buyOrders) {
            synchronized (sellOrders) {
                if(order.botId() == -1) {
                    return;
                }

                SortedMap<Double, List<Order>> ordersMap;
                if (order.type().equals(Order.Type.BUY)) {
                    ordersMap = buyOrders;
                } else if (order.type().equals(Order.Type.SELL)) {
                    ordersMap = sellOrders;
                } else {
                    throw new IllegalArgumentException("Invalid order type: " + order.type());
                }

                List<Order> vals = ordersMap.get(order.pricePerQuantity());
                if(vals != null) {
                    ArrayList<Order> replacement = new ArrayList<>(vals);
                    try {
                        replacement.add(order);
                    } catch (UnsupportedOperationException e) {
                        System.out.println("VALS: " + vals);
                        System.out.println("REPLACEMENT: " + replacement);
                        System.out.println("ERROR: " + order);
                    }
                    ordersMap.put(order.pricePerQuantity(), replacement);
                } else {;
                    ordersMap.put(order.pricePerQuantity(), List.of(order));
                }

                if(order.botId() != 0) {
                    System.out.println("New Order: " + order);
                }
            }
        }
    }

    private boolean removeOrder(Order order) {
        if(order.type().equals(Order.Type.BUY)) {
            List<Order> vals = buyOrders.get(order.pricePerQuantity());
            int index = -1;
            for(int i=0; i<vals.size(); i++) {
                if(vals.get(i).equals(order)) {
                    index = i;
                    break;
                }
            }

            List<Order> copy = new ArrayList<>(vals);
            copy.remove(index);

            if(vals.size() > 1) {
                buyOrders.put(order.pricePerQuantity(), copy);
            } else {
                buyOrders.remove(order.pricePerQuantity());
            }
            return true;

//            if(vals != null) {
//                for(int i=0; i<vals.size(); i++) {
//                    Order val = vals.get(i);
//                    if(val.equals(order)) {
//                        vals.remove(i);
//                    }
//                }
//            }
        } else {
            List<Order> vals = sellOrders.get(order.pricePerQuantity());
            int index = -1;
            for(int i=0; i<vals.size(); i++) {
                if(vals.get(i).equals(order)) {
                    index = i;
                    break;
                }
            }

            List<Order> copy = new ArrayList<>(vals);
            copy.remove(index);

            if(vals.size() > 1) {
                sellOrders.put(order.pricePerQuantity(), copy);
            } else {
                sellOrders.remove(order.pricePerQuantity());
            }
            return true;
        }
    }

    boolean removeOrder(int orderId) {
        synchronized (buyOrders) {
            synchronized (sellOrders) {
                Order order = new Order();
                boolean check = false;
                for(List<Order> entries : buyOrders.values()) {
                    for(Order entry : entries) {
                        if(entry.orderId() == orderId) {
                            check = true;
                            order = entry;
                        }
                    }
                }

                if(!check) {
                    for(List<Order> entries : sellOrders.values()) {
                        for(Order entry : entries) {
                            if(entry.orderId() == orderId) {
                                check = true;
                                order = entry;
                            }
                        }
                    }
                }

                if(check) {
                    removeOrder(order);
                    return true;
                }
                return false;
            }
        }
    }

    public SortedMap<Double, List<Order>> getBuyOrders() {
        return new TreeMap<>(buyOrders);
    }

    public SortedMap<Double, List<Order>> getSellOrders() {
        return new TreeMap<>(sellOrders);
    }

    List<Set<Order>> matchOrders() {
        List<Set<Order>> matches = new ArrayList<>();
        for(List<Order> list : sellOrders.values()) {
            for(Order order : list) {
                var buys = buyOrders.values();
                boolean matchGot = false;

                for(List<Order> buys1 : buys) {
                    for(Order buy : buys1) {
                        if(buy.pricePerQuantity() >= order.pricePerQuantity()) {
                            if(order.quantity() >= buy.quantity()) {
                                if(!matchGot) {
                                    if(buy.botId() != order.botId()) {
                                        matches.add(new HashSet<>(Set.of(order, buy)));
                                        matchGot = true;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return matches;
    }

    public static StringBuilder getListedMatches(int botId, OrderBook book, boolean checker) {

        StringBuilder builder = new StringBuilder();
        for(List<Order> orders : book.getBuyOrders().values()) {
            for(Order order : orders) {
                if(order.botId() == botId) {
                    if(checker) {
                        builder.append(order).append("\n");
                    } else {
                        builder.append(order.cleanFormat()).append("\n");
                    }
                }
            }
        }

        for(List<Order> orders : book.getSellOrders().values()) {
            for(Order order : orders) {
                if(order.botId() == botId) {
                    if(checker) {
                        builder.append(order).append("\n");
                    } else {
                        builder.append(order.cleanFormat()).append("\n");
                    }
                }
            }
        }
        return builder;
    }

    public static boolean haveOrder(int botId, OrderBook book, int orderId) {
        String list = getListedMatches(botId, book, true).toString();
        String[] matches = list.split("\n");
        List<Order> orders = new ArrayList<>();
        for(String match : matches) {
            orders.add(Order.toOrder(match));
//            System.out.println("OrderId: " + Order.toOrder(match).orderId());
        }

        for(Order order : orders) {
            if(order.orderId() == orderId) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        synchronized (this) {
            return "buyOrders=" + buyOrders +
                    ", sellOrders=" + sellOrders;
        }
    }

    public StringBuilder serialize() {
        synchronized (buyOrders) {
            StringBuilder builder = new StringBuilder();

            for(double key : buyOrders.keySet()) {
                builder.append(key).append(":");
                for(Order order : buyOrders.get(key)) {
                    builder.append(order).append("-");
                }
                builder.replace(builder.length() - 1, builder.length(), "`");
            }

            builder.append("~");

            for(double key : sellOrders.keySet()) {
                builder.append(key).append(":");
                for(Order order : sellOrders.get(key)) {
                    builder.append(order).append("-");
                }
                builder.replace(builder.length() - 1, builder.length(), "`");
            }

            return builder;
        }
    }

    public static OrderBook unserialize(String str) {
        StringBuilder builder = new StringBuilder(str);

        if(builder.equals("~")) {
            return new OrderBook();
        }

        OrderBook book = new OrderBook();
        String[] buySell = builder.toString().split("~");

//        if(buySell.length == 2) {
        String[] buyLines = buySell[0].split("`");
        String[] sellLines = null;

        if(buySell.length > 1) {
            sellLines = buySell[1].split("`");
        }

        for(String line : buyLines) {
            String[] parts = line.split(":");
            Order order = Order.toOrder(parts[1]);

            book.addOrder(order);
        }

        if(sellLines == null) {
            return new OrderBook();
        }

        for(String line : sellLines) {
            String[] parts = line.split(":");
            Order order = Order.toOrder(parts[1]);

            book.addOrder(order);
        }

        return book;
//        } else {
//            String[] parts = buySell[0].split(":");
//            Order order = Order.toOrder(parts[1]);
//
//            book.addOrder(order);
//
//            return book;
//        }
    }
}
