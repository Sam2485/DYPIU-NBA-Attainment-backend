# Local Server Storage Specifications

## Dedicated Local Server File Storage

Per project requirements, cloud file storage (AWS S3, Azure Blob, Firebase) is strictly avoided. All marksheets, survey Excel sheets, and report attachments are stored in **Local Server Disk Storage**.

### Storage Directory Structure

```text
${app.storage.local-dir}/
├── marks/
│   └── {courseId}/
│       └── {uploadId}_{fileName}.xlsx
├── surveys/
│   └── {courseId}/
│       └── {surveyId}_{fileName}.csv
└── reports/
    └── {programmeId}/
        └── {atrId}_{fileName}.pdf
```

### Safety & Configuration Rules
- Storage base path is configurable via `LOCAL_STORAGE_PATH` environment variable.
- Absolute paths are never hardcoded in Java source files.
- File upload limits are set to 20MB (`spring.servlet.multipart.max-file-size=20MB`).
