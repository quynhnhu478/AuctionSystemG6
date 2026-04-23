package auction.model.base;
import auction.enums.CategoryType;
import auction.enums.ItemStatus;
import auction.model.user.Seller;

import java.time.LocalDateTime;
import java.util.*;

public abstract class Item extends Entity {
    protected String description;// mo ta san pham

    protected double startingPrice; //giá khởi điểm, không đổi

    protected ItemStatus status;

    protected UUID sellerID;//id người đăng sản phẩm

    protected CategoryType category; // nhan phan loai

    protected Map<String, Object> specifications;// danh sach thong tin san pham nguoi ban can nhap, cac thuoc tinh dong (RAM, Engine, Artist...)

    protected List<String> images;// danh sach URL san pham

    public Item(String id, String name, String description, double startingPrice, UUID sellerID, CategoryType category) {
        super(id,name);
        this.description = description;
        this.startingPrice = startingPrice;
        this.status = ItemStatus.PENDING;
        this.sellerID = sellerID;
        this.category = category;
        this.specifications = new HashMap<>();
        this.images = new ArrayList<>();
    }

    //kiểm tra dữ liệu hợp lệ - lớp con có thể override để thêm kiểm tra riêng
    public void validate() {
        if (name == null || name.isBlank()) { //check chuỗi rỗng, có khoảng trắng
            throw new IllegalArgumentException("Tên sản phẩm không được để trống.");
        }
        if (startingPrice <= 0) {
            throw new IllegalArgumentException("Giá khởi điểm phải lớn hơn 0.");
        }
    }
    public void addAttribute(String key, Object value) {
        specifications.put(key,value);
    }
    public void addImages(String url){
        images.add(url);
    }

    //setter
    public void setName(String name) {
        this.name = name;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public void setStartingPrice(double price) {
        this.startingPrice = price;
    }
    public void setStatus(ItemStatus status) {
        this.status = status;
    }

    //getter
    public String getDescription() {
        return description;
    }
    public double getStartingPrice() {
        return startingPrice;
    }
    public ItemStatus getStatus() {
        return status;
    }
    public UUID getSeller() {
        return sellerID;
    }

    //buộc lớp con in thông tin theo cách riêng (Polymorphism)
    public abstract void printInfo();

    //buộc lớp con trả về danh mục
    public abstract String getCategory();
}