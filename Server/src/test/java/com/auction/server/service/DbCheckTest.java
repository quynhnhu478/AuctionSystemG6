package com.auction.server.service;

import com.auction.server.model.user.User;
import com.auction.server.model.item.Item;
import com.auction.server.repository.UserRepository;
import com.auction.server.repository.ItemRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
@Slf4j
public class DbCheckTest {

    // Nếu không dùng Lombok, hãy bỏ comment dòng dưới đây để tạo logger thủ công:
    // private static final Logger log = LoggerFactory.getLogger(DbCheckTest.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Test
    public void checkUsers() {
        log.info("=== START DB CHECK ===");
        List<User> users = userRepository.findAll();
        for (User u : users) {
            log.info("ID: {} | Name: {} | Pass: {} | Email: {}",
                    u.getId(), u.getName(), u.getPassword(), u.getEmail());
        }
        log.info("=== END DB CHECK ===");

        log.info("=== START ITEMS CHECK ===");
        List<Item> items = itemRepository.findAll();
        for (Item i : items) {
            log.info("ID: {} | Name: {} | Price: {} | Categories: {} | ImageUrl: {} | ImageUrls: {} | EndTime: {}",
                    i.getId(), i.getName(), i.getPrice(), i.getCategories(), i.getImageUrl(), i.getImageUrls(), i.getEndTime());
        }
        log.info("=== END ITEMS CHECK ===");
    }
}