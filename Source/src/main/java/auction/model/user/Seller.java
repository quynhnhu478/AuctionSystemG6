package auction.model.user;

import auction.enums.UserRole;
import auction.model.base.User;

public class Seller extends User {

    public Seller() { super(); }

    public Seller(String name, String email) {
        super(name, email, 10000.0, UserRole.SELLER);
    }

    public Seller(String id, String name, String email, double balance) {
        super(id, name, email, balance, UserRole.SELLER);
    }

    @Override public boolean canBid()   { return true; }
    @Override public boolean canSell()  { return true; }
    @Override public boolean canAdmin() { return false; }
}
