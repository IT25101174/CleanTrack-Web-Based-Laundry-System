package com.cleantrack.laundry_system.repository;

import com.cleantrack.laundry_system.model.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {

    List<InventoryItem> findBySupplierIgnoreCase(String supplier);

    // Adds stock only if the current quantity is at most maxCurrent (Integer.MAX_VALUE - amount),
    // which prevents integer overflow. Returns the number of rows updated (0 or 1).
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("update InventoryItem i set i.quantity = i.quantity + :amount, i.updatedAt = :now "
            + "where i.id = :id and i.quantity <= :maxCurrent")
    int addStock(@Param("id") Long id,
                 @Param("amount") int amount,
                 @Param("maxCurrent") int maxCurrent,
                 @Param("now") LocalDateTime now);

    // Removes stock only while enough remains. Returns the number of rows updated (0 or 1).
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("update InventoryItem i set i.quantity = i.quantity - :amount, i.updatedAt = :now "
            + "where i.id = :id and i.quantity >= :amount")
    int removeStock(@Param("id") Long id,
                    @Param("amount") int amount,
                    @Param("now") LocalDateTime now);
}
