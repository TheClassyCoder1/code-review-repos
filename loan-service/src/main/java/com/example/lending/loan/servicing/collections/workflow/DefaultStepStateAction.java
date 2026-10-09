package com.example.lending.loan.servicing.collections.workflow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** State action used by the standard collections workflows. */
@Component
public class DefaultStepStateAction extends AbstractStepStateAction {

    private static final Logger log = LoggerFactory.getLogger(DefaultStepStateAction.class);

    @Override
    protected void onStateChanged(CaseExecution caseExecution, StepExecution stepExecution) {
        log.info("Case {} step {} is now {}", caseExecution.getCaseId(), stepExecution.getStepKey(),
                stepExecution.getState());
    }
}
