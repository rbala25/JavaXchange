package com.rishibala.server;

import java.util.*;

public class OrderBook {
    private final SortedMap<Double, List<Order>> buyOrders;
    private final SortedMap<Double, List<Order>> sellOrders;

    OrderBook() {
        buyOrders = new TreeMap<>();
        sellOrders = new TreeMap<>();
    }

    void addOrder(Order order) {
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

        List<Order> vals = ordersMap.get(order.price());
        if(vals != null) {
            vals.add(order);
            ordersMap.put(order.price(), vals);
        } else {
            ordersMap.put(order.price(), List.of(order));
        }

        System.out.println("New Order: " + order);
    }

    private void removeOrder(Order order) {
        if(order.type().equals(Order.Type.BUY)) {
            List<Order> vals = buyOrders.get(order.price());
            int index = -1;
            for(int i=0; i<vals.size(); i++) {
                if(vals.get(i).equals(order)) {
                    index = i;
                    break;
                }
            }

            List<Order> copy = new ArrayList<>(vals);
            copy.remove(index);
            buyOrders.put(order.price(), copy);

//            if(vals != null) {
//                for(int i=0; i<vals.size(); i++) {
//                    Order val = vals.get(i);
//                    if(val.equals(order)) {
//                        vals.remove(i);
//                    }
//                }
//            }
        } else {
            List<Order> vals = sellOrders.get(order.price());

            sellOrders.put(order.price(), vals);
        }
    }

    void removeOrder(int orderId) {
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
                        if(buy.price() >= order.price()) {
                            if(!matchGot) {
                                matches.add(new HashSet<>(Set.of(order, buy)));
                                matchGot = true;
                            }
                        }
                    }
                }
            }
        }

        System.out.println("Matches?: " + matches);
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
        return "buyOrders=" + buyOrders +
                ", sellOrders=" + sellOrders;
    }
}
