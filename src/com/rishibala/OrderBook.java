package com.rishibala;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class OrderBook {
    private final SortedMap<Double, List<Order>> buyOrders;
    private final SortedMap<Double, List<Order>> sellOrders;

    public OrderBook() {
        buyOrders = new TreeMap<>();
        sellOrders = new TreeMap<>();
    }

    public void addOrder(Order order) {
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
    }

    boolean removeOrder(Order order) {
        if(order.type().equals(Order.Type.BUY)) {
            List<Order> vals = buyOrders.get(order.price());

           boolean removed = vals.remove(order);
           buyOrders.put(order.price(), vals);
           return removed;

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

            boolean removed = vals.remove(order);
            sellOrders.put(order.price(), vals);
            return removed;
        }
    }

    boolean removeOrder(int orderId) {
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
            return removeOrder(order);
        } else {
            return false;
        }
    }

    public SortedMap<Double, List<Order>> getBuyOrders() {
        SortedMap<Double, List<Order>> copy = new TreeMap<>(buyOrders);
        return copy;
    }

    public SortedMap<Double, List<Order>> getSellOrders() {
        SortedMap<Double, List<Order>> copy = new TreeMap<>(sellOrders);
        return copy;
    }
}
