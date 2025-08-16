package com.backend.perfumes.services;


import com.backend.perfumes.model.User;
import com.backend.perfumes.repositories.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.graphql.ConditionalOnGraphQlSchema;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class UserDetailService implements UserDetailsService {
    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        //looking users by email
        User user = userRepository.findByEmail(email).orElseThrow(() -> {
            log.info("No user found with email {}", email);
            return new UsernameNotFoundException(" user not found " );
        });

        log.info("User found : {} with Role:  {}",email,user.getRole());

        return user;
    }



}
