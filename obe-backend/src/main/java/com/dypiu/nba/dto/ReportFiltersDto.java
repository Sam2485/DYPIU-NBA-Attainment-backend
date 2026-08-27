package com.dypiu.nba.dto;

import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportFiltersDto {
    private String role;
    private List<Item> programmes;
    private List<BatchItem> batches;
    private List<OfferingItem> courseOfferings;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Item {
        private String id;
        private String name;
        private String code;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class BatchItem {
        private String id;
        private String masterProgrammeId;
        private String name;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class OfferingItem {
        private String id;
        private String masterCourseId;
        private String programmeBatchId;
        private String courseCode;
        private String courseName;
        private Integer semester;
    }
}
