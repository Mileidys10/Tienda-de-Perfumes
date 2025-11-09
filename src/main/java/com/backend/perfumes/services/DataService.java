package com.backend.perfumes.services;

import com.backend.perfumes.model.Role;
import com.backend.perfumes.model.User;
import com.backend.perfumes.repositories.UserRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class DataService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;


    @PostConstruct
    public void init(){
//        User admin = new User();
//        admin.setEmail("mileidys@gmail.com");
//        admin.setPassword(passwordEncoder.encode("admin123"));
//        admin.setName("Mileidys");
//        admin.setLastName("Agamez");
//       admin.setActive(true);
//       admin.setRole(Role.ADMIN);
//       userRepository.save(admin);
    }

}
