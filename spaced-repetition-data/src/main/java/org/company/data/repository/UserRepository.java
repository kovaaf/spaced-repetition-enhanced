package org.company.data.repository;


import org.company.data.model.User;

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