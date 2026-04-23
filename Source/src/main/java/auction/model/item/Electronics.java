package auction.model.item;

import auction.model.base.Item;

public class Electronics extends Item {
    private String brand;
    private String condition; // New, Used, Refurbished

    public Electronics() { super(); }

    public Electronics(String name, String description, double startingPrice, String sellerId, String brand, String condition) {
        super(name, description, startingPrice, sellerId);
        this.brand = brand;
        this.condition = condition;
    }

    @Override
    public String getCategory() { return "Electronics"; }

    @Override
    public String getDisplayInfo() {
        return String.format("[Electronics] %s (%s) — $%.0f", getName(), condition, getStartingPrice());
    }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }
}
