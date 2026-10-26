package ru.mirea.computerservice.service;

import ru.mirea.computerservice.exception.BusinessException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Единое место всех проверок пользовательского ввода.
 * Проверяет данные ДО обращения к базе данных и выдает понятные сообщения пользователю.
 */
final class Validation {

    static final int NAME_MIN = 2;
    static final int NAME_MAX = 150;           // clients.full_name VARCHAR(150)
    static final int PHONE_RAW_MAX = 40;       // защита от слишком длинного ввода
    static final int EMAIL_MAX = 100;          // clients.email VARCHAR(100)
    static final int DEVICE_MAX = 50;          // repair_requests.device_type VARCHAR(50)
    static final int DESCRIPTION_MAX = 1000;   // repair_requests.problem_description TEXT
    static final BigDecimal COST_MAX = new BigDecimal("99999999.99"); // NUMERIC(10,2)

    // Разрешённые почтовые сервисы
    private static final Set<String> ALLOWED_EMAIL_DOMAINS = Set.of(
            "mail.ru", "yandex.ru", "gmail.com", "ya.ru", "bk.ru"
    );

    private static final Pattern PHONE_CHARS = Pattern.compile("^\\+?[0-9\\s()\\-]+$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+\\-]+@([A-Za-z0-9.\\-]+\\.[A-Za-z]{2,})$");

    private Validation() {
    }

    static String requiredText(String value, String field, int max) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(field + ": поле обязательно для заполнения");
        }
        String v = value.trim();
        if (v.length() > max) {
            throw new BusinessException(field + ": слишком длинное значение (" + v.length()
                    + " симв., максимум " + max + ")");
        }
        return v;
    }

    /**
     * Валидация и нормализация ФИО:
     * - Требует ровно 3 части: Фамилия, Имя, Отчество
     * - Запрещает неполный ввод (только Фамилия или Фамилия + Имя)
     * - Схлопывает лишние пробелы между словами
     * - Автоматически делает первые буквы заглавными (смолкин -> Смолкин)
     * - Запрещает цифры и посторонние знаки
     */
    static String personName(String value) {
        String raw = requiredText(value, "ФИО", NAME_MAX);

        // Убираем пробелы по краям и заменяем множественные пробелы между словами на один
        String normalized = raw.trim().replaceAll("\\s+", " ");
        String[] parts = normalized.split(" ");

        if (parts.length < 3) {
            throw new BusinessException("ФИО: необходимо указать полностью Фамилию, Имя и Отчество через пробел (нельзя ввести только Фамилию или Имя)");
        }
        if (parts.length > 3) {
            throw new BusinessException("ФИО: укажите ровно три слова (Фамилия, Имя, Отчество)");
        }

        StringBuilder formattedName = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];

            // Проверка: только буквы и дефис (для двойных фамилий)
            if (!part.matches("^[A-Za-zА-Яа-яЁё\\-]+$")) {
                throw new BusinessException("ФИО: слово «" + part + "» должно содержать только буквы");
            }
            if (part.length() < 2) {
                throw new BusinessException("ФИО: слово «" + part + "» слишком короткое (минимум 2 буквы)");
            }

            // Автоматически делаем первую букву заглавной, а остальные строчными
            String capitalized = Character.toUpperCase(part.charAt(0)) + part.substring(1).toLowerCase(Locale.ROOT);
            formattedName.append(capitalized);

            if (i < parts.length - 1) {
                formattedName.append(" ");
            }
        }

        return formattedName.toString();
    }

    /**
     * Строгая проверка номера телефона:
     * - Только допустимые символы (+, пробелы, скобки, дефисы)
     * - Ровно 11 цифр
     * - Номер РФ: начинается с +7 или 8, код оператора начинается с 9
     * - Приводит к единому виду: +7XXXXXXXXXX
     */
    static String phone(String value) {
        String v = requiredText(value, "Телефон", PHONE_RAW_MAX);
        if (!PHONE_CHARS.matcher(v).matches()) {
            throw new BusinessException("Телефон: допустимы только цифры, пробелы, скобки, дефис и «+» в начале");
        }

        String digits = v.replaceAll("[^0-9]", "");

        if (digits.length() != 11) {
            throw new BusinessException("Телефон: российский номер должен содержать ровно 11 цифр (сейчас " + digits.length() + ")");
        }

        if (!digits.startsWith("7") && !digits.startsWith("8")) {
            throw new BusinessException("Телефон: номер должен начинаться с +7 или 8");
        }

        if (digits.charAt(1) != '9') {
            throw new BusinessException("Телефон: некорректный мобильный код оператора (ожидается +7 (9XX) ...)");
        }

        // Нормализуем строго к виду: +79XXXXXXXXX
        return "+7" + digits.substring(1);
    }

    /**
     * Проверка Email:
     * - Необязателен (пустое поле допустимо)
     * - Если введён — проверяется формат
     * - Разрешены ТОЛЬКО: mail.ru, yandex.ru, gmail.com, ya.ru, bk.ru
     */
    static String emailOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String v = value.trim().toLowerCase(Locale.ROOT);
        if (v.length() > EMAIL_MAX) {
            throw new BusinessException("Email: слишком длинное значение (максимум " + EMAIL_MAX + " символов)");
        }

        var matcher = EMAIL_PATTERN.matcher(v);
        if (!matcher.matches()) {
            throw new BusinessException("Email: неверный формат (ожидается вид name@domain.ru)");
        }

        String domain = matcher.group(1);
        if (!ALLOWED_EMAIL_DOMAINS.contains(domain)) {
            throw new BusinessException("Email: разрешены только сервисы " + String.join(", ", ALLOWED_EMAIL_DOMAINS));
        }

        return v;
    }

    static String deviceType(String value) {
        return requiredText(value, "Тип устройства", DEVICE_MAX);
    }

    static String description(String value) {
        return requiredText(value, "Описание неисправности", DESCRIPTION_MAX);
    }

    static <T> T required(T value, String field) {
        if (value == null) {
            throw new BusinessException(field + ": значение не выбрано");
        }
        return value;
    }

    /**
     * Проверка стоимости:
     * - Не null, не отрицательная
     * - Максимум 2 знака после запятой
     * - Не больше 99 999 999.99
     */
    static BigDecimal cost(BigDecimal value) {
        required(value, "Стоимость");
        if (value.signum() < 0) {
            throw new BusinessException("Стоимость: не может быть отрицательной");
        }
        if (value.stripTrailingZeros().scale() > 2) {
            throw new BusinessException("Стоимость: не более двух знаков после запятой");
        }
        if (value.compareTo(COST_MAX) > 0) {
            throw new BusinessException("Стоимость: не больше 99 999 999,99");
        }
        return value.setScale(2, RoundingMode.UNNECESSARY);
    }
}