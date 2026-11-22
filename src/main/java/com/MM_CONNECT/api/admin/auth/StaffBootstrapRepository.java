package com.MM_CONNECT.api.admin.auth;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import java.util.Optional;

public interface StaffBootstrapRepository extends JpaRepository<StaffBootstrapState, Short> {
    @Override @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<StaffBootstrapState> findById(Short id);
}
