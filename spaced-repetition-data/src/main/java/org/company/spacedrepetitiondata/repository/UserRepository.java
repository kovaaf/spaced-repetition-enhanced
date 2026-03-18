package org.company.spacedrepetitiondata.repository;

import org.company.spacedrepetitiondata.model.User;

import java.util.List;

/**
 * Repository for users.
 */
public interface UserRepository {
    /**
     * Retrieves all users.
     *
     * @return list of users
     */
    List<User> findAll();
}