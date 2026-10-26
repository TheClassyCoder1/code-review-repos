import json, os, sys
root = sys.argv[1]
B = 'typescript-large-01/part3/src/'
cases = [
("syn-typescript-check_then_act_race-D11-1", B+"disbursement/disbursement-service.ts", """    const existing = await this.store.find(cmd.idempotencyKey);
    if (existing) {
      this.log.info('disbursement replayed', { loanId: cmd.loanId, payoutId: existing.gatewayPayoutId });
      return existing;
    }
    const receipt = await this.gateway.submitPayout({""", None),
("syn-typescript-lost_update-D12-1", B+"accounts/fee-assessment-service.ts", """    const account = await this.accounts.load(accountId);
    if (account.balanceMinor <= 0) {
      return 0;
    }
    const fee = await this.feePolicy.lateFeeFor(account, asOf);
    if (fee <= 0) {
      return 0;
    }
    account.feesMinor += fee;
    account.balanceMinor += fee;
    await this.accounts.overwrite(account);""", None),
("syn-typescript-lost_update-D13-1", B+"collections/collections-tally.ts", """    const current = this.totals.get(event.agentId) ?? 0;
    await this.audit.append({
      type: 'RECOVERY',
      agentId: event.agentId,
      loanId: event.loanId,
      amountMinor: event.amountMinor,
      eventId: event.eventId,
    });
    this.totals.set(event.agentId, current + event.amountMinor);""", None),
("syn-typescript-broken_exception_handler-D14-1", B+"collections/charge-off-service.ts", """    } catch (err) {
      this.log.warn('charge-off step did not complete', { loanId, error: describeError(err) });
    }
    this.log.info('loan charged off', { loanId, dpd });
    return { loanId, status: 'CHARGED_OFF', writtenOffMinor: balance };""", None),
("syn-typescript-endless_loop-D15-1", B+"partners/partner-feed.ts", """    do {
      const page = await this.fetchPage(cursor);
      all.push(...page.items);
      pages++;
      cursor = page.nextCursor;
    } while (cursor !== null);""", None),
("syn-typescript-no_network_timeout-D16-1", B+"clients/payment-gateway-client.ts", """    const res = await fetch(`${this.baseUrl}/v2/payouts`, {
      method: 'POST',
      headers: this.headers(),
      body: JSON.stringify(req),
    });""", None),
("syn-typescript-broken_exception_handler-D17-1", B+"reconciliation/reconciler.ts", """    const statement = await this.bank.fetchStatement(date).catch((err: unknown) => {
      this.log.error('bank statement fetch failed', { date });
      new ReconciliationError(`bank statement unavailable for ${date}`, err);
    });
    if (!statement) {
      return { statementDate: date, entries: 0, attempted: 0 };
    }""", None),
("syn-typescript-check_then_act_race-D18-1", B+"locks/file-lock.ts", """    if (existsSync(path)) {
      if (!this.isStale(path)) {
        return null;
      }
      unlinkSync(path);
    }
    writeFileSync(path, `${hostname()}:${process.pid}:${Date.now()}`);""", None),
("syn-typescript-endless_loop-D19-1", B+"schedule/batch-runner.ts", """  while (i < items.length) {
    const item = items[i];
    if (item.amountMinor <= 0 || item.mandateId === null) {
      result.skipped.push(item.installmentId);
      continue;
    }""", None),
("syn-typescript-endless_loop-D20-1", B+"hardship/hardship-plan.ts", """    while (balance > 0) {
      const interest = Math.round(balance * rate);
      interestTotal += interest;
      balance = balance + interest - req.affordablePaymentMinor;
      months++;
    }""", None),
("syn-typescript-resource_leak-T1-1", B+"statements/statement-export.ts", """  const out = createWriteStream(statementPath(dir, accountId, period), { flags: 'w', encoding: 'utf8' });
  out.write(STATEMENT_HEADER);
  return out;""", B+"statements/statement-job.ts"),
("syn-typescript-unbounded_memory-T2-1", B+"pricing/rate-quote-service.ts", """    this.quotes.set(key, quote);""", B+"util/bounded-map.ts"),
("syn-typescript-unbounded_memory-T3-1", B+"collections/delinquency-collector.ts", """    const loans: DelinquentLoan[] = [];
    for await (const page of pages) {
      for (const loan of page.loans) {
        loans.push(loan);
      }
    }""", B+"jobs/delinquency-job.ts"),
("syn-typescript-missing_value_used-T4-1", B+"planning/installment-registry.ts", """    const plan = this.plans.get(loanId)!;
    return plan.installments.find((i) => !i.paid) ?? null;""", B+"planning/next-due-service.ts"),
("syn-typescript-broken_exception_handler-T5-1", B+"collections/dunning-service.ts", """    try {
      await this.notices.dispatch(row);
    } catch (err) {
      this.log.debug('dunning notice delivery deferred', { loanId: candidate.loanId, error: describeError(err) });
    }""", B+"notifications/outbox.ts"),
("syn-typescript-missing_value_used-T6-1", B+"pricing/rate-quote-service.ts", """    const row = this.rateTable.find((r) => r.tenorMonths === req.tenorMonths)!;
    const aprBps = row.baseAprBps + TIER_SPREAD_BPS[req.tier];""", B+"pricing/quote-request.ts"),
("syn-typescript-resource_leak-T7-1", B+"jobs/accrual-job.ts", """    const conn = await this.pool.acquire();""", B+"db/scoped-pool.ts"),
("syn-typescript-endless_loop-T8-1", B+"schedule/amortization.ts", """  while (balance > 0) {
    period++;
    const interest = Math.round(balance * r);
    const payment = Math.min(terms.paymentMinor, balance + interest);
    const principal = payment - interest;
    balance -= principal;""", B+"schedule/schedule-service.ts"),
("syn-typescript-unbounded_memory-T9-1", B+"intake/document-parser.ts", """  const raw = await readFile(path);
  const rows = raw.toString('utf8').split(/\\r?\\n/).filter((r) => r.trim().length > 0);""", B+"intake/document-intake.ts"),
("syn-typescript-broken_exception_handler-T10-1", B+"reconciliation/reconciler.ts", """    await Promise.all(entries.map((entry) => this.matchEntry(entry).catch(() => undefined)));""", B+"reconciliation/dead-letter.ts"),
("syn-typescript-no_network_timeout-T11-1", B+"clients/bureau-client.ts", """    const res = await this.fetchImpl(`${this.baseUrl}/v1/reports/${encodeURIComponent(borrowerRef)}?mode=soft`, {
      headers: { accept: 'application/json' },
    });""", B+"http/timed-fetch.ts"),
("syn-typescript-broken_exception_handler-T12-1", B+"settlement/settlement-status.ts", """    } catch (err) {
      this.log.debug('settlement lookup failed', { batchId, error: describeError(err) });
      return 'PENDING';
    }""", B+"settlement/settlement-poller.ts"),
("syn-typescript-no_network_timeout-T13-1", B+"clients/ledger-client.ts", """    const res = await fetch(`${this.baseUrl}/v1/journal/entries`, {
      ...init,
      method: 'POST',
      headers: { 'content-type': 'application/json', 'idempotency-key': entry.entryRef },
      body: JSON.stringify(entry),
    });""", B+"ledger/ledger-sync.ts"),
("syn-typescript-lost_update-T14-1", B+"accounts/payment-posting-service.ts", """    const account = await this.accounts.load(payment.accountId);
    const toFees = Math.min(account.feesMinor, payment.amountMinor);
    const toPrincipal = Math.min(account.balanceMinor, payment.amountMinor - toFees);
    account.feesMinor -= toFees;
    account.balanceMinor -= toPrincipal;
    account.paidToDateMinor += payment.amountMinor;
    account.lastPaymentAt = payment.receivedAt.toISOString();
    return this.accounts.save(account);""", B+"accounts/servicing-account-repository.ts"),
("syn-typescript-endless_loop-T15-1", B+"schedule/billing-calendar.ts", """  while (cursor < end) {
    dates.push(cursor);
    cursor = nextBillingDate(cursor, frequency);
  }""", B+"schedule/billing-plan.ts"),
]
ok = True
for cid, f, snip, g in cases:
    s = open(os.path.join(root, f)).read()
    n = s.count(snip + "\n")
    if n != 1 or (g and not os.path.exists(os.path.join(root, g))):
        ok = False
        print("BAD", cid, n)
print("all ok" if ok else "problems", len(cases))
json.dump([{"case_id": c, "file": f, "snippet": s, "guard_file": g} for c, f, s, g in cases], sys.stdout if False else open(os.path.join(root, '.p3tsc/cases.json'), 'w'), indent=1)
