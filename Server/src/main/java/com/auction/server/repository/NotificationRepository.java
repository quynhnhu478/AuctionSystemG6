package com.auction.server.repository;

import com.auction.server.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    // Thêm dòng này để phục vụ API lấy lịch sử thông báo theo User (sắp xếp mới nhất lên đầu)
    List<Notification> findByUserIdOrderByIdDesc(Long userId);

    // Dòng này phục vụ cho hàm tác vụ ngầm @Scheduled quét bùng kèo (đã viết ở câu trước)
    List<Notification> findByTypeAndHandledFalseAndDeadlineBefore(String type, LocalDateTime deadline);
}
