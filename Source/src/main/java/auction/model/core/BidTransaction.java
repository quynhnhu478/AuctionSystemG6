package auction.model.core;

import auction.model.base.Entity;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BidTransaction extends Entity {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("h:mm:ss a");

    private final String auctionId;
    private final String bidderId;
    private final String bidderName;
    private final double amount;
    private final LocalDateTime timestamp;

    public BidTransaction(String auctionId, String bidderId, String bidderName, double amount) {
        super();
        this.auctionId  = auctionId;
        this.bidderId   = bidderId;
        this.bidderName = bidderName;
        this.amount     = amount;
        this.timestamp  = LocalDateTime.now();
    }

    @Override
    public String getDisplayInfo() {
        return String.format("%s bid $%.0f at %s", bidderName, amount, timestamp.format(FMT));
    }

    public String getAuctionId()   { return auctionId; }
    public String getBidderId()    { return bidderId; }
    public String getBidderName()  { return bidderName; }
    public double getAmount()      { return amount; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getFormattedTime() { return timestamp.format(FMT); }
}
