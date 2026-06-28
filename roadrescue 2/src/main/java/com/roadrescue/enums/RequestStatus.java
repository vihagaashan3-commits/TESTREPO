package com.roadrescue.enums;

public enum RequestStatus {
    PENDING,        // Driver submitted request
    ACCEPTED,       // Garage accepted the request
    QUOTED,         // Garage sent payment quote to driver
    QUOTE_APPROVED, // Driver approved the quote
    IN_PROGRESS,    // Technician dispatched, work underway
    COMPLETED,      // Job done
    CANCELLED,      // Declined or rejected
    PAID            // Payment confirmed
}