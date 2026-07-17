package com.project.ridelink.riderequest.controller;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.project.ridelink.auth.dto.LoginRequest;
import com.project.ridelink.auth.dto.RegisterRequest;
import com.project.ridelink.auth.repository.RefreshTokenRepository;
import com.project.ridelink.availability.dto.AvailabilityRequest;
import com.project.ridelink.availability.repository.RideAvailabilityRepository;
import com.project.ridelink.college.entity.College;
import com.project.ridelink.college.repository.CollegeRepository;
import com.project.ridelink.ridematch.repository.RideMatchMemberRepository;
import com.project.ridelink.ridematch.repository.RideMatchRepository;
import com.project.ridelink.riderequest.repository.RideRequestRepository;
import com.project.ridelink.user.entity.User;
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
class RideRequestAndMatchTest {

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

    @Autowired
    private RideRequestRepository rideRequestRepository;

    @Autowired
    private RideMatchRepository rideMatchRepository;

    @Autowired
    private RideMatchMemberRepository rideMatchMemberRepository;

    @Autowired
    private com.project.ridelink.chat.repository.ChatMessageRepository chatMessageRepository;

    private College testCollege;
    private User studentA;
    private User studentB;
    private User studentC;
    private String tokenA;
    private String tokenB;
    private String tokenC;

    @BeforeEach
    void setUp() throws Exception {
        chatMessageRepository.deleteAll();
        rideRequestRepository.deleteAll();
        rideMatchMemberRepository.deleteAll();
        rideMatchRepository.deleteAll();
        rideAvailabilityRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

        // Setup college
        if (collegeRepository.count() == 0) {
            testCollege = collegeRepository.save(College.builder()
                    .name("MIT World Peace University (MIT-WPU), Pune")
                    .city("Pune")
                    .build());
        } else {
            testCollege = collegeRepository.findAll().get(0);
        }

        // Register & login users
        tokenA = registerAndLogin("usera", "usera@mitwpu.edu.in");
        tokenB = registerAndLogin("userb", "userb@mitwpu.edu.in");
        tokenC = registerAndLogin("userc", "userc@mitwpu.edu.in");

        studentA = userRepository.findByUsername("usera").orElseThrow();
        studentB = userRepository.findByUsername("userb").orElseThrow();
        studentC = userRepository.findByUsername("userc").orElseThrow();

        // Activate availabilities (essential to send/receive requests!)
        activateAvailability(tokenA, "Karve Nagar");
        activateAvailability(tokenB, "Karve Nagar");
        activateAvailability(tokenC, "Karve Nagar");
    }

    private String registerAndLogin(String username, String email) throws Exception {
        RegisterRequest register = RegisterRequest.builder()
                .name(username + " Student")
                .username(username)
                .email(email)
                .password("securepassword")
                .collegeId(testCollege.getId())
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated());

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

    private void activateAvailability(String token, String destination) throws Exception {
        AvailabilityRequest request = AvailabilityRequest.builder()
                .destination(destination)
                .departureTime(LocalDateTime.now().plusMinutes(20))
                .transportType("ANY")
                .build();

        mockMvc.perform(post("/api/availabilities")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void testSendAndGetRequests() throws Exception {
        // User A sends request to User B
        MvcResult result = mockMvc.perform(post("/api/requests/send/" + studentB.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.senderUsername", is("usera")))
                .andExpect(jsonPath("$.receiverUsername", is("userb")))
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.destination", is("Karve Nagar")))
                .andReturn();

        // Get incoming for User B
        mockMvc.perform(get("/api/requests/incoming")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].senderUsername", is("usera")));

        // Get outgoing for User A
        mockMvc.perform(get("/api/requests/outgoing")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].receiverUsername", is("userb")));
    }

    @Test
    void testCancelAndRejectRequests() throws Exception {
        // User A sends request to User B
        MvcResult res1 = mockMvc.perform(post("/api/requests/send/" + studentB.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andReturn();
        String json1 = res1.getResponse().getContentAsString();
        long reqId1 = objectMapper.readTree(json1).get("id").asLong();

        // User A cancels it
        mockMvc.perform(put("/api/requests/" + reqId1 + "/cancel")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());

        // Verify no pending incoming for User B
        mockMvc.perform(get("/api/requests/incoming")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // User C sends request to User B
        MvcResult res2 = mockMvc.perform(post("/api/requests/send/" + studentB.getId())
                        .header("Authorization", "Bearer " + tokenC))
                .andExpect(status().isOk())
                .andReturn();
        String json2 = res2.getResponse().getContentAsString();
        long reqId2 = objectMapper.readTree(json2).get("id").asLong();

        // User B rejects it
        mockMvc.perform(put("/api/requests/" + reqId2 + "/reject")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk());

        // Verify no pending incoming for User B
        mockMvc.perform(get("/api/requests/incoming")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void testAcceptRequestCreatesMatchAndCleansUp() throws Exception {
        // User A sends request to User B
        MvcResult res1 = mockMvc.perform(post("/api/requests/send/" + studentB.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andReturn();
        long reqId1 = objectMapper.readTree(res1.getResponse().getContentAsString()).get("id").asLong();

        // User C sends request to User B (competing request)
        mockMvc.perform(post("/api/requests/send/" + studentB.getId())
                        .header("Authorization", "Bearer " + tokenC))
                .andExpect(status().isOk());

        // User B accepts User A's request
        mockMvc.perform(put("/api/requests/" + reqId1 + "/accept")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk());

        // Verify both User A and User B availabilities deactivated
        mockMvc.perform(get("/api/availabilities/me")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active", is(false)));

        mockMvc.perform(get("/api/availabilities/me")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active", is(false)));

        // Verify other pending requests for User B are cleaned up (User C's request should be rejected/cancelled)
        mockMvc.perform(get("/api/requests/incoming")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // Verify active match is created
        MvcResult matchResult = mockMvc.perform(get("/api/matches/active")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ACTIVE")))
                .andExpect(jsonPath("$.participants", hasSize(2)))
                .andReturn();

        long matchId = objectMapper.readTree(matchResult.getResponse().getContentAsString()).get("id").asLong();

        // User A completes the match
        mockMvc.perform(put("/api/matches/" + matchId + "/complete")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());

        // Verify active match is gone
        mockMvc.perform(get("/api/matches/active")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());

        // Verify match history
        mockMvc.perform(get("/api/matches/history")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is((int) matchId)))
                .andExpect(jsonPath("$[0].status", is("COMPLETED")));
    }
}
