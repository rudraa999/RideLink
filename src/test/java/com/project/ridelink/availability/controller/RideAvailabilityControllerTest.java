package com.project.ridelink.availability.controller;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.project.ridelink.auth.dto.LoginRequest;
import com.project.ridelink.auth.dto.RegisterRequest;
import com.project.ridelink.auth.repository.RefreshTokenRepository;
import com.project.ridelink.availability.dto.AvailabilityRequest;
import com.project.ridelink.availability.repository.RideAvailabilityRepository;
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
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RideAvailabilityControllerTest {

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
    private RideAvailabilityRepository rideAvailabilityRepository;

    private College testCollege;
    private College otherCollege;
    private String tokenA;
    private String tokenB;
    private String tokenC;
    private String tokenD; // User in a different college

    @BeforeEach
    void setUp() throws Exception {
        rideAvailabilityRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

        // Setup colleges
        if (collegeRepository.count() == 0) {
            testCollege = collegeRepository.save(College.builder()
                    .name("MIT World Peace University (MIT-WPU), Pune")
                    .city("Pune")
                    .build());
        } else {
            testCollege = collegeRepository.findAll().get(0);
        }

        otherCollege = collegeRepository.findByName("COEP Technological University, Pune")
                .orElseGet(() -> collegeRepository.save(College.builder()
                        .name("COEP Technological University, Pune")
                        .city("Pune")
                        .build()));

        // Register and Login Users to get actual tokens
        tokenA = registerAndLogin("usera", "usera@mitwpu.edu.in", testCollege.getId());
        tokenB = registerAndLogin("userb", "userb@mitwpu.edu.in", testCollege.getId());
        tokenC = registerAndLogin("userc", "userc@mitwpu.edu.in", testCollege.getId());
        tokenD = registerAndLogin("userd", "userd@coep.edu.in", otherCollege.getId());
    }

    private String registerAndLogin(String username, String email, Long collegeId) throws Exception {
        // Register
        RegisterRequest register = RegisterRequest.builder()
                .name(username + " Student")
                .username(username)
                .email(email)
                .password("securepassword")
                .collegeId(collegeId)
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated());

        // Login
        LoginRequest login = LoginRequest.builder()
                .usernameOrEmail(username)
                .password("securepassword")
                .build();

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();

        String responseString = result.getResponse().getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(responseString);
        return jsonNode.get("accessToken").asText();
    }

    @Test
    void testActivateAndGetMe() throws Exception {
        AvailabilityRequest request = AvailabilityRequest.builder()
                .destination("Kothrud")
                .departureTime(LocalDateTime.now().plusMinutes(10))
                .transportType("AUTO")
                .build();

        mockMvc.perform(post("/api/availabilities")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.destination", is("Kothrud")))
                .andExpect(jsonPath("$.transportType", is("AUTO")))
                .andExpect(jsonPath("$.active", is(true)));

        // Get me
        mockMvc.perform(get("/api/availabilities/me")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.destination", is("Kothrud")))
                .andExpect(jsonPath("$.active", is(true)));
    }

    @Test
    void testDeactivateAvailability() throws Exception {
        // Activate first
        AvailabilityRequest request = AvailabilityRequest.builder()
                .destination("Kothrud")
                .departureTime(LocalDateTime.now().plusMinutes(10))
                .transportType("AUTO")
                .build();

        mockMvc.perform(post("/api/availabilities")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Deactivate
        mockMvc.perform(put("/api/availabilities/deactivate")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Availability deactivated successfully")));

        // Check if inactive
        mockMvc.perform(get("/api/availabilities/me")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active", is(false)));
    }

    @Test
    void testFindCompatibleMatches() throws Exception {
        LocalDateTime departureTime = LocalDateTime.now().plusMinutes(15);

        // User A (searcher) activates availability for Kothrud
        AvailabilityRequest reqA = AvailabilityRequest.builder()
                .destination("Kothrud")
                .departureTime(departureTime)
                .transportType("ANY")
                .build();
        mockMvc.perform(post("/api/availabilities")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqA)))
                .andExpect(status().isOk());

        // User B (same college, same destination, same time) activates -> Should match
        AvailabilityRequest reqB = AvailabilityRequest.builder()
                .destination("Kothrud")
                .departureTime(departureTime.plusMinutes(10)) // within 60 mins
                .transportType("AUTO")
                .build();
        mockMvc.perform(post("/api/availabilities")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqB)))
                .andExpect(status().isOk());

        // User C (same college, different destination) activates -> Should NOT match
        AvailabilityRequest reqC = AvailabilityRequest.builder()
                .destination("Katraj")
                .departureTime(departureTime)
                .transportType("CAB")
                .build();
        mockMvc.perform(post("/api/availabilities")
                        .header("Authorization", "Bearer " + tokenC)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqC)))
                .andExpect(status().isOk());

        // User D (different college, same destination) activates -> Should NOT match
        AvailabilityRequest reqD = AvailabilityRequest.builder()
                .destination("Kothrud")
                .departureTime(departureTime)
                .transportType("ANY")
                .build();
        mockMvc.perform(post("/api/availabilities")
                        .header("Authorization", "Bearer " + tokenD)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqD)))
                .andExpect(status().isOk());

        // Fetch compatible matches for User A
        mockMvc.perform(get("/api/availabilities/compatible")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1))) // Only User B should match!
                .andExpect(jsonPath("$[0].username", is("userb")))
                .andExpect(jsonPath("$[0].destination", is("Kothrud")))
                .andExpect(jsonPath("$[0].transportType", is("AUTO")));
    }
}
