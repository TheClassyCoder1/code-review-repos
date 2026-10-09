package com.example.lending.loan.servicing.statements.notices;

import com.example.lending.loan.servicing.common.ServicingException;
import com.example.lending.loan.servicing.security.OperatorPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Creates notices on a statement notice board. */
@Service
public class StatementNoticePublisher {

    /** Notice boards and the operators' permission needed to post on them. */
    public enum NoticeBoard {

        BRANCH("notice:branch"),
        RATES("notice:rates"),
        REGULATORY("notice:regulatory");

        private final String permission;

        NoticeBoard(String permission) {
            this.permission = permission;
        }

        public String permission() {
            return permission;
        }

        public static NoticeBoard of(String boardId) {
            for (NoticeBoard board : values()) {
                if (board.name().equalsIgnoreCase(boardId)) {
                    return board;
                }
            }
            return null;
        }

        public static Set<NoticeBoard> all() {
            return Set.of(values());
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(StatementNoticePublisher.class);

    /** Names new notice entries: {@code <board>_notice_<date>_<n>}. */
    static class NoticeEntryNamer {

        private final StatementNoticeRepository repository;

        NoticeEntryNamer(StatementNoticeRepository repository) {
            this.repository = repository;
        }

        String getNewEntryPage(NoticeBoard board) {
            long sequence = repository.countByBoardId(board.name()) + 1;
            return board.name().toLowerCase(Locale.ROOT) + "_notice_"
                    + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "_" + sequence;
        }
    }

    private final StatementNoticeRepository repository;

    public StatementNoticePublisher(StatementNoticeRepository repository) {
        this.repository = repository;
    }

    public String newPost(final String boardId,
                          final OperatorPrincipal operator,
                          final Map<String, Object> content,
                          final boolean publish) throws NoticePublishException {
        LOG.info("notice.newPost() called");
        final NoticeBoard board = NoticeBoard.of(boardId);
        checkPermissions(board, operator);

        final String pageName;
        try {
            final NoticeEntryNamer namer = new NoticeEntryNamer(repository);
            pageName = namer.getNewEntryPage(board);
            final StatementNotice entryPage = new StatementNotice();
            entryPage.setBoardId(board.name());
            entryPage.setName(pageName);
            entryPage.setAuthor(operator.getUsername());

            final StringBuilder text = new StringBuilder();
            text.append("!").append(content.get("title"));
            text.append("\n\n");
            text.append(content.get("description"));

            LOG.debug("Writing entry: " + text);

            entryPage.setText(text.toString());
            entryPage.setPublished(publish);
            repository.saveAndFlush(entryPage);
        } catch (final Exception e) {
            LOG.error("Failed to create notice entry", e);
            throw new NoticePublishException(0, "Failed to create notice entry: " + e.getMessage());
        }

        return pageName;
    }

    private void checkPermissions(NoticeBoard board, OperatorPrincipal operator) {
        if (board == null) {
            throw ServicingException.notFound("Unknown notice board");
        }
        if (!operator.hasAuthority(board.permission())) {
            throw ServicingException.forbidden("Not allowed to post on this board");
        }
    }
}
