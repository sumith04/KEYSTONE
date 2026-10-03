package com.keystone.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DevEmailServiceImpl implements EmailService {

    @Override
    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        log.info("[DEV EMAIL SERVICE] Simulated Password Reset Request:");
        log.info("[DEV EMAIL SERVICE] Recipient: {}", toEmail);
        log.info("[DEV EMAIL SERVICE] Reset Token: {}", resetToken);
        log.info("[DEV EMAIL SERVICE] Safe Reset Link Placeholder: http://localhost:5173/reset-password?token={}", resetToken);
    }
}
