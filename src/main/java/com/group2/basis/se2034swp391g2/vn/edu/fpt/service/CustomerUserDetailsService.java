package com.group2.basis.se2034swp391g2.vn.edu.fpt.service;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.User;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import lombok.*;
@Service
@RequiredArgsConstructor
public class CustomerUserDetailsService implements UserDetailsService {

     private final UserRepository userRepository;

     @Override

    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException{
         User user = userRepository.findByEmailDetail(email).orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản email: " + email));
         return new CustomerUserDetails(user);
     }
}
