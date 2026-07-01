package com.ragkb.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ragkb.entity.User;
import com.ragkb.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Spring Security loads user from database through this class.
 *
 * Separated from AuthService to avoid circular dependency:
 *   AuthService -> AuthenticationManager -> UserDetailsService -> AuthService
 *
 * Now the dependency chain is:
 *   AuthService -> AuthenticationManager -> UserDetailsServiceImpl (no cycle)
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username)
        );
        if (user == null) {
            throw new UsernameNotFoundException("User not found: " + username);
        }
        return new UserDetailsImpl(
                user.getId(),
                user.getUsername(),
                user.getPassword(),   // BCrypt hashed
                user.getRole()
        );
    }
}
