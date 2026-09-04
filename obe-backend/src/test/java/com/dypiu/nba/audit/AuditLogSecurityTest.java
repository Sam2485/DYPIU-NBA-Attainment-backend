package com.dypiu.nba.audit;

import com.dypiu.nba.dto.ApiResponse;
import com.dypiu.nba.entity.AuditLog;
import com.dypiu.nba.entity.User;
import com.dypiu.nba.entity.UserRole;
import com.dypiu.nba.repository.AuditLogRepository;
import com.dypiu.nba.repository.UserRepository;
import com.dypiu.nba.security.JwtTokenProvider;
import com.dypiu.nba.service.AuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class AuditLogSecurityTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String iqacToken;
    private String directorToken;
    private String hodToken;
    private String pcToken;
    private String ccToken;
    private String facultyToken;

    private Long sampleAuditLogId;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();
        userRepository.deleteAll();

        User iqac = createUser("iqac.sec@dypiu.ac.in", "iqac_sec", UserRole.IQAC);
        User director = createUser("director.sec@dypiu.ac.in", "dir_sec", UserRole.DIRECTOR);
        User hod = createUser("hod.sec@dypiu.ac.in", "hod_sec", UserRole.HOD);
        User pc = createUser("pc.sec@dypiu.ac.in", "pc_sec", UserRole.PROGRAMME_COORDINATOR);
        User faculty = createUser("faculty.sec@dypiu.ac.in", "faculty_sec", UserRole.FACULTY);

        iqacToken = generateToken(iqac);
        directorToken = generateToken(director);
        hodToken = generateToken(hod);
        pcToken = generateToken(pc);
        facultyToken = generateToken(faculty);

        AuditLog log = auditLogService.recordSuccess(
                AuditAction.CREATE, ResourceType.SCHOOL, "sch-100", null, "ACTIVE", "Created School", Map.of("code", "SOE")
        );
        sampleAuditLogId = log.getId();
    }

    private User createUser(String email, String username, UserRole role) {
        return userRepository.save(User.builder()
                .email(email)
                .username(username)
                .name(username)
                .passwordHash("secret")
                .role(role)
                .isActive(true)
                .build());
    }

    private String generateToken(User user) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                user.getEmail(), null, List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
        return jwtTokenProvider.generateToken(auth);
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return headers;
    }

    @Test
    void testUnauthenticatedAccessReturns401() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/audit-logs", HttpMethod.GET, new HttpEntity<>(authHeaders(null)), String.class
        );
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void testNonIqacRolesReturn403() {
        for (String token : List.of(directorToken, hodToken, pcToken, facultyToken)) {
            ResponseEntity<String> response = restTemplate.exchange(
                    "/audit-logs", HttpMethod.GET, new HttpEntity<>(authHeaders(token)), String.class
            );
            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode(), "Expected 403 Forbidden for non-IQAC user");
        }
    }

    @Test
    void testIqacAccessAllowed() {
        ResponseEntity<ApiResponse> response = restTemplate.exchange(
                "/audit-logs", HttpMethod.GET, new HttpEntity<>(authHeaders(iqacToken)), ApiResponse.class
        );
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
    }

    @Test
    void testAuditLogByIdRestrictedToIqac() {
        // Director forbidden
        ResponseEntity<String> dirRes = restTemplate.exchange(
                "/audit-logs/" + sampleAuditLogId, HttpMethod.GET, new HttpEntity<>(authHeaders(directorToken)), String.class
        );
        assertEquals(HttpStatus.FORBIDDEN, dirRes.getStatusCode());

        // IQAC allowed
        ResponseEntity<ApiResponse> iqacRes = restTemplate.exchange(
                "/audit-logs/" + sampleAuditLogId, HttpMethod.GET, new HttpEntity<>(authHeaders(iqacToken)), ApiResponse.class
        );
        assertEquals(HttpStatus.OK, iqacRes.getStatusCode());
        assertTrue(iqacRes.getBody().isSuccess());
    }

    @Test
    void testAuditLogImmutabilityNoMutationEndpointsExist() {
        // Test PUT on audit log
        ResponseEntity<String> putRes = restTemplate.exchange(
                "/audit-logs/" + sampleAuditLogId, HttpMethod.PUT, new HttpEntity<>(Map.of("remarks", "tampered"), authHeaders(iqacToken)), String.class
        );
        assertTrue(putRes.getStatusCode() == HttpStatus.METHOD_NOT_ALLOWED || putRes.getStatusCode() == HttpStatus.NOT_FOUND || putRes.getStatusCode() == HttpStatus.FORBIDDEN);

        // Test DELETE on audit log
        ResponseEntity<String> delRes = restTemplate.exchange(
                "/audit-logs/" + sampleAuditLogId, HttpMethod.DELETE, new HttpEntity<>(authHeaders(iqacToken)), String.class
        );
        assertTrue(delRes.getStatusCode() == HttpStatus.METHOD_NOT_ALLOWED || delRes.getStatusCode() == HttpStatus.NOT_FOUND || delRes.getStatusCode() == HttpStatus.FORBIDDEN);
    }
}
