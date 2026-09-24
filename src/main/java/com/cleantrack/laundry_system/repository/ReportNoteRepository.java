package com.cleantrack.laundry_system.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cleantrack.laundry_system.model.ReportNote;

@Repository
public interface ReportNoteRepository extends JpaRepository<ReportNote, Long> {
    List<ReportNote> findAllByOrderByCreatedAtDesc();
}
