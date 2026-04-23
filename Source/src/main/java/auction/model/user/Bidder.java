package auction.model.user;

import auction.enums.UserRole;
import auction.model.base.User;

public class Bidder extends User {

    public Bidder() { super(); }

    public Bidder(String name, String email) {
        super(name, email, 10000.0, UserRole.BIDDER);
    }

    public Bidder(String id, String name, String email, double balance) {
        super(id, name, email, balance, UserRole.BIDDER);
    }

    @Override public boolean canBid()   { return true; }
    @Override public boolean canSell()  { return true; }
    @Override public boolean canAdmin() { return false; }
}
