package auction.model.base;

import java.io.Serializable;

public abstract class Entity implements Serializable {//các đối tượng thuộc lớp này có thể được chuyển đổi thành luồng byte

    private static int nextId = 1;//biến static để nhớ số thứ tự hiện tại của toàn bộ hệ thống

    private final String id; // ID của riêng từng đối tượng (dùng kiểu String để giữ cấu trúc cũ của bạn)

    protected Entity() //tạo mới một đối tượng trong lúc chương trình đang chạy
    {
        this.id = String.valueOf(nextId++); //mỗi khi 'new', lấy số hiện tại làm ID rồi tăng nextId lên 1
    }

    protected Entity(String id)
    {
        this.id = id;
        /*đọc dữ liệu từ file:trong file ghi "ID: 10", bạn truyền số 10 vào đây để đối tượng giữ nguyên ID cũ,
        không bị cấp số mới*/

    }

    public String getId() { return id; }

    public abstract String getDisplayInfo();// in ra ID như: "ID: 1", "ID: 2"


    @Override
    public boolean equals(Object o) {
        if (this == o) return true; // Nếu cùng địa chỉ nhớ -> là 1
        if (!(o instanceof Entity other)) return false; // Nếu không phải Entity -> không so sánh
        return id.equals(other.id); // Hai Entity bằng nhau NẾU chúng có cùng ID mặc dù ở 2 ô nhớ khác nhau
    }

    @Override
    public int hashCode() {
        return id.hashCode();// Trả về mã băm dựa trên ID

    }
}
