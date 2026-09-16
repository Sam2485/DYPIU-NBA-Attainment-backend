package com.dypiu.nba.emmu;

import com.dypiu.nba.emmu.resolver.EntityMatchingUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AcademicEntityResolverTest {

    @Test
    @DisplayName("Normalization: Removes punctuation, normalizes whitespace and lowercase")
    void testNormalize() {
        assertEquals("computer engineering", EntityMatchingUtils.normalize("Computer Engineering"));
        assertEquals("computer engineering", EntityMatchingUtils.normalize("COMPUTER ENGINEERING"));
        assertEquals("computer engineering", EntityMatchingUtils.normalize("Computer - Engineering"));
        assertEquals("database management systems dbms", EntityMatchingUtils.normalize("Database Management Systems (DBMS)"));
        assertEquals("operating systems lab", EntityMatchingUtils.normalize("Operating   Systems   Lab!"));
    }

    @Test
    @DisplayName("NormalizeCode: Standardizes code representations")
    void testNormalizeCode() {
        assertEquals("PO3", EntityMatchingUtils.normalizeCode("PO-3"));
        assertEquals("PO3", EntityMatchingUtils.normalizeCode("PO 3"));
        assertEquals("CO2", EntityMatchingUtils.normalizeCode("co_2"));
        assertEquals("CS201", EntityMatchingUtils.normalizeCode("cs 201"));
        assertEquals("PSO1", EntityMatchingUtils.normalizeCode("PSO-1"));
    }

    @Test
    @DisplayName("Integer Extraction: Extracts numbers correctly from queries")
    void testExtractInteger() {
        assertEquals(Optional.of(5), EntityMatchingUtils.extractInteger("Semester 5"));
        assertEquals(Optional.of(3), EntityMatchingUtils.extractInteger("Sem 3"));
        assertEquals(Optional.of(2025), EntityMatchingUtils.extractInteger("Batch 2025"));
        assertEquals(Optional.empty(), EntityMatchingUtils.extractInteger("Computer Engineering"));
    }

    @Test
    @DisplayName("Acronym Generation: Dynamically generates academic abbreviations")
    void testGenerateAcronyms() {
        Set<String> ceAcronyms = EntityMatchingUtils.generateAcronyms("Computer Engineering");
        assertTrue(ceAcronyms.contains("CE") || ceAcronyms.contains("CSE"));

        Set<String> dbmsAcronyms = EntityMatchingUtils.generateAcronyms("Database Management Systems");
        assertTrue(dbmsAcronyms.contains("DBMS"));

        Set<String> osAcronyms = EntityMatchingUtils.generateAcronyms("Operating Systems");
        assertTrue(osAcronyms.contains("OS"));

        Set<String> soetAcronyms = EntityMatchingUtils.generateAcronyms("School of Engineering and Technology");
        assertTrue(soetAcronyms.contains("SOET") || soetAcronyms.contains("SOE"));
    }

    @Test
    @DisplayName("Fuzzy Matching: Levenshtein distance & similarity handles typos accurately")
    void testLevenshteinAndSimilarity() {
        // Exact
        assertEquals(1.0, EntityMatchingUtils.similarity("Operating Systems", "Operating Systems"));

        // Typos
        double sim1 = EntityMatchingUtils.similarity("Operting Systems", "Operating Systems");
        assertTrue(sim1 >= 0.85, "Expected similarity >= 0.85 for single character omission");

        double sim2 = EntityMatchingUtils.similarity("Computar Engineering", "Computer Engineering");
        assertTrue(sim2 >= 0.85, "Expected similarity >= 0.85 for typo");

        // Unrelated
        double sim3 = EntityMatchingUtils.similarity("Quantum Physics", "Database Systems");
        assertTrue(sim3 < 0.40, "Expected low similarity for unrelated strings");
    }

    @Test
    @DisplayName("Token Similarity: Handles word reordering")
    void testTokenSimilarity() {
        double sim = EntityMatchingUtils.tokenSimilarity("Systems Operating", "Operating Systems");
        assertEquals(1.0, sim);
    }
}
