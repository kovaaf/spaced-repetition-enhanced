package org.company.spacedrepetitionbot.constants;

public enum OutboxStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    DLQ
}