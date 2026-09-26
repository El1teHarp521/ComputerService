package ru.mirea.computerservice.util; // Пакет, в котором находится класс

import java.math.BigDecimal; // Импорт BigDecimal для работы с денежными значениями
import java.util.Scanner; // Импорт Scanner для чтения ввода пользователя

public final class InputHelper { // Класс-утилита (final — нельзя наследовать)

    private InputHelper() { // Приватный конструктор — запрет на создание экземпляров класса
    }

    public static int readInt(Scanner scanner, String prompt) { // Метод чтения целого числа
        while (true) { // Бесконечный цикл — повторяем, пока пользователь не введет корректное значение
            System.out.print(prompt); // Выводим приглашение к вводу
            String line = scanner.nextLine().trim(); // Читаем строку и убираем пробелы по краям
            try {
                return Integer.parseInt(line); // Пытаемся преобразовать строку в int и вернуть
            } catch (NumberFormatException e) { // Если преобразование не удалось
                System.out.println("Ошибка: введите целое число."); // Сообщаем об ошибке и повторяем цикл
            }
        }
    }

    public static BigDecimal readBigDecimal(Scanner scanner, String prompt) { // Метод чтения числа с плавающей точкой
        while (true) { // Бесконечный цикл — повторяем, пока пользователь не введет корректное значение
            System.out.print(prompt); // Выводим приглашение к вводу
            String line = scanner.nextLine().trim().replace(",", "."); // Читаем строку, убираем пробелы, заменяем запятую на точку
            try {
                return new BigDecimal(line); // Пытаемся создать BigDecimal и вернуть
            } catch (NumberFormatException e) { // Если преобразование не удалось
                System.out.println("Ошибка: введите число, например 1500.00"); // Сообщаем об ошибке и повторяем цикл
            }
        }
    }

    public static String readNonEmptyString(Scanner scanner, String prompt) { // Метод чтения непустой строки
        while (true) { // Бесконечный цикл — повторяем, пока пользователь не введет непустое значение
            System.out.print(prompt); // Выводим приглашение к вводу
            String line = scanner.nextLine().trim(); // Читаем строку и убираем пробелы по краям
            if (!line.isEmpty()) { // Если строка не пустая
                return line; // Возвращаем ее
            }
            System.out.println("Поле не может быть пустым."); // Иначе сообщаем об ошибке и повторяем цикл
        }
    }

    public static String readString(Scanner scanner, String prompt) { // Метод чтения строки (может быть пустой)
        System.out.print(prompt); // Выводим приглашение к вводу
        return scanner.nextLine().trim(); // Читаем строку, убираем пробелы и возвращаем (без проверки на пустоту)
    }
}