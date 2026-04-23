package auction.model.base;
import auction.enums.UserRole;

public abstract class User extends Entity {

    private String name;
    private String email;
    private double balance;
    private UserRole role;

    protected User() //tạo đối tượng tạm thời
    {
        // -> tự động tăng 1,2,3,...
        super();
    }

    protected User(String name, String email, double balance, UserRole role) //người dùng nhập form
    {
        //->tự động tăng 1,2,3...
        super();
        this.name = name;
        this.email = email;
        this.balance = balance;
        this.role = role;
        //User hoàn chỉnh với ID tự động sinh ra mà không cần phải truyền ID bằng tay
    }

    protected User(String id, String name, String email, double balance, UserRole role) //đọc danh sách từ file đã lưu
    {
        super(id);// dùng lại ID cũ, KHÔNG bấm số mới
        this.name = name;
        this.email = email;
        this.balance = balance;
        this.role = role;
    }

    //để lớp con tự định danh
    public abstract boolean canBid();
    public abstract boolean canSell();
    public abstract boolean canAdmin();

    @Override
    public String getDisplayInfo() {
        return String.format("[%s] %s <%s> — Balance: $%.0f", role, name, email, balance);
    }

    public boolean deductBalance(double amount) {
        if (balance < amount) return false;
        balance -= amount;
        return true;
    }

    public void addBalance(double amount) {
        if (amount > 0) balance += amount;
    }

    // Getters & Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }

    public UserRole getRole() { return role; }
    protected void setRole(UserRole role) { this.role = role; }

    @Override
    public String toString() { return name + " (" + role + ")"; }
}
/*3 constructor:
ví dụ : đã có một người tên "Bình" với ID là "99".
Nếu dùng constructor số 2, "Bình" sẽ bị máy bấm số cấp cho ID mới (ví dụ là "1").
-> làm sai lệch toàn bộ dữ liệu lịch sử của Bình -> constructor số 3 dùng để khôi phục đúng Bình với id là 99
 */