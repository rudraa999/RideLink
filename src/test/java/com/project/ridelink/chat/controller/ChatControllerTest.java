package com.project.ridelink.chat.controller;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.project.ridelink.auth.dto.LoginRequest;
import com.project.ridelink.auth.dto.RegisterRequest;
import com.project.ridelink.auth.repository.RefreshTokenRepository;
import com.project.ridelink.availability.dto.AvailabilityRequest;
import com.project.ridelink.availability.repository.RideAvailabilityRepository;
import com.project.ridelink.chat.dto.ChatMessageRequest;
import com.project.ridelink.chat.repository.ChatMessageRepository;
import com.project.ridelink.chat.service.ChatService;
import com.project.ridelink.college.entity.College;
import com.project.ridelink.college.repository.CollegeRepository;
import com.project.ridelink.ridematch.entity.RideMatch;
import com.project.ridelink.ridematch.repository.RideMatchMemberRepository;
import com.project.ridelink.ridematch.repository.RideMatchRepository;
import com.project.ridelink.riderequest.repository.RideRequestRepository;
import com.project.ridelink.riderequest.dto.RideRequestResponse;
import com.project.ridelink.riderequest.service.RideRequestService;
import com.project.ridelink.user.entity.User;
import com.project.ridelink.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.security.Principal;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ChatControllerTest {

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
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private RideRequestService rideRequestService;

    @Autowired
    private ChatService chatService;

    @Autowired
    private ChatController chatController;

    @MockitoBean
    private SimpMessagingTemplate messagingTemplate;

    private College testCollege;
    private User userA;
    private User userB;
    private User userC; // Unauthorized user
    private String tokenA;
    private String tokenB;
    private String tokenC;
    private long matchId;

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

        // Register/Login Users
        tokenA = registerAndLogin("usera", "usera@mitwpu.edu.in");
        tokenB = registerAndLogin("userb", "userb@mitwpu.edu.in");
        tokenC = registerAndLogin("userc", "userc@mitwpu.edu.in");

        userA = userRepository.findByUsername("usera").orElseThrow();
        userB = userRepository.findByUsername("userb").orElseThrow();
        userC = userRepository.findByUsername("userc").orElseThrow();

        // Set active availabilities
        activateAvailability(tokenA);
        activateAvailability(tokenB);

        // Send & Accept Request to create a Match
        RideRequestResponse reqResponse = rideRequestService.sendRequest(userA, userB.getId());
        rideRequestService.acceptRequest(userB, reqResponse.getId());

        // Get the active match ID
        RideMatch activeMatch = rideMatchRepository.findActiveMatchForUser(userA, com.project.ridelink.ridematch.entity.MatchStatus.ACTIVE).orElseThrow();
        matchId = activeMatch.getId();
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

    private void activateAvailability(String token) throws Exception {
        AvailabilityRequest request = AvailabilityRequest.builder()
                .destination("Karve Nagar")
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
    void testGetChatHistorySuccess() throws Exception {
        // Save some messages via service
        chatService.saveMessage(userA, matchId, "Hello B, I am at the gate.");
        chatService.saveMessage(userB, matchId, "Hey A, coming in 2 mins.");

        // Get history as User A -> Should return both messages
        mockMvc.perform(get("/api/chats/" + matchId + "/messages")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].senderUsername", is("usera")))
                .andExpect(jsonPath("$[0].content", is("Hello B, I am at the gate.")))
                .andExpect(jsonPath("$[1].senderUsername", is("userb")))
                .andExpect(jsonPath("$[1].content", is("Hey A, coming in 2 mins.")));
    }

    @Test
    void testGetChatHistoryForbidden() throws Exception {
        // User C (not participant) tries to view history -> Should fail with 403 Forbidden
        mockMvc.perform(get("/api/chats/" + matchId + "/messages")
                        .header("Authorization", "Bearer " + tokenC))
                .andExpect(status().isForbidden());
    }

    @Test
    void testWebSocketMessageMappingSendsToBroker() {
        ChatMessageRequest request = ChatMessageRequest.builder()
                .content("Stomp test message")
                .build();

        Principal mockPrincipal = Mockito.mock(Principal.class);
        Mockito.when(mockPrincipal.getName()).thenReturn("usera");

        chatController.handleChatMessage(matchId, request, mockPrincipal);

        // Verify message is saved and broadcast
        verify(messagingTemplate).convertAndSend(
                eq("/topic/match/" + matchId + "/chat"),
                any(com.project.ridelink.chat.dto.ChatMessageResponse.class)
        );
    }
}
