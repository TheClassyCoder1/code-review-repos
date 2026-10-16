import { config } from '../config';
import { HttpError } from '../errors';

export interface BusySlot {
  start: string;
  end: string;
}

export interface CalendarEventInput {
  interviewId: string;
  interviewerEmail: string;
  candidateEmail: string;
  startsAt: Date;
  endsAt: Date;
  location: string;
}

export async function fetchAvailability(providerUrl: string, interviewerEmail: string, from: Date, to: Date) {
  const query = new URLSearchParams({ calendar: interviewerEmail, from: from.toISOString(), to: to.toISOString() });
  const response = await fetch(`${providerUrl.replace(/\/$/, '')}/v1/freebusy?${query}`, {
    headers: { authorization: `Bearer ${config.calendar.apiKey}` },
    signal: AbortSignal.timeout(config.calendar.timeoutMs),
  });
  if (!response.ok) {
    throw new HttpError(502, `calendar provider returned ${response.status}`);
  }
  const body = (await response.json()) as { busy?: BusySlot[] };
  return body.busy ?? [];
}

interface CreatedEvent {
  id: string;
  htmlLink?: string;
}

export async function createCalendarEvent(input: CalendarEventInput): Promise<CreatedEvent> {
  const response = await fetch(`${config.calendar.baseUrl}/v1/events`, {
    method: 'POST',
    headers: { 'content-type': 'application/json', authorization: `Bearer ${config.calendar.apiKey}` },
    body: JSON.stringify({
      externalId: input.interviewId,
      organizer: input.interviewerEmail,
      attendees: [input.interviewerEmail, input.candidateEmail],
      start: input.startsAt.toISOString(),
      end: input.endsAt.toISOString(),
      location: input.location,
    }),
    signal: AbortSignal.timeout(config.calendar.timeoutMs),
  });
  if (!response.ok) {
    throw new HttpError(502, `calendar provider returned ${response.status}`);
  }
  return (await response.json()) as CreatedEvent;
}
