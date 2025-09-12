package com.example.passman.service;

public interface BreachCheckService {
    boolean isEnabled();
    boolean isCompromised(String password);
}
