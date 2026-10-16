import { prisma } from '../db';
import { escapeHtml } from '../lib/html';
import { InviteView, renderInterviewInvite } from '../lib/templates';
import { createSchedulingLink } from './inviteTokens';

interface InviteSource {
  candidateName: string;
  jobTitle: string;
  interviewerEmail: string;
  startsAt: Date;
  location: string;
  rescheduleUrl: string;
}

function toInviteView(source: InviteSource): InviteView {
  return {
    candidateName: escapeHtml(source.candidateName),
    jobTitle: escapeHtml(source.jobTitle),
    interviewerName: escapeHtml(source.interviewerEmail.split('@')[0]),
    when: escapeHtml(source.startsAt.toUTCString()),
    location: escapeHtml(source.location),
    rescheduleUrl: escapeHtml(source.rescheduleUrl),
  };
}

export async function listUpcomingInterviews(companyId: string, interviewerId?: string) {
  return prisma.interview.findMany({
    where: {
      status: 'scheduled',
      startsAt: { gte: new Date() },
      application: { job: { companyId } },
      ...(interviewerId ? { interviewerId } : {}),
    },
    orderBy: { startsAt: 'asc' },
    take: 100,
    include: { application: { include: { candidate: { select: { fullName: true } }, job: { select: { title: true } } } } },
  });
}

export async function buildInterviewInviteEmail(interviewId: string): Promise<{ to: string; html: string }> {
  const interview = await prisma.interview.findUniqueOrThrow({
    where: { id: interviewId },
    include: { application: { include: { candidate: true, job: true } } },
  });
  const interviewer = await prisma.recruiter.findUniqueOrThrow({ where: { id: interview.interviewerId } });
  const link = await createSchedulingLink(interview.id);
  const html = renderInterviewInvite(
    toInviteView({
      candidateName: interview.application.candidate.fullName,
      jobTitle: interview.application.job.title,
      interviewerEmail: interviewer.email,
      startsAt: interview.startsAt,
      location: interview.location,
      rescheduleUrl: link.url,
    }),
  );
  return { to: interview.application.candidate.email, html };
}
