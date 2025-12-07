package com.example.datamanagementsystem.repository;

import com.example.datamanagementsystem.entity.File;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileRepository extends JpaRepository<File, String> {
    // You can add custom query methods here if needed
    List<File> findByUploaderId(String uploaderId);
}