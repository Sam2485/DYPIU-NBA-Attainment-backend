#!/bin/bash
sed -i '' -e 's/public List<Course> getCoursesByProgramme(String programmeId) {/public List<Course> getCoursesByProgramme(String programmeId, String batchId) {/' /Users/rajshaikh/Desktop/DYPIU-NBA-Attainment-backend/obe-backend/src/main/java/com/dypiu/nba/service/AcademicService.java
