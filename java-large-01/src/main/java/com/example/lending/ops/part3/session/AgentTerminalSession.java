package com.example.lending.ops.part3.session;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Per-agent session shared by all requests of that agent; written back to the store on every change. */
public class AgentTerminalSession {
	private final String agentId;
	private final AgentTerminalSessionStore store;
	private final List<SaTerminalInfo> terminalList = new CopyOnWriteArrayList<>();
	private int historyTerminalCount;
	AgentTerminalSession(String agentId, AgentTerminalSessionStore store) { this.agentId = agentId; this.store = store; }
	public String getAgentId() { return agentId; }
	public int terminalCount() { return terminalList.size(); }
	public SaTerminalInfo getTerminal(String tokenValue) {
		return terminalList.stream().filter(t -> t.getTokenValue().equals(tokenValue)).findFirst().orElse(null);
	}
	public void addTerminal(SaTerminalInfo terminalInfo) {
		// 根据 tokenValue 值查重，如果存在旧的，则先删除
		SaTerminalInfo oldTerminal = getTerminal(terminalInfo.getTokenValue());
		if(oldTerminal != null) {
			terminalList.remove(oldTerminal);
		}
		// 然后添加新的
		this.historyTerminalCount++;
		terminalInfo.setIndex(this.historyTerminalCount);
		terminalList.add(terminalInfo);
		update();
	}
	public void update() { store.save(this); }
}
