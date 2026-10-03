package com.keystone.service;

import com.keystone.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReportingDateRangeTest {

    @Test
    void parse_WhenBlank_ShouldLeaveUnbounded() {
        ReportingDateRange range = ReportingDateRange.parse(null, "  ");
        assertNull(range.from());
        assertNull(range.to());
    }

    @Test
    void parse_WhenValidIsoDates_ShouldUseInclusiveDayBounds() {
        ReportingDateRange range = ReportingDateRange.parse("2026-01-01", "2026-01-31");
        assertEquals(LocalDate.of(2026, 1, 1).atStartOfDay(), range.from());
        assertEquals(LocalDate.of(2026, 1, 31).atTime(LocalTime.MAX), range.to());
    }

    @Test
    void parse_WhenFromAfterTo_ShouldReturn400() {
        ApiException exception = assertThrows(ApiException.class, () -> ReportingDateRange.parse("2026-02-01", "2026-01-01"));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void parse_WhenInvalidFormat_ShouldReturn400() {
        ApiException exception = assertThrows(ApiException.class, () -> ReportingDateRange.parse("01-01-2026", null));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }
}
