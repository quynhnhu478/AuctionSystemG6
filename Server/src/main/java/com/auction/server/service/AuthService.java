package com.auction.server.service;

import com.auction.server.exception.AuthException;
import com.auction.server.model.User;
import com.auction.server.payload.LoginRequest;
import com.auction.server.payload.RegisterRequest;
import com.auction.server.payload.UserResponse;
import com.auction.server.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository){
        this.userRepository = userRepository;
    }

    public UserResponse register(RegisterRequest request){
        if (userRepository.findByName(request.getName()) != null){
            throw new AuthException("Username already exists!");
        }
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());
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
