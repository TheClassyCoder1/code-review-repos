package com.example.lending.loan.servicing.statements.notices;

import com.example.lending.loan.servicing.common.ServicingResult;
import com.example.lending.loan.servicing.security.OperatorPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Statement notice boards. */
@RestController
@RequestMapping("/servicing/statements/notice-boards")
public class StatementNoticeController {

    private static final Logger log = LoggerFactory.getLogger(StatementNoticeController.class);

    private final StatementNoticePublisher publisher;

    public StatementNoticeController(StatementNoticePublisher publisher) {
        this.publisher = publisher;
    }

    @PostMapping("/{boardId}/notices")
    public ServicingResult<String> post(@PathVariable String boardId,
                                        @RequestBody Map<String, Object> content,
                                        @RequestParam(defaultValue = "false") boolean publish,
                                        @AuthenticationPrincipal OperatorPrincipal operator) throws NoticePublishException {
        return ServicingResult.ok(publisher.newPost(boardId, operator, content, publish));
    }

    @ExceptionHandler(NoticePublishException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ServicingResult<Void> handlePublishFailure(NoticePublishException e) {
        log.warn("Notice publishing failed with code {}", e.getCode());
        return ServicingResult.failed("The notice could not be published. Please try again later.");
    }
}
