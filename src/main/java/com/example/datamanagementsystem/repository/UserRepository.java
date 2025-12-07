package com.example.datamanagementsystem.repository;

import com.example.datamanagementsystem.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    // You can add custom query methods here if needed
    User findByUsername(String username);
}