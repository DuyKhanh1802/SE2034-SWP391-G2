package com.group2.basis.se2034swp391g2.vn.edu.fpt.service;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.User;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request.AccountCreateRequest;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request.AccountUpdateRequest;

import java.util.List;

public interface UserService {
    List<User> getAllStaffUsers();
    User getUserById(Long id);
    void createUser(AccountCreateRequest request);
    void updateUser(Long id, AccountUpdateRequest request);
}