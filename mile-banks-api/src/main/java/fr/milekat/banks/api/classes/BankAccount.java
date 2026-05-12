package fr.milekat.banks.api.classes;

import java.util.Map;

public record BankAccount(Map<String, Object> tags, int balance) {}
