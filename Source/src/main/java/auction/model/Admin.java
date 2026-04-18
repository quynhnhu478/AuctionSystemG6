package auction.model;

import auction.enums.UserRole;

class Admin extends User {

    public Admin(String id, String name, String userId, String userName, String password, String email) {
        super(id, name, userId, userName, password, email);
    }

    @Override
    public UserRole getRole() {
        return UserRole.ADMIN;
    }
}
