package com.dypiu.nba.service;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Method;
import java.util.Collections;

public class CourseMappingExcelImportServiceTest {

    @Test
    public void testParseLocalFile() throws Exception {
        File file = new File("/Users/rajshaikh/Desktop/co-po:pso.xlsx");
        if (!file.exists()) {
            System.out.println("File not found on desktop");
            return;
        }

        CourseMappingExcelImportService service = new CourseMappingExcelImportService(
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null
        );

        Method parseSheetMethod = CourseMappingExcelImportService.class.getDeclaredMethod(
                "parseSheet", org.apache.poi.ss.usermodel.Sheet.class, boolean.class,
                java.util.List.class, java.util.List.class, java.util.List.class
        );
        parseSheetMethod.setAccessible(true);

        try (FileInputStream fis = new FileInputStream(file); Workbook wb = WorkbookFactory.create(fis)) {
            org.apache.poi.ss.usermodel.Sheet poSheet = wb.getSheetAt(0);
            Object result = parseSheetMethod.invoke(service, poSheet, false, Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
            System.out.println("Parse result: " + result);

            // Access previewItems
            java.lang.reflect.Field itemsField = result.getClass().getDeclaredField("previewItems");
            itemsField.setAccessible(true);
            java.util.List<?> items = (java.util.List<?>) itemsField.get(result);
            System.out.println("Total preview items: " + items.size());
            for (Object it : items) {
                System.out.println("Item: " + it);
            }

            java.lang.reflect.Field matrixField = result.getClass().getDeclaredField("matrixLevels");
            matrixField.setAccessible(true);
            System.out.println("Matrix levels: " + matrixField.get(result));

            if (wb.getNumberOfSheets() > 1) {
                org.apache.poi.ss.usermodel.Sheet psoSheet = wb.getSheetAt(1);
                Object psoResult = parseSheetMethod.invoke(service, psoSheet, true, Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
                java.lang.reflect.Field psoItemsField = psoResult.getClass().getDeclaredField("previewItems");
                psoItemsField.setAccessible(true);
                java.util.List<?> psoItems = (java.util.List<?>) psoItemsField.get(psoResult);
                System.out.println("Total PSO preview items: " + psoItems.size());
                for (Object it : psoItems) {
                    System.out.println("PSO Item: " + it);
                }
            }
        }
    }

    @Test
    public void testCustomNamedCosMapping() throws Exception {
        CourseMappingExcelImportService service = new CourseMappingExcelImportService(
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null
        );

        Method findCoIdByCodeMethod = CourseMappingExcelImportService.class.getDeclaredMethod(
                "findCoIdByCode", java.util.List.class, String.class
        );
        findCoIdByCodeMethod.setAccessible(true);

        java.util.List<com.dypiu.nba.entity.CourseOutcome> cos = java.util.List.of(
                com.dypiu.nba.entity.CourseOutcome.builder().id("id-1").code("EM321.1").build(),
                com.dypiu.nba.entity.CourseOutcome.builder().id("id-2").code("EM321.2").build(),
                com.dypiu.nba.entity.CourseOutcome.builder().id("id-3").code("EM321.3").build(),
                com.dypiu.nba.entity.CourseOutcome.builder().id("id-4").code("EM321.4").build(),
                com.dypiu.nba.entity.CourseOutcome.builder().id("id-5").code("EM321.5").build()
        );

        org.junit.jupiter.api.Assertions.assertEquals("id-1", findCoIdByCodeMethod.invoke(service, cos, "CO1"));
        org.junit.jupiter.api.Assertions.assertEquals("id-2", findCoIdByCodeMethod.invoke(service, cos, "CO2"));
        org.junit.jupiter.api.Assertions.assertEquals("id-3", findCoIdByCodeMethod.invoke(service, cos, "CO3"));
        org.junit.jupiter.api.Assertions.assertEquals("id-4", findCoIdByCodeMethod.invoke(service, cos, "CO4"));
        org.junit.jupiter.api.Assertions.assertEquals("id-5", findCoIdByCodeMethod.invoke(service, cos, "CO5"));
        System.out.println("Custom named COs EM321.1..EM321.5 mapped successfully to CO1..CO5!");
    }
}
