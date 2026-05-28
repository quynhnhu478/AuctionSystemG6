package com.auction.server.config;

import com.auction.server.model.user.Roles;
import com.auction.server.model.user.User;
import com.auction.server.repository.RoleRepository;
import com.auction.server.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Slf4j
@Configuration
@Profile("!test")
public class DatabaseSeedConfig {
    private static final String DEFAULT_ADMIN_NAME = "admin";
    private static final String DEFAULT_ADMIN_EMAIL = "admin@auction.local";
    private static final String DEFAULT_ADMIN_PASSWORD = "admin";

    @Bean
    ApplicationRunner seedRequiredDatabaseData(RoleRepository roleRepository, UserRepository userRepository) {
        return args -> seed(roleRepository, userRepository);
    }

    @Transactional
    void seed(RoleRepository roleRepository, UserRepository userRepository) {
        Roles adminRole = ensureRole(roleRepository, "ADMIN");
        ensureRole(roleRepository, "SELLER");
        ensureRole(roleRepository, "BIDDER");

        User admin = userRepository.findByName(DEFAULT_ADMIN_NAME);
        if (admin == null) {
            admin = new User();
            admin.setName(DEFAULT_ADMIN_NAME);
            admin.setEmail(DEFAULT_ADMIN_EMAIL);
            admin.setPassword(DEFAULT_ADMIN_PASSWORD);
            admin.setRoles(new HashSet<>(Set.of(adminRole)));
            userRepository.save(admin);
            log.info("Created default admin user '{}'", DEFAULT_ADMIN_NAME);
        } else if (admin.getRoles().stream().noneMatch(role -> "ADMIN".equals(role.getRolename()))) {
            admin.getRoles().add(adminRole);
            userRepository.save(admin);
            log.info("Added ADMIN role to existing user '{}'", DEFAULT_ADMIN_NAME);
        }
    }

    private Roles ensureRole(RoleRepository roleRepository, String roleName) {
        Roles role = roleRepository.findByRolename(roleName);
        if (role != null) {
            return role;
        }

        role = new Roles();
        role.setRolename(roleName);
        Roles savedRole = roleRepository.save(role);
        log.info("Created role '{}'", roleName);
        return savedRole;
    }
}
