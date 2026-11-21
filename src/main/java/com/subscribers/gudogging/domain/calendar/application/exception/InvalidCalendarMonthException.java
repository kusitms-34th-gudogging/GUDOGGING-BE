package com.subscribers.gudogging.domain.calendar.application.exception;

import com.subscribers.gudogging.global.common.exception.ApplicationException;

public class InvalidCalendarMonthException extends ApplicationException {

    public InvalidCalendarMonthException() {
        super(CalendarErrorCode.INVALID_CALENDAR_MONTH);
    }
}
