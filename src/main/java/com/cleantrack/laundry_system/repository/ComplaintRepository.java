package com.cleantrack.laundry_system.repository;

import com.cleantrack.laundry_system.model.Complaint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long> {
    List<Complaint> findByStatus(String status);
    List<Complaint> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
    List<Complaint> findByAssignedStaffIdOrderByCreatedAtDesc(Long staffId);
    List<Complaint> findAllByOrderByCreatedAtDesc();
    long countByStatus(String status);
}
