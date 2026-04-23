package auction.model.item;

import auction.model.base.Item;

public class Art extends Item {
    private String artist;
    private int yearCreated;

    public Art() { super(); }

    public Art(String name, String description, double startingPrice, String sellerId, String artist, int yearCreated) {
        super(name, description, startingPrice, sellerId);
        this.artist = artist;
        this.yearCreated = yearCreated;
    }

    @Override
    public String getCategory() { return "Art"; }//Nhãn dán phân loại

    @Override
    public String getDisplayInfo() {
        return String.format("[Art] %s by %s (%d) — $%.0f", getName(), artist, yearCreated, getStartingPrice());
    }

    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }
    public int getYearCreated() { return yearCreated; }
    public void setYearCreated(int yearCreated) { this.yearCreated = yearCreated; }
}
