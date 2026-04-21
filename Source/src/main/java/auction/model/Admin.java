package auction.model;

import auction.enums.UserRole;
import auction.model.User;

class Admin extends User {

    public Admin(String id, String name, String userId, String userName, String password, String email) {
        super(id, name, userName, password, email);
    }

    @Override
    public UserRole getRole() {
        return UserRole.ADMIN;
    }
}
