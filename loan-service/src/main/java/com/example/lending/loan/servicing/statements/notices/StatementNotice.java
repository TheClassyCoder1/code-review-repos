package com.example.lending.loan.servicing.statements.notices;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Notice printed on statements of a board (branch closures, rate changes, regulatory text). */
@Entity
@Table(name = "servicing_statement_notices", schema = "lending")
public class StatementNotice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "board_id", nullable = false)
    private String boardId;
    @Column(name = "name", nullable = false, unique = true)
    private String name;
    @Column(name = "author", nullable = false)
    private String author;
    @Column(name = "text", nullable = false, length = 8000)
    private String text;
    @Column(name = "published", nullable = false)
    private boolean published;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getBoardId() { return boardId; }
    public void setBoardId(String boardId) { this.boardId = boardId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public boolean isPublished() { return published; }
    public void setPublished(boolean published) { this.published = published; }
}
