package com.dypiu.nba.reports.excel;

import com.dypiu.nba.reports.model.snapshot.CourseAttainmentSnapshot;
import lombok.Builder;
import lombok.Data;

import java.util.*;

/**
 * Authoritative helper for ordering Course Outcomes in natural ascending order
 * and mapping actual curriculum CO codes (e.g. CS201.1 .. CS201.6) to standard
 * acronyms (CO1 .. CON) aligned 1-to-1 with the sorted COs.
 */
public class CourseOutcomeOrderHelper {

    @Data
    @Builder
    public static class CourseOutcomeItem {
        private int index; // 0-based index
        private String actualCode; // e.g. "CS201.1" or "CO1"
        private String acronym; // "CO1", "CO2", ..., "CON"
        private String statement;
        private CourseAttainmentSnapshot.CoAttainmentRow attainmentRow;
        private CourseAttainmentSnapshot.CoMappingRow mappingRow;
    }

    /**
     * Case-insensitive natural numerical comparator that properly sorts
     * strings like "CO1", "CO2", "CO10", "CS201.1", "CS201.2", "CS201.10", etc.
     */
    public static final Comparator<String> NATURAL_ORDER = (s1, s2) -> {
        if (s1 == null && s2 == null) return 0;
        if (s1 == null) return -1;
        if (s2 == null) return 1;

        int i1 = 0, i2 = 0;
        int len1 = s1.length(), len2 = s2.length();

        while (i1 < len1 && i2 < len2) {
            char c1 = s1.charAt(i1);
            char c2 = s2.charAt(i2);

            if (Character.isDigit(c1) && Character.isDigit(c2)) {
                int start1 = i1;
                while (i1 < len1 && Character.isDigit(s1.charAt(i1))) i1++;
                int start2 = i2;
                while (i2 < len2 && Character.isDigit(s2.charAt(i2))) i2++;

                String num1 = s1.substring(start1, i1).replaceFirst("^0+(?!$)", "");
                String num2 = s2.substring(start2, i2).replaceFirst("^0+(?!$)", "");

                if (num1.length() != num2.length()) {
                    return Integer.compare(num1.length(), num2.length());
                }
                int cmp = num1.compareTo(num2);
                if (cmp != 0) return cmp;
            } else {
                int cmp = Character.compare(Character.toLowerCase(c1), Character.toLowerCase(c2));
                if (cmp != 0) return cmp;
                i1++;
                i2++;
            }
        }
        return Integer.compare(len1, len2);
    };

    public static class CourseOutcomeRegistry {
        private final List<CourseOutcomeItem> items;
        private final Map<String, CourseOutcomeItem> byActualCode = new HashMap<>();
        private final Map<String, CourseOutcomeItem> byAcronym = new HashMap<>();

        public CourseOutcomeRegistry(List<CourseOutcomeItem> items) {
            this.items = Collections.unmodifiableList(items);
            for (CourseOutcomeItem item : items) {
                if (item.getActualCode() != null) {
                    byActualCode.put(item.getActualCode().trim().toLowerCase(), item);
                }
                if (item.getAcronym() != null) {
                    byAcronym.put(item.getAcronym().trim().toLowerCase(), item);
                }
            }
        }

        public List<CourseOutcomeItem> getItems() {
            return items;
        }

        public int size() {
            return items.size();
        }

        public CourseOutcomeItem get(int index) {
            return items.get(index);
        }

        public CourseOutcomeItem find(String codeOrAcronym) {
            if (codeOrAcronym == null) return null;
            String key = codeOrAcronym.trim().toLowerCase();
            CourseOutcomeItem item = byActualCode.get(key);
            if (item != null) return item;
            return byAcronym.get(key);
        }

        public List<String> getAcronyms() {
            return items.stream().map(CourseOutcomeItem::getAcronym).toList();
        }

        public List<String> getActualCodes() {
            return items.stream().map(CourseOutcomeItem::getActualCode).toList();
        }

        /**
         * Look up a value from a map keyed by either the actual CO code, acronym, normalized code,
         * suffix number, or positional fallback.
         */
        public <T> T lookupValue(Map<String, T> map, CourseOutcomeItem item) {
            if (map == null || map.isEmpty() || item == null) return null;

            // 1. Direct exact lookup on actualCode
            if (item.getActualCode() != null && map.containsKey(item.getActualCode())) {
                return map.get(item.getActualCode());
            }

            // 2. Direct exact lookup on acronym
            if (item.getAcronym() != null && map.containsKey(item.getAcronym())) {
                return map.get(item.getAcronym());
            }

            // 3. Case-insensitive lookup on actualCode or acronym
            for (Map.Entry<String, T> entry : map.entrySet()) {
                String k = entry.getKey();
                if (k != null) {
                    if (k.equalsIgnoreCase(item.getActualCode()) || k.equalsIgnoreCase(item.getAcronym())) {
                        return entry.getValue();
                    }
                }
            }

            // 4. Normalized lookup (strip non-alphanumerics, e.g. "CO 1", "CO-1", "CO_1" -> "co1")
            String normActual = item.getActualCode() != null ? item.getActualCode().replaceAll("[^a-zA-Z0-9]", "").toLowerCase() : "";
            String normAcronym = item.getAcronym() != null ? item.getAcronym().replaceAll("[^a-zA-Z0-9]", "").toLowerCase() : "";
            for (Map.Entry<String, T> entry : map.entrySet()) {
                String k = entry.getKey();
                if (k != null) {
                    String normK = k.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
                    if ((!normActual.isEmpty() && normK.equals(normActual))
                            || (!normAcronym.isEmpty() && normK.equals(normAcronym))) {
                        return entry.getValue();
                    }
                }
            }

            // 5. Suffix / Number lookup (e.g. key is "1" or "01" and acronym is "CO1")
            int oneBasedIndex = item.getIndex() + 1;
            String indexStr = String.valueOf(oneBasedIndex);
            for (Map.Entry<String, T> entry : map.entrySet()) {
                String k = entry.getKey();
                if (k != null) {
                    String normK = k.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
                    if (normK.equals(indexStr) || normK.endsWith("co" + indexStr) || normK.equals("q" + indexStr)) {
                        return entry.getValue();
                    }
                }
            }

            // 6. Positional fallback if map size covers the items
            if (item.getIndex() >= 0 && item.getIndex() < map.size()) {
                List<T> values = new ArrayList<>(map.values());
                return values.get(item.getIndex());
            }

            return null;
        }

