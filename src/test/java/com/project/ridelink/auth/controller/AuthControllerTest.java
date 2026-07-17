package com.project.ridelink.auth.controller;

import tools.jackson.databind.ObjectMapper;
import com.project.ridelink.auth.dto.LoginRequest;
import com.project.ridelink.auth.dto.RegisterRequest;
import com.project.ridelink.auth.repository.RefreshTokenRepository;
import com.project.ridelink.college.entity.College;
import com.project.ridelink.college.repository.CollegeRepository;
import com.project.ridelink.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CollegeRepository collegeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private com.project.ridelink.availability.repository.RideAvailabilityRepository rideAvailabilityRepository;

    @Autowired
    private com.project.ridelink.chat.repository.ChatMessageRepository chatMessageRepository;

    @Autowired
    private com.project.ridelink.riderequest.repository.RideRequestRepository rideRequestRepository;

    @Autowired
    private com.project.ridelink.ridematch.repository.RideMatchMemberRepository rideMatchMemberRepository;

    @Autowired
    private com.project.ridelink.ridematch.repository.RideMatchRepository rideMatchRepository;

    private College testCollege;

    @BeforeEach
    void setUp() {
        chatMessageRepository.deleteAll();
        rideRequestRepository.deleteAll();
        rideMatchMemberRepository.deleteAll();
        rideMatchRepository.deleteAll();
        rideAvailabilityRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

        // Ensure there is at least one college
        if (collegeRepository.count() == 0) {
            testCollege = collegeRepository.save(College.builder()
                    .name("MIT World Peace University (MIT-WPU), Pune")
                    .city("Pune")
                    .build());
        } else {
            testCollege = collegeRepository.findAll().get(0);
        }
    }

    @Test
    void testGetColleges() throws Exception {
        mockMvc.perform(get("/api/colleges"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$[0].name", is(testCollege.getName())));
    }

    @Test
    void testRegisterUser() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .name("Test Student")
                .username("teststudent")
                .email("teststudent@mitwpu.edu.in")
                .password("securepassword")
                .collegeId(testCollege.getId())
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message", is("User registered successfully")));
    }

    @Test
    void testLoginUserSuccess() throws Exception {
        // First register
        RegisterRequest register = RegisterRequest.builder()
                .name("Login Test Student")
                .username("logintest")
                .email("logintest@mitwpu.edu.in")
                .password("securepassword")
                .collegeId(testCollege.getId())
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated());

        // Then Login
        LoginRequest login = LoginRequest.builder()
                .usernameOrEmail("logintest")
                .password("securepassword")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("refreshToken"))
                .andExpect(cookie().httpOnly("refreshToken", true))
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.username", is("logintest")))
                .andExpect(jsonPath("$.name", is("Login Test Student")))
                .andExpect(jsonPath("$.collegeName", is(testCollege.getName())));
    }

    @Test
    void testLoginUserFailure() throws Exception {
        LoginRequest login = LoginRequest.builder()
                .usernameOrEmail("nonexistent")
                .password("wrongpassword")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isUnauthorized());
    }
}
