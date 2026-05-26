package com.auction.client.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class AppEventBus {
    // Lưu danh sách các hàm cần chạy khi có sự kiện (Event) tương ứng kích hoạt
    private static final Map<String, List<Consumer<Object>>> listeners = new HashMap<>();

    // Các màn hình con dùng hàm này để đăng ký lắng nghe sự kiện
    public static void subscribe(String eventType, Consumer<Object> action) {
        listeners.computeIfAbsent(eventType, k -> new ArrayList<>()).add(action);
    }
    // hàm để ở controller nới nhân tín hiệu
    public static void on(String eventType, Consumer<Object> callback) {
        listeners.computeIfAbsent(eventType, k -> new ArrayList<>()).add(callback);
    }

    // MainController dùng hàm này để phát tín hiệu đi toàn app
    public static void emit(String eventType, Object data) {
        if (listeners.containsKey(eventType)) {
            for (Consumer<Object> action : listeners.get(eventType)) {
                action.accept(data);
            }
        }
    }
}