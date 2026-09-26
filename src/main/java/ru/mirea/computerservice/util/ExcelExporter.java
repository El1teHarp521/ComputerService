package ru.mirea.computerservice.util; // Пакет, в котором находится класс

import org.apache.poi.ss.usermodel.Row; // Импорт Row — строка Excel
import org.apache.poi.ss.usermodel.Sheet; // Импорт Sheet — лист Excel
import org.apache.poi.ss.usermodel.Workbook; // Импорт Workbook — книга Excel (интерфейс)
import org.apache.poi.xssf.usermodel.XSSFWorkbook; // Импорт XSSFWorkbook — реализация Workbook для формата .xlsx
import ru.mirea.computerservice.model.RepairRequest; // Импорт модели RepairRequest

import java.io.FileOutputStream; // Импорт FileOutputStream для записи файла
import java.io.IOException; // Импорт IOException для ошибок ввода-вывода
import java.time.format.DateTimeFormatter; // Импорт DateTimeFormatter для форматирования даты
import java.util.List; // Импорт List

public final class ExcelExporter { // Класс-утилита (final — нельзя наследовать)

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"); // Константа — формат даты

    private ExcelExporter() { // Приватный конструктор — запрет на создание экземпляров
    }

    public static void exportRequests(List<RepairRequest> requests, String filePath) throws IOException { // Метод экспорта заявок в Excel
        try (Workbook workbook = new XSSFWorkbook()) { // Создаем новую книгу Excel (автоматически закроется)
            Sheet sheet = workbook.createSheet("Заявки на ремонт"); // Создаем лист с названием

            String[] headers = {"ID", "Клиент", "Устройство", "Описание", "Статус", "Приоритет", "Стоимость", "Дата создания"}; // Массив заголовков
            Row headerRow = sheet.createRow(0); // Создаем нулевую строку (заголовки)
            for (int i = 0; i < headers.length; i++) { // Проходим по всем заголовкам
                headerRow.createCell(i).setCellValue(headers[i]); // Создаем ячейку и записываем заголовок
            }

            int rowNum = 1; // Начинаем с первой строки (после заголовков)
            for (RepairRequest r : requests) { // Проходим по всем заявкам
                Row row = sheet.createRow(rowNum++); // Создаем новую строку и увеличиваем счетчик
                row.createCell(0).setCellValue(r.getId()); // Ячейка 0 — ID
                row.createCell(1).setCellValue(r.getClientName()); // Ячейка 1 — имя клиента
                row.createCell(2).setCellValue(r.getDeviceType()); // Ячейка 2 — тип устройства
                row.createCell(3).setCellValue(r.getProblemDescription()); // Ячейка 3 — описание проблемы
                row.createCell(4).setCellValue(r.getStatus().getDisplayName()); // Ячейка 4 — статус (русское название)
                row.createCell(5).setCellValue(r.getPriority().getDisplayName()); // Ячейка 5 — приоритет (русское название)
                row.createCell(6).setCellValue(r.getCost() == null ? 0 : r.getCost().doubleValue()); // Ячейка 6 — стоимость (0, если null)
                row.createCell(7).setCellValue(r.getCreatedAt() == null ? "" : r.getCreatedAt().format(FORMATTER)); // Ячейка 7 — дата создания
            }

            for (int i = 0; i < headers.length; i++) { // Проходим по всем колонкам
                sheet.autoSizeColumn(i); // Автоматически подбираем ширину колонки
            }

            try (FileOutputStream fos = new FileOutputStream(filePath)) { // Открываем поток для записи в файл
                workbook.write(fos); // Записываем книгу в файл
            }
        }
    }
}