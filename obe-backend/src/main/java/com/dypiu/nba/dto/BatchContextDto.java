package com.dypiu.nba.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchContextDto {
    private BatchSummary batch;
    private ProgrammeSummary programme;
    private DepartmentSummary department;
    private SchoolSummary school;
    private Statistics statistics;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class BatchSummary {
        private String id;
        private String name;
        private String programmeId;
        private String programmeName;
        private String status;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ProgrammeSummary {
        private String id;
        private String code;
        private String name;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class DepartmentSummary {
        private String id;
        private String name;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SchoolSummary {
        private String id;
        private String name;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Statistics {
        private long studentCount;
        private long courseCount;
        private long courseOfferingCount;
        private long completedCourseAtrCount;
        private String programmeAtrStatus;
    }
}
