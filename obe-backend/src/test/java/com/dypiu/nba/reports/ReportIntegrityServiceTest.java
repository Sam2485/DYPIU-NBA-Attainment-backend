package com.dypiu.nba.reports;

import com.dypiu.nba.reports.integrity.ReportIntegrityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class ReportIntegrityServiceTest {

    private ReportIntegrityService integrityService;
    private final String secret = "test_super_secure_secret_key_2026";

    @BeforeEach
    void setUp() {
        integrityService = new ReportIntegrityService(secret);
    }

    @Test
    @DisplayName("Identical byte arrays produce identical SHA-256 and HMAC signatures")
    void testIdenticalBytesDeterministic() {
        byte[] originalBytes = "DYPIU NBA Attainment Master Report Content 2026".getBytes(StandardCharsets.UTF_8);
        byte[] copyBytes = "DYPIU NBA Attainment Master Report Content 2026".getBytes(StandardCharsets.UTF_8);

        String sha1 = integrityService.calculateSha256(originalBytes);
        String sha2 = integrityService.calculateSha256(copyBytes);
        assertEquals(sha1, sha2);
        assertNotNull(sha1);
        assertEquals(64, sha1.length());

        String hmac1 = integrityService.calculateHmac(originalBytes);
        String hmac2 = integrityService.calculateHmac(copyBytes);
        assertEquals(hmac1, hmac2);
        assertNotNull(hmac1);
        assertEquals(64, hmac1.length());

        ReportIntegrityService.VerificationResult result = integrityService.verifyArtifact(copyBytes, sha1, hmac1);
        assertEquals(ReportIntegrityService.VerificationResult.VALID, result);
    }

    @Test
    @DisplayName("Tampered file content fails SHA-256 verification")
    void testTamperedContentFails() {
        byte[] validBytes = "Original Authentic PDF Stream Bytes".getBytes(StandardCharsets.UTF_8);
        byte[] tamperedBytes = "Tampered/Modified PDF Stream Bytes".getBytes(StandardCharsets.UTF_8);

        String validSha = integrityService.calculateSha256(validBytes);
        String validHmac = integrityService.calculateHmac(validBytes);

        ReportIntegrityService.VerificationResult result = integrityService.verifyArtifact(tamperedBytes, validSha, validHmac);
        assertEquals(ReportIntegrityService.VerificationResult.HASH_MISMATCH, result);
    }

    @Test
    @DisplayName("Tampered HMAC signature fails HMAC verification")
    void testInvalidHmacFails() {
        byte[] validBytes = "Original Authentic Excel Stream Bytes".getBytes(StandardCharsets.UTF_8);
        String validSha = integrityService.calculateSha256(validBytes);
        String invalidHmac = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

        ReportIntegrityService.VerificationResult result = integrityService.verifyArtifact(validBytes, validSha, invalidHmac);
        assertEquals(ReportIntegrityService.VerificationResult.HMAC_MISMATCH, result);
    }

    @Test
    @DisplayName("Renamed file remains cryptographically valid because verification relies on byte content")
    void testRenamedFileRemainsValid() {
        byte[] fileBytes = "Standard Master Attainment Workbook binary stream".getBytes(StandardCharsets.UTF_8);
        String sha = integrityService.calculateSha256(fileBytes);
        String hmac = integrityService.calculateHmac(fileBytes);

        // Renamed file loaded with exact same bytes
        byte[] renamedFileBytes = fileBytes.clone();
        ReportIntegrityService.VerificationResult result = integrityService.verifyArtifact(renamedFileBytes, sha, hmac);
        assertEquals(ReportIntegrityService.VerificationResult.VALID, result);
    }
}
