package com.tinsae.socialmediaplatform.common.util;

import com.tinsae.socialmediaplatform.common.exception.BusinessRuleException;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.function.Function;

public final class CursorUtils {

    private CursorUtils() {
    }

    public static Instant parseCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }

        try {
            return Instant.parse(cursor);
        } catch (DateTimeParseException exception) {
            throw new BusinessRuleException("Invalid cursor format. Use ISO-8601 format.");
        }
    }

    public static <T> String trimAndNextCursor(List<T> items, int limit, Function<T, Instant> cursorExtractor) {
        if (items.size() <= limit) {
            return null;
        }

        items.remove(items.size() - 1);
        Instant nextCursor = cursorExtractor.apply(items.get(items.size() - 1));
        return nextCursor != null ? nextCursor.toString() : null;
    }
}
