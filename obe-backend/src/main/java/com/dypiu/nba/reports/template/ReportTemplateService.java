package com.dypiu.nba.reports.template;

import com.dypiu.nba.reports.model.*;
import com.dypiu.nba.reports.repository.ReportAssetRepository;
import com.dypiu.nba.reports.repository.ReportTemplateRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportTemplateService {

    private final ReportTemplateRepository templateRepository;
    private final ReportAssetRepository assetRepository;
    private final ObjectMapper objectMapper;

    public ReportTemplateDto resolveTemplate(ReportType reportType, String institutionId) {
        Optional<ReportTemplateEntity> entityOpt = Optional.empty();
        if (institutionId != null && !institutionId.isBlank()) {
            entityOpt = templateRepository.findFirstByReportTypeAndInstitutionIdAndIsDefaultTrue(reportType, institutionId);
            if (entityOpt.isEmpty()) {
                entityOpt = templateRepository.findFirstByInstitutionIdAndIsDefaultTrue(institutionId);
            }
        }
        if (entityOpt.isEmpty()) {
            entityOpt = templateRepository.findFirstByReportTypeAndIsDefaultTrue(reportType);
        }
        if (entityOpt.isEmpty()) {
            entityOpt = templateRepository.findFirstByIsDefaultTrue();
        }
        if (entityOpt.isPresent()) {
            ReportTemplateDto dto = toDto(entityOpt.get());
            if (dto.getHeaderConfig() != null && (dto.getHeaderConfig().getHeaderTitle() == null || dto.getHeaderConfig().getHeaderTitle().isBlank())) {
                dto.getHeaderConfig().setHeaderTitle(getReportTitle(reportType));
            }
            if (dto.getBodyDefinition() != null) {
                dto.getBodyDefinition().setReportType(reportType);
                String orientation = (reportType == ReportType.COURSE_ATR || reportType == ReportType.PROGRAMME_ATR)
                        ? "PORTRAIT" : "LANDSCAPE";
                dto.getBodyDefinition().setOrientation(orientation);
            }
            return dto;
        }
        return createDefaultTemplate(reportType, institutionId);
    }

    @Transactional(readOnly = true)
    public ReportTemplateDto getInstitutionTemplate(String institutionId) {
        String inst = (institutionId != null && !institutionId.isBlank()) ? institutionId : "DYPIU";
        Optional<ReportTemplateEntity> entityOpt = templateRepository.findFirstByInstitutionIdAndIsDefaultTrue(inst);
        if (entityOpt.isEmpty()) {
            entityOpt = templateRepository.findFirstByIsDefaultTrue();
        }
        return entityOpt.map(this::toDto).orElseGet(() -> createDefaultTemplate(ReportType.PROGRAMME_ATTAINMENT, inst));
    }

    @Transactional
    public ReportTemplateDto saveInstitutionTemplate(ReportTemplateDto dto, String institutionId) {
        String inst = (institutionId != null && !institutionId.isBlank())
                ? institutionId
                : (dto.getInstitutionId() != null && !dto.getInstitutionId().isBlank() ? dto.getInstitutionId() : "DYPIU");

        ReportTemplateEntity entity = templateRepository.findFirstByInstitutionIdAndIsDefaultTrue(inst)
                .orElse(null);

        if (entity == null) {
            entity = ReportTemplateEntity.builder()
                    .id("tpl-" + UUID.randomUUID().toString().substring(0, 8))
                    .templateName(dto.getTemplateName() != null ? dto.getTemplateName() : "Institution Common Template")
                    .reportType(dto.getReportType() != null ? dto.getReportType() : ReportType.PROGRAMME_ATTAINMENT)
                    .templateVersion(1)
                    .isDefault(true)
                    .institutionId(inst)
                    .build();
        } else {
            entity.setTemplateVersion((entity.getTemplateVersion() != null ? entity.getTemplateVersion() : 1) + 1);
            if (dto.getTemplateName() != null && !dto.getTemplateName().isBlank()) {
                entity.setTemplateName(dto.getTemplateName());
            }
        }

        try {
            if (dto.getHeaderConfig() != null) {
                entity.setHeaderConfigJson(objectMapper.writeValueAsString(dto.getHeaderConfig()));
            }
            if (dto.getBodyDefinition() != null) {
                entity.setBodyDefinitionJson(objectMapper.writeValueAsString(dto.getBodyDefinition()));
            }
            if (dto.getFooterConfig() != null) {
                entity.setFooterConfigJson(objectMapper.writeValueAsString(dto.getFooterConfig()));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize template configuration", e);
        }

        ReportTemplateEntity saved = templateRepository.save(entity);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public HeaderConfig getHeaderConfig(String institutionId) {
        ReportTemplateDto template = getInstitutionTemplate(institutionId);
        return template.getHeaderConfig() != null ? template.getHeaderConfig() : HeaderConfig.builder().build();
    }

    @Transactional
    public HeaderConfig saveHeaderConfig(HeaderConfig headerConfig, String institutionId) {
        String inst = (institutionId != null && !institutionId.isBlank()) ? institutionId : "DYPIU";
        ReportTemplateDto current = getInstitutionTemplate(inst);
        current.setHeaderConfig(headerConfig);
        ReportTemplateDto saved = saveInstitutionTemplate(current, inst);
        return saved.getHeaderConfig();
    }

    public ReportTemplateDto createDefaultTemplate(ReportType reportType, String institutionId) {
        String orientation = (reportType == ReportType.COURSE_ATR || reportType == ReportType.PROGRAMME_ATR)
                ? "PORTRAIT" : "LANDSCAPE";

        HeaderConfig header = HeaderConfig.builder()
                .institutionName("D. Y. PATIL INTERNATIONAL UNIVERSITY, PUNE")
                .subHeader("Sector 29, Nigdi Pradhikaran, Akurdi, Pune, Maharashtra 411044")
                .accreditationText("Approved by AICTE | Outcome-Based Education (OBE) NBA Compliance")
                .headerTitle(getReportTitle(reportType))
                .showLogo(true)
                .build();

        BodyDefinition body = BodyDefinition.builder()
                .reportType(reportType)
                .orientation(orientation)
                .primaryThemeColor("#4F46E5")
                .accentThemeColor("#059669")
                .build();

        FooterConfig footer = FooterConfig.builder()
                .standardFooterText("DYPIU NBA Attainment System · Authoritative Academic Record")
                .showPageNumbers(true)
                .showGeneratedTimestamp(true)
                .showVerificationHash(true)
                .build();

        return ReportTemplateDto.builder()
                .id("tpl-def-" + reportType.name().toLowerCase())
                .templateName("Standard " + reportType.name() + " Template")
                .reportType(reportType)
                .templateVersion(1)
                .isDefault(true)
                .institutionId(institutionId != null ? institutionId : "DYPIU")
                .headerConfig(header)
                .bodyDefinition(body)
                .footerConfig(footer)
                .build();
    }

    public String getReportTitle(ReportType reportType) {
        if (reportType == null) return "ACADEMIC ATTAINMENT REPORT";
        return switch (reportType) {
            case COURSE_ATR -> "COURSE ACTION TAKEN REPORT (ATR)";
            case PROGRAMME_ATR -> "PROGRAMME ACTION TAKEN REPORT (ATR)";
            case COURSE_ATTAINMENT -> "COURSE ATTAINMENT CONSOLIDATED REPORT";
            case PROGRAMME_ATTAINMENT -> "PROGRAMME ATTAINMENT MASTER CONSOLIDATED REPORT";
            case PROGRAMME_ATTAINMENT_MAPPING -> "PROGRAMME ATTAINMENT — AVERAGE MAPPING REPORT";
            case PROGRAMME_ATTAINMENT_DIRECT -> "PROGRAMME ATTAINMENT — AVERAGE DIRECT ATTAINMENT REPORT";
            case PROGRAMME_ATTAINMENT_INDIRECT -> "PROGRAMME ATTAINMENT — AVERAGE INDIRECT ATTAINMENT REPORT";
            case PROGRAMME_ATTAINMENT_OVERALL -> "PROGRAMME ATTAINMENT — OVERALL ATTAINMENT REPORT";
        };
    }

    public ReportTemplateDto toDto(ReportTemplateEntity entity) {
        HeaderConfig header = null;
        BodyDefinition body = null;
        FooterConfig footer = null;

        try {
            if (entity.getHeaderConfigJson() != null && !entity.getHeaderConfigJson().isBlank()) {
                header = objectMapper.readValue(entity.getHeaderConfigJson(), HeaderConfig.class);
            }
            if (entity.getBodyDefinitionJson() != null && !entity.getBodyDefinitionJson().isBlank()) {
                body = objectMapper.readValue(entity.getBodyDefinitionJson(), BodyDefinition.class);
            }
            if (entity.getFooterConfigJson() != null && !entity.getFooterConfigJson().isBlank()) {
                footer = objectMapper.readValue(entity.getFooterConfigJson(), FooterConfig.class);
            }
        } catch (Exception e) {
            log.warn("Error parsing template configs for template {}: {}", entity.getId(), e.getMessage());
        }

        if (header == null) header = HeaderConfig.builder().build();
        if (body == null) body = BodyDefinition.builder().reportType(entity.getReportType()).build();
        if (footer == null) footer = FooterConfig.builder().build();

        return ReportTemplateDto.builder()
                .id(entity.getId())
                .templateName(entity.getTemplateName())
                .reportType(entity.getReportType())
                .templateVersion(entity.getTemplateVersion())
                .isDefault(entity.getIsDefault())
                .institutionId(entity.getInstitutionId())
                .headerConfig(header)
                .bodyDefinition(body)
                .footerConfig(footer)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public ReportAssetDto toAssetDto(ReportAssetEntity entity) {
        if (entity == null) return null;
        return ReportAssetDto.builder()
                .assetId(entity.getId())
                .institutionId(entity.getInstitutionId())
                .assetType(entity.getAssetType())
                .originalFilename(entity.getOriginalFilename())
                .storagePath(entity.getStoragePath())
                .mimeType(entity.getMimeType())
                .fileSize(entity.getFileSize())
                .createdBy(entity.getCreatedBy())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
