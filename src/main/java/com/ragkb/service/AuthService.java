package com.ragkb.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ragkb.common.BusinessException;
import com.ragkb.common.GlobalExceptionHandler;
import com.ragkb.common.Result;
import com.ragkb.dto.LoginRequest;
import com.ragkb.dto.RegisterRequest;
import com.ragkb.entity.User;
import com.ragkb.mapper.UserMapper;
import com.ragkb.security.JwtUtils;
import com.ragkb.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;

    public void register(RegisterRequest req) {
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, req.getUsername())
        );
        if (count > 0) {
            throw new BusinessException("Username already exists");
        }

        User user = new User();
        user.setUsername(req.getUsername());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setEmail(req.getEmail());
        user.setRole("USER");
        user.setStatus(1);
        userMapper.insert(user);
    }

    public Map<String, Object> login(LoginRequest req) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            req.getUsername(), req.getPassword()
                    )
            );
            UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();

            String token = jwtUtils.generateToken(
                    userDetails.getUserId(),
                    userDetails.getUsername(),
                    userDetails.getRole()
            );

            Map<String, Object> result = new HashMap<>();
            result.put("token", token);
            result.put("userId", userDetails.getUserId());
            result.put("username", userDetails.getUsername());
            result.put("role", userDetails.getRole());
            return result;
        }catch (AuthenticationException e){
            throw new BusinessException("用户名或密码错误");
        }
    }
}
