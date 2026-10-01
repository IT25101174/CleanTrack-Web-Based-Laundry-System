package com.cleantrack.repository;

import com.cleantrack.model.StatusUpdate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StatusUpdateRepository extends JpaRepository<StatusUpdate, Long> {
    List<StatusUpdate> findByOrderIdOrderByChangedAtDesc(Long orderId);
    List<StatusUpdate> findByOrderIdOrderByChangedAtAscIdAsc(Long orderId);
}
