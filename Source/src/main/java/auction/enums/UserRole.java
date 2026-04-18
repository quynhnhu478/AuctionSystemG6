package auction.enums;

public enum UserRole {
    BIDDER, //người mua
    SELLER, //người bán
    ADMIN; // quản lý hệ thống

}


    
/*enum: kiểu dữ liệu đặc biệt điịnh nghĩa các hằng số cố định
thay vì dùng số (1,2,3) hay dùng chuỗi ("BIDDER","ADMIN","SELLER") thì enum giúp tạo ra 1 "danh sách
chuẩn" cho cả hệ thống
-> 1 user chỉ có thể rơi vào 1 trong 3 trạng thái trên
 */