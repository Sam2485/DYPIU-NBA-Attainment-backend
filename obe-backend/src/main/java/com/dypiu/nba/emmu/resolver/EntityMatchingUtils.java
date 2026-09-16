package com.dypiu.nba.emmu.resolver;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * High-performance, dependency-free text normalization and matching utilities for academic entity resolution.
 */
public final class EntityMatchingUtils {

    private static final Pattern PUNCTUATION_PATTERN = Pattern.compile("[\\p{Punct}&&[^\\-_]]+");
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d+");

    private EntityMatchingUtils() {}

    /**
     * Normalizes text by lowercasing, stripping punctuation, and collapsing whitespaces.
     */
    public static String normalize(String input) {
        if (input == null) return "";
        String cleaned = PUNCTUATION_PATTERN.matcher(input).replaceAll(" ");
        cleaned = cleaned.replace('-', ' ').replace('_', ' ').replace('/', ' ');
        cleaned = WHITESPACE_PATTERN.matcher(cleaned).replaceAll(" ").trim();
        return cleaned.toLowerCase(Locale.ROOT);
    }

    /**
     * Normalizes code strings (e.g. 'PO-3' -> 'PO3', 'CO 2' -> 'CO2', 'CS 201' -> 'CS201').
     */
    public static String normalizeCode(String input) {
        if (input == null) return "";
        return input.toUpperCase(Locale.ROOT).replaceAll("[\\s\\-_/.]+", "");
    }

    /**
     * Extracts all integer digits from a string (e.g. 'Semester 5' -> 5, 'Batch 2025' -> 2025).
     */
    public static Optional<Integer> extractInteger(String input) {
        if (input == null || input.isBlank()) return Optional.empty();
        Matcher matcher = NUMBER_PATTERN.matcher(input);
        if (matcher.find()) {
            try {
                return Optional.of(Integer.parseInt(matcher.group()));
            } catch (NumberFormatException ignored) {}
        }
        return Optional.empty();
    }

    /**
     * Generates standard academic acronyms and initials from a full title.
     * e.g. 'Computer Engineering' -> ['CE', 'CSE']
     * e.g. 'Data Structures and Algorithms' -> ['DSA', 'DS']
     * e.g. 'Database Management Systems' -> ['DBMS', 'DMS']
     */
    public static Set<String> generateAcronyms(String name) {
        if (name == null || name.isBlank()) return Collections.emptySet();
        Set<String> acronyms = new HashSet<>();

        String[] stopWords = {"and", "of", "the", "in", "for", "to", "with", "on", "at", "a", "an", "lab", "laboratory"};
        Set<String> stops = new HashSet<>(Arrays.asList(stopWords));

        String[] words = name.trim().split("[\\s\\-_/]+");
        StringBuilder allInitials = new StringBuilder();
        StringBuilder nonStopInitials = new StringBuilder();

        for (String w : words) {
            String clean = w.replaceAll("[^a-zA-Z0-9]", "");
            if (!clean.isEmpty()) {
                char initial = Character.toUpperCase(clean.charAt(0));
                allInitials.append(initial);
                if (!stops.contains(clean.toLowerCase(Locale.ROOT))) {
                    nonStopInitials.append(initial);
                }
            }
        }

        if (allInitials.length() >= 2) {
            acronyms.add(allInitials.toString());
        }
        if (nonStopInitials.length() >= 2) {
            acronyms.add(nonStopInitials.toString());
        }

        // Domain specific common abbreviations
        String norm = normalize(name);
        if (norm.contains("computer science") || norm.contains("computer engineering")) {
            acronyms.add("CSE");
            acronyms.add("CE");
            acronyms.add("CS");
        }
        if (norm.contains("information technology")) {
            acronyms.add("IT");
        }
        if (norm.contains("data structure")) {
            acronyms.add("DS");
            acronyms.add("DSA");
        }
        if (norm.contains("operating system")) {
            acronyms.add("OS");
        }
        if (norm.contains("database management") || norm.contains("data base management")) {
            acronyms.add("DBMS");
        }
        if (norm.contains("computer network")) {
            acronyms.add("CN");
        }
        if (norm.contains("software engineering")) {
            acronyms.add("SE");
        }
        if (norm.contains("object oriented")) {
            acronyms.add("OOP");
            acronyms.add("OOPS");
        }
        if (norm.contains("artificial intelligence")) {
            acronyms.add("AI");
        }
        if (norm.contains("machine learning")) {
            acronyms.add("ML");
        }
        if (norm.contains("school of engineering")) {
            acronyms.add("SOET");
            acronyms.add("SOE");
        }
        if (norm.contains("school of management")) {
            acronyms.add("SOM");
        }

        return acronyms;
    }

    /**
     * Calculates Levenshtein Distance between two strings.
     */
    public static int levenshteinDistance(String s1, String s2) {
        if (s1 == null || s2 == null) return Integer.MAX_VALUE;
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= s2.length(); j++) dp[0][j] = j;

        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = (Character.toLowerCase(s1.charAt(i - 1)) == Character.toLowerCase(s2.charAt(j - 1))) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost
                );
            }
        }
        return dp[s1.length()][s2.length()];
    }

    /**
     * Computes normalized similarity between 0.0 and 1.0 based on Levenshtein Distance.
     */
    public static double similarity(String s1, String s2) {
        if (s1 == null || s2 == null) return 0.0;
        String n1 = normalize(s1);
        String n2 = normalize(s2);
        if (n1.equals(n2)) return 1.0;
        int maxLen = Math.max(n1.length(), n2.length());
        if (maxLen == 0) return 1.0;
        int dist = levenshteinDistance(n1, n2);
        return Math.max(0.0, 1.0 - ((double) dist / maxLen));
    }

    /**
     * Computes token Jaccard similarity between two strings.
     */
    public static double tokenSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) return 0.0;
        Set<String> tokens1 = new HashSet<>(Arrays.asList(normalize(s1).split("\\s+")));
        Set<String> tokens2 = new HashSet<>(Arrays.asList(normalize(s2).split("\\s+")));
        tokens1.remove("");
        tokens2.remove("");
        if (tokens1.isEmpty() && tokens2.isEmpty()) return 1.0;
        if (tokens1.isEmpty() || tokens2.isEmpty()) return 0.0;

        Set<String> union = new HashSet<>(tokens1);
        union.addAll(tokens2);

        Set<String> intersection = new HashSet<>(tokens1);
        intersection.retainAll(tokens2);

        return (double) intersection.size() / union.size();
    }
}
