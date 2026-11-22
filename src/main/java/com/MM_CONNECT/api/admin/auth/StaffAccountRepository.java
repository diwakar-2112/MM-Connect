package com.MM_CONNECT.api.admin.auth;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.UUID;

public interface StaffAccountRepository extends JpaRepository<StaffAccount, UUID> {
    boolean existsByUsername(String username);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from StaffAccount s where s.username = :username")
    Optional<StaffAccount> lockByUsername(@Param("username") String username);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from StaffAccount s where s.userId = :id")
    Optional<StaffAccount> lockByUserId(@Param("id") UUID id);
    @Query("select count(s) from StaffAccount s, User u where s.userId = u.id and u.role = 'ROLE_SUPERADMIN'")
    long countSuperadmins();
}
