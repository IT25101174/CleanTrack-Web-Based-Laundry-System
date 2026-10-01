package com.cleantrack.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "status_updates")
public class StatusUpdate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false, length = 50)
    private String stage;

    @ManyToOne(optional = false)
    @JoinColumn(name = "updated_by", nullable = false)
    private User updatedBy;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private LocalDateTime changedAt = LocalDateTime.now();

    // Optional free-text note (used on the record created by a revert: "Reverted from X. Reason: ...")
    @Column(name = "note", length = 255)
    private String note;

    // Set when a Branch Supervisor / Admin reverts the stage this record created.
    // Nullable so records saved before this feature existed simply count as "not reverted".
    @Column(name = "reverted")
    private Boolean reverted = false;

    @Column(name = "revert_reason", length = 200)
    private String revertReason;

    @ManyToOne
    @JoinColumn(name = "reverted_by")
    private User revertedBy;

    @Column(name = "reverted_at")
    private LocalDateTime revertedAt;

    public StatusUpdate() {
    }

    public StatusUpdate(Order order, String stage, User updatedBy) {
        this.order = order;
        this.stage = stage;
        this.updatedBy = updatedBy;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public String getStage() {
        return stage;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }

    public User getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(User updatedBy) {
        this.updatedBy = updatedBy;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(LocalDateTime changedAt) {
        this.changedAt = changedAt;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public boolean isReverted() {
        return Boolean.TRUE.equals(reverted);
    }

    public void setReverted(boolean reverted) {
        this.reverted = reverted;
    }

    public String getRevertReason() {
        return revertReason;
    }

    public void setRevertReason(String revertReason) {
        this.revertReason = revertReason;
    }

    public User getRevertedBy() {
        return revertedBy;
    }

    public void setRevertedBy(User revertedBy) {
        this.revertedBy = revertedBy;
    }

    public LocalDateTime getRevertedAt() {
        return revertedAt;
    }

    public void setRevertedAt(LocalDateTime revertedAt) {
        this.revertedAt = revertedAt;
    }
}
