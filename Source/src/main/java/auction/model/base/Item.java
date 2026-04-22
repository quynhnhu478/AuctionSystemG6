package auction.model.base;
import auction.enums.ItemStatus;
import auction.model.user.Seller;

public abstract class Item extends Entity {
    protected String description;
    protected double startingPrice; //giá khởi điểm, không đổi
    protected double currentPrice; //giá cao nhất hiện tại
    protected ItemStatus status;
    protected Seller seller; //người đăng sản phẩm

    public Item(String id, String name, String description, double startingPrice, Seller seller) {
        super(id,name);
        this.description = description;
        this.startingPrice = startingPrice;
        this.currentPrice = startingPrice; //ban đầu bằng giá khởi điểm
        this.status = ItemStatus.PENDING;
        this.seller = seller;
    }

    //buộc lớp con in thông tin theo cách riêng (Polymorphism)
    public abstract void printInfo();

    //buộc lớp con trả về danh mục
    public abstract String getCategory();

    //kiểm tra dữ liệu hợp lệ - lớp con có thể override để thêm kiểm tra riêng
    public void validate() {
        if (name == null || name.isBlank()) { //check chuỗi rỗng, có khoảng trắng
            throw new IllegalArgumentException("Tên sản phẩm không được để trống.");
        }
        if (startingPrice <= 0) {
            throw new IllegalArgumentException("Giá khởi điểm phải lớn hơn 0.");
        }
    }

    //getter
    public String getDescription() {
        return description;
    }
    public double getStartingPrice() {
        return startingPrice;
    }
    public double getCurrentPrice() {
        return currentPrice;
    }
    public ItemStatus getStatus() {
        return status;
    }
    public Seller getSeller() {
        return seller;
    }

    //seller
    public void setName(String name) {
        this.name = name;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public void setStartingPrice(double price) {
        this.startingPrice = price;
    }

    public void setCurrentPrice(double price) {
        this.currentPrice = price;
    }
    public void setStatus(ItemStatus status) {
        this.status = status;
    }
}