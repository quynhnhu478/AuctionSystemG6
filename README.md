# BÀI TẬP LỚN: HỆ THỐNG ĐẤU GIÁ TRỰC TUYẾN (ONLINE AUCTION SYSTEM)

## 1. Mô tả bài toán và Phạm vi hệ thống

### 1.1. Mô tả bài toán
Hệ thống là một nền tảng phần mềm đa người dùng (Multi-user) vận hành theo kiến trúc Client - Server nhằm kết nối người bán (Seller) và người mua (Bidder) trong một môi trường số hóa an toàn, minh bạch dựa trên mô hình đấu giá tiêu biểu (tương tự eBay Auctions).

Thay vì mua bán với mức giá cố định, Seller sẽ đăng tải sản phẩm kèm theo mức giá khởi điểm và thời hạn đấu giá xác định. Các Bidder tham gia cạnh tranh công khai bằng cách trả giá (đặt bid) cao hơn giá hiện tại theo thời gian thực. Khi thời gian phiên đấu giá kết thúc, hệ thống tự động khóa sổ, xác định người thắng cuộc là người đã trả mức giá hợp lệ cao nhất tại thời điểm đóng phiên.

### 1.2. Phạm vi hệ thống

#### A. Các chức năng cốt lõi (Nằm trong phạm vi thực hiện bắt buộc)
* **Quản lý người dùng đa vai trò:** Phân quyền và xác thực (Đăng ký/Đăng nhập) cho 3 nhóm đối tượng: `Admin` (Quản trị viên), `Seller` (Người bán), và `Bidder` (Người mua/đặt giá).
* **Quản lý sản phẩm và phiên đấu giá:** Hỗ trợ Seller thiết lập thông tin sản phẩm (Tên, mô tả, hình ảnh mô phỏng, giá khởi điểm, thời gian bắt đầu & kết thúc).
* **Giao dịch đấu giá Realtime:** Cho phép Bidder đặt giá, tự động kiểm tra tính hợp lệ của mức giá mới và đồng bộ dữ liệu tức thời đến tất cả các Client đang theo dõi.
* **Tự động hóa vòng đời phiên đấu giá:** Tự động hóa quá trình đóng/chuyển đổi trạng thái phiên theo đúng quy trình logic hệ thống: `OPEN` -> `RUNNING` -> `FINISHED` -> `PAID` / `CANCELED`.
* **Xử lý đồng thời (Concurrency):** Đảm bảo hệ thống hoạt động ổn định khi có nhiều người cùng đặt giá tại một mili-giây, ngăn chặn tuyệt đối lỗi mất dữ liệu cập nhật (`Lost update`), rollback giá bừa bãi hoặc lỗi race condition.
* **Xử lý ngoại lệ và giao diện trực quan:** Hệ thống bắt và xử lý triệt để các lỗi nghiệp vụ (đặt giá thấp hơn giá hiện tại, đấu giá khi phiên đã đóng, lỗi kết nối mạng). Giao diện đồ họa tách biệt theo mô hình kiến trúc MVC.

#### B. Các tính năng mở rộng nâng cao
* **Đấu giá tự động (Auto-Bidding):** Hệ thống tự động thay mặt người dùng nâng giá dựa trên mức giá tối đa (`maxBid`) và bước giá (`increment`) được cấu hình trước.
* **Gia hạn thời gian tự động (Anti-sniping Algorithm):** Tự động kéo dài thời gian phiên đấu giá thêm Y giây nếu xuất hiện lượt đặt giá mới trong X giây cuối cùng trước khi đóng phiên nhằm chống tình trạng "bắn tỉa" giá ở giây cuối.

#### C. Giới hạn hệ thống (Nằm ngoài phạm vi đồ án)
* Hệ thống không tích hợp cổng thanh toán trực tuyến thực tế (như Paypal, VNPay...), việc thanh toán chỉ dừng lại ở mức mô phỏng chuyển đổi trạng thái phiên đấu giá sang `PAID`.
* Dữ liệu chỉ được truy cập gián tiếp thông qua phía Server (Server độc quyền tương tác với database), phía Client không can thiệp trực tiếp vào database để đảm bảo tính an toàn kiến trúc.

---

## 2. Công nghệ sử dụng & Môi trường cài đặt

