package com.auction.server.service;

import com.auction.server.model.user.User;
import com.auction.server.model.item.Item;
import com.auction.server.repository.UserRepository;
import com.auction.server.repository.ItemRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
public class DbCheckTest {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Test
    public void checkUsers() {
        System.out.println("=== START DB CHECK ===");
        List<User> users = userRepository.findAll();
        for (User u : users) {
            System.out.printf("ID: %d | Name: %s | Pass: %s | Email: %s%n",
                u.getId(), u.getName(), u.getPassword(), u.getEmail());
        }
        System.out.println("=== END DB CHECK ===");

        System.out.println("=== START ITEMS CHECK ===");
        List<Item> items = itemRepository.findAll();
        for (Item i : items) {
            System.out.printf("ID: %d | Name: %s | Price: %.2f | Categories: %s | EndTime: %s%n",
                i.getId(), i.getName(), i.getPrice(), i.getCategories(), i.getEndTime());
        }
        System.out.println("=== END ITEMS CHECK ===");
    }
}
