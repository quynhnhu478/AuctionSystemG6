package com.auction.server.service;

import com.auction.server.exception.AuthException;

import com.auction.common.payload.LoginRequest;
import com.auction.common.payload.RegisterRequest;
import com.auction.common.payload.UserResponse;
import com.auction.server.model.user.Roles;
import com.auction.server.model.user.SellerRegistration;
import com.auction.server.model.user.User;
import com.auction.server.repository.RoleRepository;
import com.auction.server.repository.SellerRegistrationRepository;
import com.auction.server.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class AuthService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final SellerRegistrationRepository sellerRegistrationRepository;
    public AuthService(UserRepository userRepository, RoleRepository roleRepository,
                        SellerRegistrationRepository sellerRegistrationRepository){

        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.sellerRegistrationRepository = sellerRegistrationRepository;

    }

    public UserResponse register(RegisterRequest request){
        String name = normalize(request.getName());
        String email = normalize(request.getEmail());
        String password = request.getPassword() == null ? "" : request.getPassword();
        if (name.isBlank() || password.isBlank()) {
            throw new AuthException("Username and password are required!");
        }
        if (userRepository.findByName(name) != null){
            throw new AuthException("Username already exists!");
        }
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(password);
        Roles bidderRole = ensureRole("BIDDER");
        user.getRoles().add(bidderRole);

        userRepository.save(user);
        UserResponse res = mapToResponse(user);
        res.setMessage("Register successfully!");
        return res;
    }
    public UserResponse login(LoginRequest request){
        String name = normalize(request.getName());
        String password = request.getPassword() == null ? "" : request.getPassword();
        if (name.isBlank() || password.isBlank()) {
            throw new AuthException("Username and password are required!");
        }
        User user = userRepository.findByNameAndPassword(name, password);
        if (user == null){
            throw new AuthException("Invalid username or password!");
        }
        UserResponse res = mapToResponse(user);
        res.setMessage("Login successfully");

        Optional<SellerRegistration> registrationOpt = sellerRegistrationRepository.findById(user.getId());
        if (registrationOpt.isPresent()){
            res.setSellerStatus(registrationOpt.get().getStatus());
        }
        else{
            res.setSellerStatus(null);
        }
        return res;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private Roles ensureRole(String roleName) {
        Roles role = roleRepository.findByRolename(roleName);
        if (role != null) {
            return role;
        }
        role = new Roles();
        role.setRolename(roleName);
        return roleRepository.save(role);
    }
    private UserResponse mapToResponse(User user){
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        response.setBalance(user.getBalance());
        if (user.getRoles() != null){
            Set<String> roleNames = user.getRoles().stream()
                    .map(role -> role.getRolename())
                    .collect(Collectors.toSet());
            response.setRoles(roleNames);
        }
        return response;
    }
    @Transactional(readOnly = true)
    public List<UserResponse> usersList() {
        List<User> users = userRepository.findAll();
        List<UserResponse> responseList = new ArrayList<>();

        for (User user : users) {
            UserResponse res = new UserResponse();
            res.setId(user.getId());
            res.setName(user.getName());
            res.setEmail(user.getEmail());
            res.setBalance(user.getBalance());
            res.setMessage("Lấy dữ liệu thành công");
            Set<String> roleNames = new HashSet<>();
            if (user.getRoles() != null) {
                for (Roles role : user.getRoles()) {
                    roleNames.add(role.getRolename());
                }
            }
            res.setRoles(roleNames);
            SellerRegistration registration = user.getSellerRegistration();

            if (registration != null) {
                res.setSellerStatus(registration.getStatus());
            } else {
                res.setSellerStatus(null);
            }
            responseList.add(res);
        }

        return responseList;
    }
    public UserResponse updateUserBalance(Long userId, double balance){
        User user = userRepository.findById(userId).orElse(null);
        if (user == null){
            throw new AuthException("User not found!");
        }
        user.setBalance(balance);
        userRepository.save(user);
        return mapToResponse(user);
    }

}
