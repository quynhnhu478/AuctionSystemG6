package auction.model.base;

import java.io.Serializable;
import java.util.UUID;
public abstract class Entity implements Serializable { // Khai báo lớp trừu tượng Entity, có thể tuần tự hóa để lưu trữ hoặc truyền tải

    protected String id; // Biến định danh duy nhất, không thể thay đổi sau khi gán
    protected String name;

    protected Entity() {
        this.id = UUID.randomUUID().toString();
        // Tự động tạo một chuỗi ID ngẫu nhiên không trùng lặp
    }

    protected Entity(String id,String name) {
        this.id = id; // Gán ID cụ thể nếu được truyền vào
        this.name=name;
    }

    public String getId() { return id; } // Trả về ID của thực thể

    public abstract String getDisplayInfo(); //buộc các lớp con phải định nghĩa cách hiển thị thông tin

    @Override
    public boolean equals(Object o) { // So sánh hai đối tượng dựa trên ID thay vì địa chỉ ô nhớ
        if (this == o) return true;
        if (!(o instanceof Entity other)) return false;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() { return id.hashCode(); } // Tạo mã băm dựa trên ID để dùng trong các cấu trúc dữ liệu như HashMap
}