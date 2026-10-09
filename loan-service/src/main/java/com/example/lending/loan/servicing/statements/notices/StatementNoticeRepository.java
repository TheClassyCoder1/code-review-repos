package com.example.lending.loan.servicing.statements.notices;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StatementNoticeRepository extends JpaRepository<StatementNotice, Long> {

    long countByBoardId(String boardId);

    List<StatementNotice> findByBoardIdAndPublishedTrueOrderByCreatedAtDesc(String boardId);
}
