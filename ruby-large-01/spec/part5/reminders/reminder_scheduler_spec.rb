require_relative "../../../lib/doc_intake/part5/reminders/reminder_scheduler"

RSpec.describe DocIntake::Part5::Reminders::ReminderScheduler do
  let(:now) { Time.utc(2026, 10, 10, 12, 0, 0) }
  let(:scheduler) { described_class.new(clock: -> { now }) }
  let(:created_at) { Time.utc(2026, 10, 1, 10, 0, 0) }

  it "schedules the first reminder a day after creation" do
    at = scheduler.next_reminder_at(created_at: created_at, last_sent_at: nil,
                                    sent_count: 0, utc_offset_seconds: 0)
    expect(at).to eq(created_at + 86_400)
  end

  it "moves evening reminders to the next local morning" do
    evening = Time.utc(2026, 10, 2, 20, 30, 0)
    shifted = scheduler.shift_out_of_quiet_hours(evening, 3600)
    expect(shifted).to eq(Time.utc(2026, 10, 3, 7, 0, 0))
  end

  it "moves early-morning reminders to the same local morning" do
    early = Time.utc(2026, 10, 2, 3, 0, 0)
    expect(scheduler.shift_out_of_quiet_hours(early, 0)).to eq(Time.utc(2026, 10, 2, 8, 0, 0))
  end

  it "stops after the last back-off step" do
    at = scheduler.next_reminder_at(created_at: created_at, last_sent_at: now,
                                    sent_count: described_class::MAX_REMINDERS,
                                    utc_offset_seconds: 0)
    expect(at).to be_nil
  end

  it "returns due reminders for incomplete, opted-in applications only" do
    apps = [
      { id: "app-1", created_at: created_at, reminders_sent: 0 },
      { id: "app-2", created_at: created_at, reminders_sent: 0, pack_complete: true },
      { id: "app-3", created_at: created_at, reminders_sent: 0, reminders_opted_out: true },
      { id: "app-4", created_at: now, reminders_sent: 0 }
    ]

    due = scheduler.due_reminders(apps)
    expect(due.map(&:application_id)).to eq(["app-1"])
    expect(due.first.sequence).to eq(1)
  end
end
