package org.company.spacedrepetitiondata.repository;

import java.util.Objects;

/**
 * Represents a user from the bot.user_info table.
 * Immutable value object.
 */
public record UserRecord(Long id, String name) {

    public UserRecord(Long id, String name) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.name = name; // nullable (some users may not have name)
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UserRecord that = (UserRecord) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "UserRecord{" + "id=" + id + ", name='" + name + '\'' + '}';
    }
}