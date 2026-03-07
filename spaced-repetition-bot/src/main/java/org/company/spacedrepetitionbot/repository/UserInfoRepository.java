package org.company.spacedrepetitionbot.repository;

import org.company.spacedrepetitionbot.model.UserInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserInfoRepository extends JpaRepository<UserInfo, Long> {
    Optional<UserInfo> findByUserName(String userName);
}
