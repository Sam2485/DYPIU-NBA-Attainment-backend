package com.dypiu.nba.service;

import com.dypiu.nba.audit.AuditAction;
import com.dypiu.nba.audit.ResourceType;
import com.dypiu.nba.dto.AuditLogPageResponseDto;
import com.dypiu.nba.dto.AuditLogResponseDto;
import com.dypiu.nba.entity.AuditLog;
import com.dypiu.nba.entity.User;
import com.dypiu.nba.repository.AuditLogRepository;
import com.dypiu.nba.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.ZonedDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "password", "passwordhash", "currentpassword", "newpassword", "confirmpassword",
            "token", "accesstoken", "refreshtoken", "authorization", "secret", "privatekey"
    );

    @Transactional(propagation = Propagation.REQUIRED)
    public AuditLog record(AuditAction action, ResourceType resourceType, String resourceId,
                           String oldStatus, String newStatus, String remarks,
                           Object metadata, boolean success) {
        ActorContext actor = resolveActorContext();
        RequestContext requestContext = extractRequestContext();
        String sanitizedMetadata = sanitizeMetadata(metadata);
        String sanitizedRemarks = sanitizeString(remarks);

        AuditLog logEntry = AuditLog.builder()
                .actorId(actor.actorId())
                .actorRole(actor.actorRole())
                .actorName(actor.actorName())
                .actorEmail(actor.actorEmail())
                .action(action)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .remarks(sanitizedRemarks)
                .metadata(sanitizedMetadata)
                .success(success)
                .ipAddress(requestContext.ipAddress())
                .userAgent(requestContext.userAgent())
                .createdAt(ZonedDateTime.now())
                .build();

        return auditLogRepository.save(logEntry);
    }

    public AuditLog recordSuccess(AuditAction action, ResourceType resourceType, String resourceId,
                                  String oldStatus, String newStatus, String remarks, Object metadata) {
        return record(action, resourceType, resourceId, oldStatus, newStatus, remarks, metadata, true);
    }

    public AuditLog recordFailure(AuditAction action, ResourceType resourceType, String resourceId,
                                  String oldStatus, String newStatus, String remarks, Object metadata) {
        return record(action, resourceType, resourceId, oldStatus, newStatus, remarks, metadata, false);
    }

    @Transactional(readOnly = true)
    public AuditLogPageResponseDto getAuditLogs(String actorId, String actorRole, AuditAction action,
                                                ResourceType resourceType, String resourceId,
                                                Boolean success, ZonedDateTime from, ZonedDateTime to,
                                                int page, int size) {
        int pageNum = Math.max(0, page);
        int pageSize = Math.min(Math.max(1, size), 100);
        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<AuditLog> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            if (actorId != null && !actorId.isBlank()) {
                predicates.add(cb.equal(root.get("actorId"), actorId.trim()));
            }
            if (actorRole != null && !actorRole.isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("actorRole")), actorRole.trim().toUpperCase()));
            }
            if (action != null) {
                predicates.add(cb.equal(root.get("action"), action));
            }
            if (resourceType != null) {
                predicates.add(cb.equal(root.get("resourceType"), resourceType));
            }
            if (resourceId != null && !resourceId.isBlank()) {
                predicates.add(cb.equal(root.get("resourceId"), resourceId.trim()));
            }
            if (success != null) {
                predicates.add(cb.equal(root.get("success"), success));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Page<AuditLog> result = auditLogRepository.findAll(spec, pageable);
        List<AuditLogResponseDto> dtoList = result.getContent().stream().map(this::toDto).toList();

        return AuditLogPageResponseDto.builder()
                .content(dtoList)
                .pageNumber(result.getNumber())
                .pageSize(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .isLast(result.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public AuditLogResponseDto getAuditLogById(Long id) {
        return auditLogRepository.findById(id)
                .map(this::toDto)
                .orElse(null);
    }

    public AuditLogResponseDto toDto(AuditLog log) {
        if (log == null) return null;
        return AuditLogResponseDto.builder()
                .id(log.getId())
                .actorId(log.getActorId())
                .actorRole(log.getActorRole())
                .actorName(log.getActorName())
                .actorEmail(log.getActorEmail())
                .action(log.getAction())
                .resourceType(log.getResourceType())
                .resourceId(log.getResourceId())
                .oldStatus(log.getOldStatus())
                .newStatus(log.getNewStatus())
                .remarks(log.getRemarks())
                .metadata(log.getMetadata())
                .success(log.isSuccess())
                .ipAddress(log.getIpAddress())
                .userAgent(log.getUserAgent())
                .createdAt(log.getCreatedAt())
                .build();
    }

    private ActorContext resolveActorContext() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            String usernameOrEmail = auth.getName();
            String role = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .map(a -> a.startsWith("ROLE_") ? a.substring(5) : a)
                    .findFirst().orElse("ANONYMOUS");

            Optional<User> userOpt = userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(usernameOrEmail, usernameOrEmail);
            if (userOpt.isPresent()) {
                User u = userOpt.get();
                return new ActorContext(String.valueOf(u.getId()), u.getRole() != null ? u.getRole().name() : role, u.getName(), u.getEmail());
            }
            return new ActorContext(usernameOrEmail, role, usernameOrEmail, usernameOrEmail);
        }
        return new ActorContext("SYSTEM", "SYSTEM", "System Process", "system@dypiu.ac.in");
    }

    private RequestContext extractRequestContext() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null && attributes.getRequest() != null) {
                HttpServletRequest request = attributes.getRequest();
                String ip = request.getHeader("X-Forwarded-For");
                if (ip == null || ip.isBlank()) {
                    ip = request.getRemoteAddr();
                } else if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                String ua = request.getHeader("User-Agent");
                if (ua != null && ua.length() > 500) {
                    ua = ua.substring(0, 500);
                }
                return new RequestContext(ip, ua);
            }
        } catch (Exception ignored) {}
        return new RequestContext(null, null);
    }

    private String sanitizeMetadata(Object metadata) {
        if (metadata == null) return null;
        try {
            if (metadata instanceof String str) {
                if (str.trim().startsWith("{") || str.trim().startsWith("[")) {
                    Map<?, ?> map = objectMapper.readValue(str, Map.class);
                    return objectMapper.writeValueAsString(sanitizeMap(map));
                }
                return sanitizeString(str);
            } else if (metadata instanceof Map<?, ?> map) {
                return objectMapper.writeValueAsString(sanitizeMap(map));
            } else {
                Map<?, ?> map = objectMapper.convertValue(metadata, Map.class);
                return objectMapper.writeValueAsString(sanitizeMap(map));
            }
        } catch (Exception e) {
            return sanitizeString(String.valueOf(metadata));
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> sanitizeMap(Map<?, ?> input) {
        Map<String, Object> sanitized = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : input.entrySet()) {
            String key = String.valueOf(entry.getKey());
            Object val = entry.getValue();
            if (SENSITIVE_KEYS.contains(key.toLowerCase().replaceAll("[^a-z]", ""))) {
                sanitized.put(key, "[REDACTED]");
            } else if (val instanceof Map<?, ?> nestedMap) {
                sanitized.put(key, sanitizeMap(nestedMap));
            } else if (val instanceof List<?> list) {
                List<Object> sanitizedList = new ArrayList<>();
                for (Object item : list) {
                    if (item instanceof Map<?, ?> itemMap) {
                        sanitizedList.add(sanitizeMap(itemMap));
                    } else {
                        sanitizedList.add(item);
                    }
                }
                sanitized.put(key, sanitizedList);
            } else {
                sanitized.put(key, val);
            }
        }
        return sanitized;
    }

    private String sanitizeString(String str) {
        if (str == null) return null;
        String lower = str.toLowerCase();
        for (String sensitive : SENSITIVE_KEYS) {
            if (lower.contains(sensitive)) {
                return "[REDACTED]";
            }
        }
        return str;
    }

    private record ActorContext(String actorId, String actorRole, String actorName, String actorEmail) {}
    private record RequestContext(String ipAddress, String userAgent) {}
}
