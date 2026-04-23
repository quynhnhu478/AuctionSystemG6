package auction.model.base;

public abstract class Item extends Entity {

    private String name;
    private String description;
    //private String imageUrl;//lưu trữ đường dẫn đến link ảnh của món hàng
    private double startingPrice; //giá khởi điểm
    private String sellerId;

    protected Item() { super(); }

    protected Item(String name, String description, double startingPrice, String sellerId) {
        super();
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.sellerId = sellerId;
    }
    //polymorphism -> Item ko biết mình thuộc loại nào -> để trống, bắt các lớp con phải tự trả lời
    public abstract String getCategory();

    @Override
    public String getDisplayInfo() {
        return String.format("[%s] %s — $%.0f", getCategory(), name, startingPrice);
    }

    // Getters & Setters (Encapsulation)
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    //public String getImageUrl() { return imageUrl; }
    //public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public double getStartingPrice() { return startingPrice; }
    public void setStartingPrice(double startingPrice) { this.startingPrice = startingPrice; }

    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }
}
