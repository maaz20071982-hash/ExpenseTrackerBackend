package com.expensetracker.backend.repository;

import com.expensetracker.backend.model.Expense;
import com.expensetracker.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findByUser(User user);
}