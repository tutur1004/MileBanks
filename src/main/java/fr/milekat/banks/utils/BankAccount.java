package fr.milekat.banks.utils;

import java.util.Map;

public record BankAccount(Map<String, Object> tags, int balance) {}
