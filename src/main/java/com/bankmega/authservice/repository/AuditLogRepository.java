package com.bankmega.authservice.repository;

import com.bankmega.authservice.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT al FROM AuditLog al WHERE al.createdAt >= :from AND al.createdAt <= :to")
    List<AuditLog> findByDateRange(@Param("from") LocalDateTime from,
                                    @Param("to") LocalDateTime to);
}
