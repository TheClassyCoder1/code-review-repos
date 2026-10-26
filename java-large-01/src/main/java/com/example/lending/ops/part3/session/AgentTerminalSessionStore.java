package com.example.lending.ops.part3.session;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** One terminal session per agent; concurrent sign-ins from several devices share the same session. */
@Slf4j
@Component
public class AgentTerminalSessionStore {
    private final Map<String, AgentTerminalSession> sessions = new ConcurrentHashMap<>();
    void save(AgentTerminalSession session) {
        sessions.put(session.getAgentId(), session);
        log.debug("agent session {} now has {} terminals", session.getAgentId(), session.terminalCount());
    }
    public SaTerminalInfo signIn(String agentId, String deviceType) {
        SaTerminalInfo terminal = new SaTerminalInfo(0, UUID.randomUUID().toString(), deviceType, System.currentTimeMillis());
        sessions.computeIfAbsent(agentId, id -> new AgentTerminalSession(id, this)).addTerminal(terminal);
        return terminal;
    }
}