* **Ngôn ngữ lập trình:** Java (JDK 21)
* **Công nghệ & Thư viện chính:**
  * **FrameWork:** SpringBoot (Java)
  * **Networking:** Spring Boot Rest API, WebSocket(STOMP). 
  * **Giao diện người dùng (GUI):** JavaFX + FXML.
  * **Cơ sở dữ liệu:** MySQL trên Aiven Cloud
  * **Quản lý dự án & Build tool:** Maven .
  * **Kiểm thử tự động:** JUnit 5 (Dành cho kiểm thử các logic nghiệp vụ quan trọng).
  * **CI/CD**: GitHub Actions
  * **Cloud lưu trữ ảnh**: Cloudinary CDN
* **Môi trường chạy hệ thống:** Cross-platform (Windows 10/11, macOS, Linux).
* **Yêu cầu cài đặt:** Máy tính chạy Server/Client cần cài đặt sẵn Java Runtime Environment (JRE) hoặc Java Development Kit (JDK) phiên bản 17 trở lên.

---

## 3. Cấu trúc thư mục ứng dụng
Dự án được phân tầng rõ ràng theo mô hình kiến trúc Client - Server kết hợp mô hình thiết kế MVC:

```text
AuctionSystemG6
 ┣ .github/workflows
 ┃ ┗ cicd.yml           # Cấu hình CI/CD tự động chạy JUnit khi push code
 ┣ server                   # MODULE SERVER
 ┃ ┣ src
 ┃ ┃ ┣ main
 ┃ ┃ ┃ ┣ java               # Mã nguồn xử lý logic phía Server (Controller, Model, DAO)
 ┃ ┃ ┃ ┃ ┗ com/auction/server
 ┃ ┃ ┃ ┃   ┣ config
 ┃ ┃ ┃ ┃   ┣ controller
 ┃ ┃ ┃ ┃   ┣ exception
 ┃ ┃ ┃ ┃   ┣ model
 ┃ ┃ ┃ ┃   ┣ repository
 ┃ ┃ ┃ ┃   ┣ service
 ┃ ┃ ┃ ┃   ┣ util
 ┃ ┃ ┃ ┃   ┗ ServerApplication.java
 ┃ ┃ ┃ ┗ resources          # File cấu hình Server (Database, Port,...)
 ┃ ┃ ┃     ┗ application.properties
 ┃ ┃ ┗ test
 ┃ ┃   ┃ java               # Mã nguồn Unit Test (JUnit 5) kiểm thử logic đấu giá
 ┃ ┃   ┗ resources
 ┃ ┣ pom.xml                # File quản lý thư viện Maven của Server
 ┃ ┗ target
 ┃ ┃ ┣ app.logs
 ┃ ┃ ┣ Server-0.0.1-SNAPSHOT.jar.original
 ┃ ┃ ┗ Server-0.0.1-SNAPSHOT.jar
 ┃ ┣ database.sql
 ┃ ┗ uploads
 ┣ client                   # MODULE CLIENT
 ┃ ┣ src
 ┃ ┃ ┗ main
 ┃ ┃   ┣ java               # Mã nguồn giao diện và xử lý kết nối phía Client
 ┃ ┃   ┃ ┣ com.auction.client
 ┃ ┃   ┃ ┃ ┣ app
 ┃ ┃   ┃ ┃ ┣ config
 ┃ ┃   ┃ ┃ ┣ controller
 ┃ ┃   ┃ ┃ ┣ service
 ┃ ┃   ┃ ┃ ┗ Launcher
 ┃ ┃   ┃ ┗ module-info.java
 ┃ ┃   ┗ resources          # Nơi chứa các file thiết kế giao diện FXML, css, images (JavaFX)
 ┃ ┃     ┗ com/auction/client
 ┃ ┃       ┣ css
 ┃ ┃       ┣ fxml
 ┃ ┃       ┗ images
 ┃ ┣ target
 ┃ ┃ ┣ Client-1.0-SNAPSHOT.jar
 ┃ ┃ ┗ original-Client-1.0-SNAPSHOT.jar
 ┃ ┗ pom.xml                # File quản lý thư viện Maven của Client
 ┣ common                 # Thư mục chứa các lớp DTO, DAO dùng chung
 ┃ ┣ src
 ┃ ┃ ┗ main
 ┃ ┃   ┗ java
 ┃ ┃     ┣ com/auction/common
 ┃ ┃     ┃ ┣ enum
 ┃ ┃     ┃ ┗ payload
 ┃ ┃     ┗ module-info.java
 ┃ ┣ target
 ┃ ┃ ┗ common.jar
 ┃ ┗ pom.xml
 ┗ .gitignore
```

