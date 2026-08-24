package com.sping.user.repository;

import com.sping.common.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByUserName(String userName);

    boolean existsByUserName(String userName);

    @Query(value = "SELECT MIN(next_id) FROM (" +
            "SELECT 1 AS next_id WHERE NOT EXISTS (SELECT 1 FROM user WHERE userId = 1) " +
            "UNION ALL " +
            "SELECT t1.userId + 1 AS next_id FROM user t1 " +
            "WHERE t1.userId + 1 NOT IN (SELECT userId FROM user)" +
            ") t", nativeQuery = true)
    Integer findMinAvailableUserId();
}