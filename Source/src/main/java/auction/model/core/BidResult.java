package auction.model.core;

public class BidResult {
    private final boolean success;
    private final String  message;
    private final BidTransaction transaction;

    private BidResult(boolean success, String message, BidTransaction tx) {
        this.success = success;
        this.message = message;
        this.transaction = tx;
    }

    public static BidResult success(BidTransaction tx) {
        return new BidResult(true, "Bid placed successfully!", tx);
    }

    public static BidResult failure(String reason) {
        return new BidResult(false, reason, null);
    }

    public boolean isSuccess() { return success; }
    public String  getMessage() { return message; }
    public BidTransaction getTransaction()  { return transaction; }
}
