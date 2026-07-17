package com.project.ridelink.college.controller;

import com.project.ridelink.college.entity.College;
import com.project.ridelink.college.repository.CollegeRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/colleges")
public class CollegeController {

    private final CollegeRepository collegeRepository;

    public CollegeController(CollegeRepository collegeRepository) {
        this.collegeRepository = collegeRepository;
    }

    @GetMapping
    public ResponseEntity<List<College>> getAllColleges() {
        List<College> colleges = collegeRepository.findAll();
        return ResponseEntity.ok(colleges);
    }
}
