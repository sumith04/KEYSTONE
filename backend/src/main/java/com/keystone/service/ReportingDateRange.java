package com.keystone.service;

import com.keystone.exception.ApiException;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public final class ReportingDateRange {

    private final LocalDateTime from;
    private final LocalDateTime to;
    private final LocalDate fromDate;
    private final LocalDate toDate;

    private ReportingDateRange(LocalDate fromDate, LocalDate toDate) {
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.from = fromDate == null ? null : fromDate.atStartOfDay();
        this.to = toDate == null ? null : toDate.atTime(LocalTime.MAX);
    }

    public static ReportingDateRange parse(String fromValue, String toValue) {
        LocalDate fromDate = parseIsoDate(fromValue, "from");
        LocalDate toDate = parseIsoDate(toValue, "to");
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new ApiException("'from' must be less than or equal to 'to'.", HttpStatus.BAD_REQUEST);
        }
        return new ReportingDateRange(fromDate, toDate);
    }

    public static ReportingDateRange parseOrLastDays(String fromValue, String toValue, int days) {
        if (isBlank(fromValue) && isBlank(toValue)) {
            LocalDate end = LocalDate.now();
            return new ReportingDateRange(end.minusDays(Math.max(days, 1) - 1L), end);
        }
        return parse(fromValue, toValue);
    }

    private static LocalDate parseIsoDate(String value, String name) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim(), DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException ex) {
            throw new ApiException(name + " must be an ISO date (yyyy-MM-dd).", HttpStatus.BAD_REQUEST);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public LocalDateTime from() {
        return from;
    }

    public LocalDateTime to() {
        return to;
    }

    public LocalDate fromDate() {
        return fromDate;
    }

    public LocalDate toDate() {
        return toDate;
    }
}
