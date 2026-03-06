package com.workproofpay.backend.shared.security;

public record AuthenticatedUser(Long userId, String email, String role) {
}
