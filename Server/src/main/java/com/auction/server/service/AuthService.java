package com.auction.server.service;

import com.auction.server.exception.AuthException;

import com.auction.common.payload.LoginRequest;
import com.auction.common.payload.RegisterRequest;
import com.auction.common.payload.UserResponse;
import com.auction.server.model.User.Roles;
import com.auction.server.model.User.User;
import com.auction.server.repository.RoleRepository;
import com.auction.server.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    public AuthService(UserRepository userRepository, RoleRepository roleRepository){

        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    public UserResponse register(RegisterRequest request){
        if (userRepository.findByName(request.getName()) != null){
            throw new AuthException("Username already exists!");
        }
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());
        Roles bidderRole = roleRepository.findByRolename("BIDDER");
        if (bidderRole == null){
            throw new RuntimeException("Role Bidder not found");
        }
        user.getRoles().add(bidderRole);

        userRepository.save(user);
        UserResponse res = mapToResponse(user);
        res.setMessage("Register successfully!");
        return res;
    }
    public UserResponse login(LoginRequest request){
        User user = userRepository.findByNameAndPassword(request.getName(), request.getPassword());
        if (user == null){
            throw new AuthException("Invalid username or password!");
        }
        UserResponse res = mapToResponse(user);
        res.setMessage("Login successfully");
        return res;
    }
    private UserResponse mapToResponse(User user){
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        return response;
    }

}
