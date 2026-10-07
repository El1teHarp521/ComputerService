package ru.mirea.computerservice.util;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import ru.mirea.computerservice.model.RepairRequest;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class ExcelExporter {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private ExcelExporter() {
    }

    public static void exportRequests(List<RepairRequest> requests, String filePath) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Заявки на ремонт");

            String[] headers = {"ID", "Клиент", "Устройство", "Описание", "Статус", "Приоритет", "Стоимость", "Дата создания"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            int rowNum = 1;
            for (RepairRequest r : requests) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(r.getId());
                row.createCell(1).setCellValue(r.getClientName());
                row.createCell(2).setCellValue(r.getDeviceType());
                row.createCell(3).setCellValue(r.getProblemDescription());
                row.createCell(4).setCellValue(r.getStatus().getDisplayName());
                row.createCell(5).setCellValue(r.getPriority().getDisplayName());
                row.createCell(6).setCellValue(r.getCost() == null ? 0 : r.getCost().doubleValue());
                row.createCell(7).setCellValue(r.getCreatedAt() == null ? "" : r.getCreatedAt().format(FORMATTER));
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                workbook.write(fos);
            }
        }
    }
}
