package com.dypiu.nba.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Entity
@Table(
        name = "master_programmes",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_department_programme_code",
                        columnNames = {"department_id", "code"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@Builder
public class MasterProgramme {

    public static final Map<String, String[]> COORDINATOR_CACHE = new ConcurrentHashMap<>();

    @Id
    private String id;

    @Column(name = "department_id", nullable = false)
    private String departmentId;

    @Column(nullable = false, length = 20, unique = true)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "duration_years", nullable = false)
    @Builder.Default
    private Integer durationYears = 4;

    @Builder.Default
    @Column(nullable = false, length = 30)
    private String status = "ACTIVE";

    @Column(name = "department_name", length = 255)
    private String departmentName;

    @Column(name = "created_at", insertable = false, updatable = false)
    private ZonedDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private ZonedDateTime updatedAt;

    @Transient
    private String coordinator;

    @Transient
    private String coordinatorEmail;

    public MasterProgramme(String id, String departmentId, String code, String name, Integer durationYears, String status, String departmentName, ZonedDateTime createdAt, ZonedDateTime updatedAt, String coordinator, String coordinatorEmail) {
        this.id = id;
        this.departmentId = departmentId;
        this.code = code;
        this.name = name;
        this.durationYears = durationYears != null ? durationYears : 4;
        this.status = status != null ? status : "ACTIVE";
        this.departmentName = departmentName;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.coordinator = coordinator;
        this.coordinatorEmail = coordinatorEmail;
        if (id != null && coordinator != null && !coordinator.isBlank()) {
            COORDINATOR_CACHE.put(id, new String[]{coordinator, coordinatorEmail != null ? coordinatorEmail : ""});
        }
    }

    public String getCoordinator() {
        if (this.coordinator != null && !this.coordinator.isBlank() && !"Not Assigned".equalsIgnoreCase(this.coordinator)) {
            if (this.id != null) {
                COORDINATOR_CACHE.put(this.id, new String[]{this.coordinator, this.coordinatorEmail != null ? this.coordinatorEmail : ""});
            }
            return this.coordinator;
        }
        if (this.id != null && COORDINATOR_CACHE.containsKey(this.id)) {
            String[] pair = COORDINATOR_CACHE.get(this.id);
            if (pair != null && pair.length > 0 && pair[0] != null && !pair[0].isBlank()) {
                return pair[0];
            }
        }
        return this.coordinator;
    }

    public String getCoordinatorEmail() {
        if (this.coordinatorEmail != null && !this.coordinatorEmail.isBlank()) {
            if (this.id != null) {
                COORDINATOR_CACHE.put(this.id, new String[]{this.coordinator != null ? this.coordinator : "", this.coordinatorEmail});
            }
            return this.coordinatorEmail;
        }
        if (this.id != null && COORDINATOR_CACHE.containsKey(this.id)) {
            String[] pair = COORDINATOR_CACHE.get(this.id);
            if (pair != null && pair.length > 1 && pair[1] != null && !pair[1].isBlank()) {
                return pair[1];
            }
        }
        return this.coordinatorEmail;
    }

    public void setCoordinator(String coordinator) {
        this.coordinator = coordinator;
        if (this.id != null && coordinator != null && !coordinator.isBlank()) {
            COORDINATOR_CACHE.put(this.id, new String[]{coordinator, this.coordinatorEmail != null ? this.coordinatorEmail : ""});
        }
    }

    public void setCoordinatorEmail(String coordinatorEmail) {
        this.coordinatorEmail = coordinatorEmail;
        if (this.id != null && coordinatorEmail != null && !coordinatorEmail.isBlank()) {
            String c = this.coordinator != null ? this.coordinator : "";
            COORDINATOR_CACHE.put(this.id, new String[]{c, coordinatorEmail});
        }
    }
}
