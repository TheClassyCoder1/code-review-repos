# Ticketing service

Rails 7 API for event ticketing: organisers publish events, attendees buy tickets,
and organisers handle refunds and payouts.

- `app/controllers` - public, organiser and admin endpoints
- `app/models` - Event, Ticket, Organiser, Refund, ApiKey
- `app/services` - PDF tickets, badge printing, payouts, webhooks, reports
