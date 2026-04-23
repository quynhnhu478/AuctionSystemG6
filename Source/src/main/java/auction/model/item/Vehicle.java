package auction.model.item;

import auction.model.base.Item;

public class Vehicle extends Item {
    private String vehicleType; // Car, Bicycle, Motorbike, etc.
    private int year;

    public Vehicle() { super(); }

    public Vehicle(String name, String description, double startingPrice,String sellerId, String vehicleType, int year) {
        super(name, description, startingPrice, sellerId);
        this.vehicleType = vehicleType;
        this.year = year;
    }

    @Override
    public String getCategory() { return "Vehicle"; }//phân loại

    @Override
    public String getDisplayInfo() {
        return String.format("[Vehicle] %s (%d) — $%.0f", getName(), year, getStartingPrice());
    }

    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }
    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }
}
