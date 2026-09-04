package com.dypiu.nba.reports.integrity;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
@Slf4j
public class ReportIntegrityService {

    private static final String HMAC_SHA256_ALGORITHM = "HmacSHA256";
    private final String hmacSecret;

    public ReportIntegrityService(
            @Value("${app.reports.integrity.hmac-secret:dypiu_nba_reports_cryptographic_hmac_secret_key_2026_production}")
            String hmacSecret) {
        this.hmacSecret = hmacSecret;
    }

    public String calculateSha256(byte[] content) {
        if (content == null) {
            throw new IllegalArgumentException("Content cannot be null for SHA-256 calculation.");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available in environment", e);
        }
    }

    public String calculateHmac(byte[] content) {
        return calculateHmac(content, this.hmacSecret);
    }

    public String calculateHmac(byte[] content, String secret) {
        if (content == null || secret == null) {
            throw new IllegalArgumentException("Content and secret cannot be null for HMAC calculation.");
        }
        try {
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256_ALGORITHM);
            Mac mac = Mac.getInstance(HMAC_SHA256_ALGORITHM);
            mac.init(secretKey);
            byte[] hmac = mac.doFinal(content);
            return HexFormat.of().formatHex(hmac);
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate HMAC-SHA256 signature", e);
        }
    }

    public VerificationResult verifyArtifact(byte[] content, String expectedSha256, String expectedHmac) {
        if (content == null || expectedSha256 == null || expectedHmac == null) {
            return VerificationResult.INVALID_INPUT;
        }

        String calculatedSha256 = calculateSha256(content);
        if (!calculatedSha256.equalsIgnoreCase(expectedSha256)) {
            log.warn("Artifact SHA-256 mismatch! Expected: {}, Calculated: {}", expectedSha256, calculatedSha256);
            return VerificationResult.HASH_MISMATCH;
        }

        String calculatedHmac = calculateHmac(content);
        if (!calculatedHmac.equalsIgnoreCase(expectedHmac)) {
            log.warn("Artifact HMAC mismatch! Expected: {}, Calculated: {}", expectedHmac, calculatedHmac);
            return VerificationResult.HMAC_MISMATCH;
        }

        return VerificationResult.VALID;
    }

    public enum VerificationResult {
        VALID,
        HASH_MISMATCH,
        HMAC_MISMATCH,
        INVALID_INPUT
    }
}
