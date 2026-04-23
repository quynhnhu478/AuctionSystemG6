package auction.model.base;
import auction.enums.UserRole;
import auction.exception.AuthException;

public abstract class User extends Entity {
    protected String userName;
    protected String password;
    protected String email;

    public User(String id, String userName, String name,String password, String email) {
        super(id, name);
        this.userName = userName;
        this.password = password;
        this.email = email;
    }
    //đăng nhập - ném AuthException nếu sai thông tin
    public boolean login(String inputUsername, String inputPassword) throws AuthException {
        if(this.userName.equals(inputUsername) && this.password.equals(inputPassword)) {
            System.out.println("Đăng nhập thành công: " + this.userName);
            return true;
        }
        else {
            throw new AuthException("Tên đăng nhập hoặc mật khẩu không chính xác!");
        }
    }
    //Đăng xuất
    public void logout() {
        System.out.println("User " + this.userName + " đã đăng xuất");
    }
    //buộc lớp con tự định nghĩa vai trò
    public abstract UserRole getRole();

    //getter
    public String getUserName() { return userName; }
    public String getEmail() { return email; }
    //ko tạo getPassword() để bảo mật

    //setter
    public void setUserName(String userName) {
        this.userName=userName;
    }
    public void setEmail(String email) {
        this.email=email;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    //ko có setCreatedAt() vì ko được thay đổi ngày tạo

    @Override
    public String toString() {
        return "[" + getRole() + "]" + userName + " (" + email + ")";
    }
}