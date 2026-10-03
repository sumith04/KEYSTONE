package com.keystone.repository;

import com.keystone.entity.Notification;
import com.keystone.enums.NotificationType;
import com.keystone.enums.RelatedEntityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("SELECT n FROM Notification n WHERE n.recipient.id = :recipientId AND " +
           "(:read IS NULL OR n.read = :read) AND " +
           "(:type IS NULL OR n.type = :type)")
    Page<Notification> searchForRecipient(
            @Param("recipientId") Long recipientId,
            @Param("read") Boolean read,
            @Param("type") NotificationType type,
            Pageable pageable
    );

    long countByRecipientIdAndReadFalse(Long recipientId);

    Optional<Notification> findByIdAndRecipientId(Long id, Long recipientId);

    boolean existsByRecipientIdAndTypeAndRelatedEntityTypeAndRelatedEntityId(
            Long recipientId,
            NotificationType type,
            RelatedEntityType relatedEntityType,
            Long relatedEntityId
    );

    boolean existsByRecipientIdAndTypeAndRelatedEntityTypeAndRelatedEntityIdAndReadFalse(
            Long recipientId,
            NotificationType type,
            RelatedEntityType relatedEntityType,
            Long relatedEntityId
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Notification n SET n.read = true, n.readAt = :readAt " +
           "WHERE n.recipient.id = :recipientId AND n.read = false")
    int markAllReadForRecipient(
            @Param("recipientId") Long recipientId,
            @Param("readAt") LocalDateTime readAt
    );
}
