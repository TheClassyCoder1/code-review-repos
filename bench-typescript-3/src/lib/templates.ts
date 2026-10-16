import { escapeHtml } from './html';

export interface JobSummary {
  id: string;
  title: string;
  location: string;
  employmentType: string;
}

export interface InviteView {
  candidateName: string;
  jobTitle: string;
  interviewerName: string;
  when: string;
  location: string;
  rescheduleUrl: string;
}

function layout(title: string, body: string): string {
  return `<!doctype html>
<html lang="en">
<head><meta charset="utf-8"><title>${escapeHtml(title)}</title><link rel="stylesheet" href="/careers/static/site.css"></head>
<body>${body}</body>
</html>`;
}

export function renderJobCard(job: JobSummary): string {
  const href = `/careers/jobs/${encodeURIComponent(job.id)}`;
  return `<li class="job">
  <a href="${escapeHtml(href)}">${escapeHtml(job.title)}</a>
  <span class="meta">${escapeHtml(job.location)} &middot; ${escapeHtml(job.employmentType.replace('_', ' '))}</span>
</li>`;
}

export function renderJobList(jobs: JobSummary[]): string {
  const items = jobs.map((job) => renderJobCard(job)).join('\n');
  return layout('Open positions', `<h1>Open positions</h1><ul class="jobs">${items}</ul>`);
}

export function renderSearchResults(term: string, jobs: JobSummary[]): string {
  const items = jobs.map((job) => renderJobCard(job)).join('\n');
  const summary = jobs.length === 1 ? '1 open position' : `${jobs.length} open positions`;
  return layout(
    'Search jobs',
    `<h1>Results for "${term}"</h1>
<p class="summary">${summary}</p>
<ul class="jobs">${items}</ul>`,
  );
}

function footer(): string {
  const year = new Date().getUTCFullYear();
  return `<p class="footer">You received this email because you applied for a role with us. &copy; ${year}</p>`;
}

export function renderInterviewInvite(view: InviteView): string {
  return `<p>Hi ${view.candidateName},</p>
<p>Your interview for <strong>${view.jobTitle}</strong> with ${view.interviewerName} is scheduled for ${view.when}.</p>
<p>Location: ${view.location}</p>
<p>Need a different time? <a href="${view.rescheduleUrl}">Pick a new slot</a>.</p>
${footer()}`;
}