        /**
         * Look up a value by CO index from a map keyed by either actual code, acronym, or position.
         */
        public <T> T lookupValue(Map<String, T> map, int index) {
            if (index < 0 || index >= items.size()) return null;
            return lookupValue(map, items.get(index));
        }
    }

    /**
     * Resolves all course outcomes from snapshot, sorts them in natural ascending order,
     * and maps each to acronyms CO1, CO2, ..., CON aligned 1-to-1 with actual CO codes.
     */
    public static CourseOutcomeRegistry resolveRegistry(CourseAttainmentSnapshot snapshot) {
        Map<String, CourseOutcomeItem.CourseOutcomeItemBuilder> candidateBuilders = new LinkedHashMap<>();

        // 1. Table 3 (Attainment rows) - Primary authoritative source
        if (snapshot != null && snapshot.getTable3CoAttainments() != null) {
            for (CourseAttainmentSnapshot.CoAttainmentRow r : snapshot.getTable3CoAttainments()) {
                if (r.getCoCode() != null && !r.getCoCode().isBlank()) {
                    String code = r.getCoCode().trim();
                    candidateBuilders.computeIfAbsent(code, k -> CourseOutcomeItem.builder().actualCode(k))
                            .statement(r.getStatement() != null ? r.getStatement() : "")
                            .attainmentRow(r);
                }
            }
        }

        // 2. Table 1 (Mapping rows) - Secondary curriculum source
        if (snapshot != null && snapshot.getTable1Mapping() != null) {
            for (CourseAttainmentSnapshot.CoMappingRow r : snapshot.getTable1Mapping()) {
                if (r.getCoCode() != null && !r.getCoCode().isBlank()) {
                    String code = r.getCoCode().trim();
                    CourseOutcomeItem.CourseOutcomeItemBuilder builder = candidateBuilders.computeIfAbsent(
                            code, k -> CourseOutcomeItem.builder().actualCode(k));
                    builder.mappingRow(r);
                }
            }
        }

        // 3. Only if no COs exist from Table 3 or Table 1, fall back to Examination Data CO codes
        if (candidateBuilders.isEmpty() && snapshot != null && snapshot.getExaminationData() != null && snapshot.getExaminationData().getCoCodes() != null) {
            for (String c : snapshot.getExaminationData().getCoCodes()) {
                if (c != null && !c.isBlank()) {
                    String code = c.trim();
                    candidateBuilders.computeIfAbsent(code, k -> CourseOutcomeItem.builder().actualCode(k));
                }
            }
        }

        // 4. Only if still empty, fall back to Survey Data CO codes
        if (candidateBuilders.isEmpty() && snapshot != null && snapshot.getSurveyData() != null && snapshot.getSurveyData().getCoCodes() != null) {
            for (String c : snapshot.getSurveyData().getCoCodes()) {
                if (c != null && !c.isBlank()) {
                    String code = c.trim();
                    candidateBuilders.computeIfAbsent(code, k -> CourseOutcomeItem.builder().actualCode(k));
                }
            }
        }

        // Default fallback if no COs are found anywhere
        if (candidateBuilders.isEmpty()) {
            for (int i = 1; i <= 6; i++) {
                String code = "CO" + i;
                candidateBuilders.put(code, CourseOutcomeItem.builder()
                        .actualCode(code)
                        .statement("Statement for " + code));
            }
        }

        // Sort candidates by actualCode in natural ascending order
        List<String> sortedCodes = new ArrayList<>(candidateBuilders.keySet());
        sortedCodes.sort(NATURAL_ORDER);

        List<CourseOutcomeItem> result = new ArrayList<>();
        for (int i = 0; i < sortedCodes.size(); i++) {
            String code = sortedCodes.get(i);
            CourseOutcomeItem.CourseOutcomeItemBuilder b = candidateBuilders.get(code);
            b.index(i);
            b.acronym("CO" + (i + 1));
            result.add(b.build());
        }

        return new CourseOutcomeRegistry(result);
    }
}
