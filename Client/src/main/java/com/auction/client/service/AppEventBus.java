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

    // MainController dùng hàm này để phát tín hiệu đi toàn app
    public static void emit(String eventType, Object data) {
        if (listeners.containsKey(eventType)) {
            for (Consumer<Object> action : listeners.get(eventType)) {
                action.accept(data);
            }
        }
    }

    /*ĐĂNG KÝ LẮNG NGHE SỰ KIỆN (Dùng ở các màn hình Popup, Thẻ sản phẩm...)
     Khi sự kiện có tên tương ứng được kích hoạt, hàm callback sẽ được thực thi.
     */
    public static void on(String eventName, Consumer<Object> callback) {
        if (callback == null) return;

        // Nếu tên sự kiện chưa tồn tại trong bản đồ, tạo mới một danh sách rỗng
        listeners.computeIfAbsent(eventName, k -> new ArrayList<>());

        // Thêm hàm lắng nghe này vào danh sách phát sóng của sự kiện
        listeners.get(eventName).add(callback);
    }
    /*
     HỦY ĐĂNG KÝ (Tùy chọn)
     Hàm dùng để giải phóng bộ nhớ khi một Popup hoặc màn hình bị đóng lại hoàn toàn.
     */
    public static void off(String eventName, Consumer<Object> callback) {
        if (listeners.containsKey(eventName)) {
            listeners.get(eventName).remove(callback);
        }
    }
}
