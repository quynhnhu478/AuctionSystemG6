package auction.model;

public abstract class Item extends Entity{
    protected String itemId, name, description;
    protected double startingPrice, currentPrice;
    protected Seller seller;
    public Item(String id, String name) {
        super(id, name);
    }
}
