package com.dypiu.nba.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentRequestDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private String role;
    private String schoolId;
    private String schoolName;
    private String departmentId;
    private String masterProgrammeId;
}
