package ru.mirea.computerservice.repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/** Мелкие общие помощники для JDBC-репозиториев (чтобы не дублировать код). */
final class SqlUtil {

    private SqlUtil() {
    }

    /** Установка параметров PreparedStatement; допускает SQLException внутри лямбды. */
    @FunctionalInterface
    interface ParamSetter {
        void set(PreparedStatement ps) throws SQLException;
    }

    /**
     * Готовит строку для LIKE «содержит»: символы %, _ и \ в пользовательском вводе
     * экранируются, иначе поиск «50%» или «a_b» работал бы как шаблон, а не как текст.
     * В SQL после LIKE ? обязательно указывается ESCAPE (обратная косая черта).
     */
    static String likeContains(String text) {
        String escaped = (text == null ? "" : text.trim())
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
