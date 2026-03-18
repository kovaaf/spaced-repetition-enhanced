package org.company.spacedrepetitiondata.model;

import java.util.Objects;

/**
 * Domain entity representing a user.
 */
public record User(Long id, String name) {
    public User(Long id, String name) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.name = name; // nullable
    }
}