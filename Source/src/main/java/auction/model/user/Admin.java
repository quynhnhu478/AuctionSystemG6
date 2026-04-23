package auction.model.user;


import auction.enums.UserRole;
import auction.model.base.User;

public class Admin extends User {

    public Admin() { super(); }

    public Admin(String name, String email) {
        super(name, email, 0.0, UserRole.ADMIN);
    }

    public Admin(String id, String name, String email) {
        super(id, name, email, 0.0, UserRole.ADMIN);
    }

    @Override public boolean canBid()   { return false; }
    @Override public boolean canSell()  { return false; }
    @Override public boolean canAdmin() { return true; }
}
