package com.expensetracker.backend;

import com.expensetracker.backend.model.Expense;
import com.expensetracker.backend.model.User;
import com.expensetracker.backend.repository.ExpenseRepository;
import com.expensetracker.backend.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class ExpenseTrackerBackendApplicationTests {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        expenseRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void contextLoads() {
        assertNotNull(userRepository);
        assertNotNull(expenseRepository);
    }

    @Test
    void testUserRegistrationAndLoginFlow() throws Exception {
        User user = new User();
        user.setFullName("John Doe");
        user.setUsername("johndoe");
        user.setEmail("john@example.com");
        user.setPassword("secret123");

        // Register user
        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.username", is("johndoe")))
                .andExpect(jsonPath("$.email", is("john@example.com")));

        assertTrue(userRepository.existsByUsername("johndoe"));

        // Login with correct credentials
        User loginReq = new User();
        loginReq.setUsername("johndoe");
        loginReq.setPassword("secret123");

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("johndoe")));

        // Login with invalid password
        loginReq.setPassword("wrongpassword");
        assertThrows(Exception.class, () ->
                mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
        );
    }

    @Test
    void testDuplicateRegistrationFails() throws Exception {
        User user = new User();
        user.setFullName("User One");
        user.setUsername("user1");
        user.setEmail("user1@example.com");
        user.setPassword("password");
        userRepository.save(user);

        // Attempt duplicate username
        User duplicateUser = new User();
        duplicateUser.setFullName("User Two");
        duplicateUser.setUsername("user1");
        duplicateUser.setEmail("user2@example.com");
        duplicateUser.setPassword("password");

        assertThrows(Exception.class, () ->
                mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateUser)))
        );
    }

    @Test
    void testExpenseCrudAndUserIsolation() throws Exception {
        // Create User 1
        User user1 = new User();
        user1.setFullName("Alice");
        user1.setUsername("alice");
        user1.setEmail("alice@example.com");
        user1.setPassword("pass123");
        user1 = userRepository.save(user1);

        // Create User 2
        User user2 = new User();
        user2.setFullName("Bob");
        user2.setUsername("bob");
        user2.setEmail("bob@example.com");
        user2.setPassword("pass123");
        user2 = userRepository.save(user2);

        // Add Expense for Alice
        Expense expense = new Expense();
        expense.setTitle("Groceries");
        expense.setAmount(150.50);
        expense.setCategory("Food");
        expense.setDate(LocalDate.of(2026, 10, 2));

        String responseJson = mockMvc.perform(post("/api/expenses")
                        .param("username", "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(expense)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.title", is("Groceries")))
                .andExpect(jsonPath("$.amount", is(150.50)))
                .andExpect(jsonPath("$.date", is("2026-10-02")))
                .andReturn().getResponse().getContentAsString();

        Expense createdExpense = objectMapper.readValue(responseJson, Expense.class);
        Long expenseId = createdExpense.getId();

        // Get Expenses for Alice
        mockMvc.perform(get("/api/expenses")
                        .param("username", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("Groceries")));

        // Get Expenses for Bob (should be empty - isolation)
        mockMvc.perform(get("/api/expenses")
                        .param("username", "bob"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // Attempt Update by Bob (unauthorized user)
        Expense updateReq = new Expense();
        updateReq.setTitle("Hacked Title");
        updateReq.setAmount(999.00);
        updateReq.setCategory("Hacked");
        updateReq.setDate(LocalDate.of(2026, 10, 2));

        assertThrows(Exception.class, () ->
                mockMvc.perform(put("/api/expenses/" + expenseId)
                        .param("username", "bob")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
        );

        // Successful Update by Alice (owner)
        updateReq.setTitle("Supermarket Groceries");
        mockMvc.perform(put("/api/expenses/" + expenseId)
                        .param("username", "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Supermarket Groceries")));

        // Attempt Delete by Bob
        mockMvc.perform(delete("/api/expenses/" + expenseId)
                        .param("username", "bob"))
                .andExpect(status().isOk())
                .andExpect(content().string("You cannot delete another user's expense"));

        // Delete by Alice
        mockMvc.perform(delete("/api/expenses/" + expenseId)
                        .param("username", "alice"))
                .andExpect(status().isOk())
                .andExpect(content().string("Expense deleted successfully"));

        // Verify empty list after deletion
        mockMvc.perform(get("/api/expenses")
                        .param("username", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
