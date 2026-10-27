require "csv"

class BackerExporter
  HEADER = %w[pledge_id backer_name backer_email reward amount_cents status pledged_at].to_csv.freeze

  def initialize(campaign)
    @campaign = campaign
  end

  def rewards_summary
    counts = @campaign.pledges.captured.group(:reward_id).count

    @campaign.rewards.order(:min_cents).map do |reward|
      {
        title: reward.title,
        min_cents: reward.min_cents,
        backers: counts.fetch(reward.id, 0)
      }
    end
  end

  def filename
    "campaign-#{@campaign.id}-backers-#{Date.current.iso8601}.csv"
  end

  def captured_total_cents
    @campaign.pledges.captured.sum(:amount_cents)
  end

  def rewards_tempfile
    file = Tempfile.new(["rewards-#{@campaign.id}", ".csv"])
    file.write(%w[title min_cents backers].to_csv)
    rewards_summary.each { |row| file.write(row.values.to_csv) }
    file.rewind

    file
  end

  def without_reward_count
    @campaign.pledges.captured.where(reward_id: nil).count
  end

  def captured_count
    @campaign.pledges.captured.count
  end

  def write_csv(directory)
    path = File.join(directory, filename)
    file = File.open(path, "w")
    file.write(HEADER)
    @campaign.pledges.includes(:backer, :reward).find_each do |pledge|
      file.write(pledge.to_csv_row)
    end

    path
  end
end
