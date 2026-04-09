package auction.model;
import auction.exception.AuthException;
import java.time.LocalDateTime;
public abstract class User extends Entity{
    protected String userId, userName, password, email;
    protected LocalDateTime createdAt;
    public User(String id, String name, String userId, String userName, String password, String email) {
        super(id, name);
        this.userId=userId;
        this.userName=userName;
        this.password=password;
        this.email=email;
        this.createdAt=LocalDateTime.now(); //lấy giờ hệ thống tại thời điểm này
    }
    //Nếu sai tài khoản/mật khẩu
    public boolean login(String inputUsername, String inputPassword) throws AuthException {
        if (this.userName.equals(inputUsername) && this.password.equals(inputPassword)) {
            System.out.println("Đăng nhập thành công!");
            return true;
        } else {
            throw new AuthException("Tên đăng nhập hoặc mật khẩu không chính xác!");
        }
    }
    public void logout(){
        System.out.println("User" + this.userName + "đã đăng xuất");
    }
    public String getUserId(){return userId;}
    public String getUserName(){return userName;}
    public void setUserName(){}
    public void setPassword(){this.password=password;}//ko tạo getPassword để bảo mật
    public String getEmail(){return email;}
    //ko tạo setCreatedAt vì ko thể thay đổi ngày tạo
    public LocalDateTime getCreatedAt(){
        return createdAt;
    }
    public abstract UserRole getRole();//buộc lớp con định nghĩa vai trò
}

