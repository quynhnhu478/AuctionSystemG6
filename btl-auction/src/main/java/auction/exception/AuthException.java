package auction.exception;
public class AuthException extends Exception {
    public AuthException(String message) {
        super(message);
    }
}
//check khớp dữ liệu đã lưu ko nếu ko ném ra exception