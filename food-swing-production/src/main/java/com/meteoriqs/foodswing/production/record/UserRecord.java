package com.meteoriqs.foodswing.production.record;

public record UserRecord (
        String username,
        String firstName,
        String lastName,
        boolean active
        ) {}
