package com.rajani.makerchecker.repo;

import com.rajani.makerchecker.domain.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, String> {
    List<OutboxEvent> findByDeliveredFalseOrderByCreatedAtAsc();
}
