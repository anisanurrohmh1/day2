package com.bankmega.authservice.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.bankmega.authservice.entity.AuditLog;
import com.bankmega.authservice.repository.AuditLogRepository;
import com.bankmega.authservice.repository.UserRepository;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @Async
    public void logAction(String action, String description) {
        try {
            log.debug("Logging action: {} - {}", action, description);

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = null;

            if (authentication != null && authentication.isAuthenticated()
                    && !"anonymousUser".equals(authentication.getPrincipal())) {
                username = authentication.getName();
            }

            String ipAddress = null;
            String userAgent = null;
            try {
                ServletRequestAttributes attributes =
                        (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attributes != null) {
                    HttpServletRequest request = attributes.getRequest();
                    ipAddress = extractIpAddress(request);
                    userAgent = request.getHeader("User-Agent");
                }
            } catch (Exception e) {
                log.debug("Could not extract IP address from request context: {}", e.getMessage());
            }

            AuditLog.AuditLogBuilder auditLogBuilder = AuditLog.builder()
                    .action(action)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .success(true);

            if (username != null) {
                userRepository.findByUsername(username).ifPresent(auditLogBuilder::user);
            }

            AuditLog auditLog = auditLogBuilder.build();
            auditLogRepository.save(auditLog);

            log.debug("Successfully logged action: {} for user: {}", action, username);

        } catch (Exception e) {
            log.error("Failed to log action: {} - {}", action, description, e);
        }
    }

    private String extractIpAddress(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");

        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("X-Real-IP");
        }

        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }

        if (ipAddress != null && ipAddress.contains(",")) {
            ipAddress = ipAddress.split(",")[0].trim();
        }

        return ipAddress;
    }
}