## 4. Vị trí các file .jar
* Server Executable File: ./server/target/Server-0.0.1-SNAPSHOT.jar
* Client Executable File: ./client/targetr/Client-1.0-SNAPSHOT.jar

## 5. Hướng dẫn chạy Server/Client
Vui lòng tuân thủ nghiêm ngặt thứ tự khởi chạy dưới đây để tránh lỗi ngoại lệ mất kết nối (Connection Refused):

* **Bước 1:** Khởi chạy Server (Bắt buộc chạy trước)
Mở Terminal (hoặc Command Prompt) tại thư mục gốc của dự án.
```text
cd Server
```
Chạy lệnh khởi động Server:
```text
java -jar Server-0.0.1-SNAPSHOT.jar
```
Khi Server khởi chạy thành công, màn hình console sẽ hiển thị thông báo trạng thái sẵn sàng kết nối cơ sở dữ liệu và lắng nghe Client trên cổng mạng chỉ định (ví dụ: Server listening on port 8080...).
* **Bước 2:** Khởi chạy Client (Chạy sau khi Server đã bật)
Mở một Terminal mới độc lập (hoặc click đúp chuột vào file client.jar).
Di chuyển vào thư mục chứa file jar và chạy lệnh:
```text
java -jar Client-1.0-SNAPSHOT.jar
```
Giao diện đăng nhập hiện ra. Nhập thông tin cấu hình mạng kết nối đến IP Server (Sử dụng localhost hoặc 127.0.0.1 nếu kiểm thử trên cùng một máy) và số Port tương ứng để kết nối vào hệ thống đấu giá.

## 6. Danh sách chức năng đã hoàn thành
### 6.1 Chức năng bắt buộc
* **Quản lý người dùng:** Đăng ký, đăng nhập tài khoản. Phân quyền và hiển thị giao diện động theo 3 vai trò: Admin, Seller, Bidder.
* **Quản lý sản phẩm:** Seller có toàn quyền thêm, sửa, xóa thông tin chi tiết các mặt hàng đấu giá (tên, mô tả, giá khởi điểm, thời gian cấu hình).
* **Tham gia đấu giá:** Kiểm tra tính hợp lệ chặt chẽ của giá bid nhập vào, cập nhật người dẫn đầu ngay lập tức.
* **Kết thúc phiên tự động:** Tự động ngắt phiên khi hết thời gian, xác định chính xác người thắng cuộc và cập nhật trạng thái vòng đời phiên (OPEN -> RUNNING -> FINISHED).
* **Xử lý lỗi & Ngoại lệ:** Xử lý triệt để các trường hợp đặt giá phá luật (thấp hơn giá hiện tại), đấu giá muộn khi phiên đã đóng, lỗi mất kết nối đột ngột...
* **Giao diện người dùng (GUI):** Triển khai các giải pháp xử lý tranh chấp tài nguyên (Concurrency control) giúp hệ thống không bị lỗi Lost update hay trùng lặp người thắng khi có bão đặt giá cùng mili-giây.
### 6.2 Chức năng nâng cao & tối ưu thuật toán
* **Realtime Update:** Áp dụng nâng cao mẫu thiết kế Observer Pattern kết hợp kết nối Socket để cập nhật giá tức thời tới mọi client đang xem mà không cần tải lại trang.
* **Auto-Bidding (Đấu giá tự động):** Cho phép đặt mức giá trần tối đa và bước giá. Sử dụng cấu trúc dữ liệu PriorityQueue để giải quyết xung đột ưu tiên thời gian đặt bid.
* **Gia hạn phiên (Anti-sniping Algorithm):** Tự động phát hiện bid trong 10 giây cuối để kéo dài thời gian thêm 30 giây, tạo sự công bằng cho phiên đấu giá.

## 7. Link báo cáo PDF và Video Demo
* **Tài liệu Báo cáo đồ án (PDF):** https://ap.wps.com/cms/docs/d/cbTaqtkwBO7ii6ZC
* **Video Demo sản phẩm:** 
