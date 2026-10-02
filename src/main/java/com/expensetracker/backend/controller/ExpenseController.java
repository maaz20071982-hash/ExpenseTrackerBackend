package com.expensetracker.backend.controller;

import com.expensetracker.backend.model.Expense;
import com.expensetracker.backend.model.User;
import com.expensetracker.backend.repository.ExpenseRepository;
import com.expensetracker.backend.repository.UserRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;

    public ExpenseController(
            ExpenseRepository expenseRepository,
            UserRepository userRepository) {

        this.expenseRepository = expenseRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<Expense> getUserExpenses(
            @RequestParam String username) {

        User user = userRepository
                .findByUsername(username)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        return expenseRepository.findByUser(user);
    }

    @PostMapping
    public Expense addExpense(
            @RequestParam String username,
            @RequestBody Expense expense) {

        User user = userRepository
                .findByUsername(username)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        expense.setUser(user);

        return expenseRepository.save(expense);
    }

    @PutMapping("/{id}")
    public Expense updateExpense(
            @PathVariable Long id,
            @RequestParam String username,
            @RequestBody Expense updatedExpense) {

        User user = userRepository
                .findByUsername(username)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        Expense expense = expenseRepository
                .findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Expense not found"));

        if (!expense.getUser().getId().equals(user.getId())) {
            throw new RuntimeException(
                    "You cannot edit another user's expense");
        }

        expense.setTitle(updatedExpense.getTitle());
        expense.setAmount(updatedExpense.getAmount());
        expense.setCategory(updatedExpense.getCategory());
        expense.setDate(updatedExpense.getDate());

        return expenseRepository.save(expense);
    }

    @DeleteMapping("/{id}")
    public String deleteExpense(
            @PathVariable Long id,
            @RequestParam String username) {

        User user = userRepository
                .findByUsername(username)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        Expense expense = expenseRepository
                .findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Expense not found"));

        if (!expense.getUser().getId().equals(user.getId())) {
            return "You cannot delete another user's expense";
        }

        expenseRepository.deleteById(id);

        return "Expense deleted successfully";
    }
}