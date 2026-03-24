package org.company.data.service.usecase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.company.data.model.User;
import org.company.data.repository.UserRepository;

import java.util.List;

/**
 * Use case for retrieving all users.
 */
@Slf4j
@RequiredArgsConstructor
public class GetUsersUseCase {
    private final UserRepository userRepository;

    /**
     * Executes the use case.
     *
     * @return list of all users
     */
    public List<User> execute() {
        log.info("Retrieving all users");
        return userRepository.findAll();
    }
}