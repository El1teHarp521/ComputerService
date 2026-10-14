package ru.mirea.computerservice.service;

import ru.mirea.computerservice.exception.BusinessException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Единое место всех проверок пользовательского ввода.
 *
 * Зачем отдельный класс: и ClientService, и RepairRequestService проверяют строки
 * одинаково, а правила длины повторяют ограничения таблиц из schema.sql
 * (VARCHAR(150), VARCHAR(20), NUMERIC(10,2) ...). Если проверить это только в базе,
 * пользователь увидит текст ошибки PostgreSQL — поэтому проверяем заранее в Java
 * и говорим человеческим языком, ЧТО за поле и ЧТО исправить.
 */
final class Validation {

    static final int NAME_MIN = 2;
    static final int NAME_MAX = 150;           // clients.full_name VARCHAR(150)
    static final int PHONE_RAW_MAX = 40;       // защита от «простыни» в поле
    static final int PHONE_DIGITS_MIN = 10;
    static final int PHONE_DIGITS_MAX = 15;
    static final int EMAIL_MAX = 100;          // clients.email VARCHAR(100)
    static final int DEVICE_MAX = 50;          // repair_requests.device_type VARCHAR(50)
    static final int DESCRIPTION_MAX = 1000;   // в БД TEXT, но разумный предел нужен
    static final BigDecimal COST_MAX = new BigDecimal("99999999.99"); // NUMERIC(10,2)

    private static final Pattern PHONE_CHARS = Pattern.compile("^\\+?[0-9\\s()\\-]+$");
    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$");

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

    static String personName(String value) {
        String v = requiredText(value, "ФИО", NAME_MAX);
        if (v.length() < NAME_MIN) {
            throw new BusinessException("ФИО: не короче " + NAME_MIN + " символов");
        }
        return v;
    }

    /**
     * Проверяет телефон и приводит к единому виду: «+», затем только цифры.
     * Нормализация нужна, чтобы «+7 (900) 111-22-33» и «+79001112233» считались
     * ОДНИМ номером при проверке уникальности.
     */
    static String phone(String value) {
        String v = requiredText(value, "Телефон", PHONE_RAW_MAX);
        if (!PHONE_CHARS.matcher(v).matches()) {
            throw new BusinessException("Телефон: допустимы только цифры, пробелы, скобки, дефис и «+» в начале");
        }
        long digits = v.chars().filter(Character::isDigit).count();
        if (digits < PHONE_DIGITS_MIN || digits > PHONE_DIGITS_MAX) {
            throw new BusinessException("Телефон: должно быть от " + PHONE_DIGITS_MIN + " до "
                    + PHONE_DIGITS_MAX + " цифр (сейчас " + digits + ")");
        }
        return (v.startsWith("+") ? "+" : "") + v.replaceAll("[^0-9]", "");
    }

    /** Email необязателен: пустое значение превращается в null. */
    static String emailOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String v = value.trim().toLowerCase(Locale.ROOT);
        if (v.length() > EMAIL_MAX) {
            throw new BusinessException("Email: слишком длинное значение (максимум " + EMAIL_MAX + " символов)");
        }
        if (!EMAIL.matcher(v).matches()) {
            throw new BusinessException("Email: неверный формат, ожидается вид name@example.com");
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

    /** Стоимость: не null, не отрицательная, не больше предела NUMERIC(10,2), максимум 2 знака после запятой. */
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
