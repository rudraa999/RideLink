package com.project.ridelink.config;

import com.project.ridelink.college.entity.College;
import com.project.ridelink.college.repository.CollegeRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final CollegeRepository collegeRepository;

    public DataInitializer(CollegeRepository collegeRepository) {
        this.collegeRepository = collegeRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if (collegeRepository.count() == 0) {
            College mitWpu = College.builder()
                    .name("MIT World Peace University (MIT-WPU), Pune")
                    .city("Pune")
                    .build();
            collegeRepository.save(mitWpu);
            System.out.println("Seeded database with initial college: " + mitWpu.getName());
        }
    }
}
