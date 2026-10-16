package money

import "fmt"

// Micros is an amount in millionths of a US dollar.
type Micros int64

// PerDollar is the number of micros in one dollar.
const PerDollar Micros = 1_000_000

// CostForImpressions returns the cost of n impressions at the given CPM,
// rounded half-up to the nearest micro.
func CostForImpressions(cpm Micros, impressions int64) Micros {
	if impressions <= 0 || cpm <= 0 {
		return 0
	}
	total := int64(cpm) * impressions
	q, r := total/1000, total%1000
	if r*2 >= 1000 {
		q++
	}
	return Micros(q)
}

// Dollars returns the whole-dollar part of m.
func (m Micros) Dollars() int64 {
	return int64(m / PerDollar)
}

// String formats m as US dollars rounded to the cent, e.g. "$12.34".
func (m Micros) String() string {
	sign := ""
	v := int64(m)
	if v < 0 {
		sign = "-"
		v = -v
	}
	cents := (v + 5000) / 10000
	return fmt.Sprintf("%s$%d.%02d", sign, cents/100, cents%100)
}

// LineItem is one campaign's delivery on an invoice.
type LineItem struct {
	CampaignID  int64 `json:"campaign_id"`
	Impressions int64 `json:"impressions"`
	CPMMicros   int64 `json:"cpm_micros"`
}

// Invoice is a monthly bill for one advertiser.
type Invoice struct {
	AdvertiserID int64      `json:"advertiser_id"`
	Lines        []LineItem `json:"lines"`
	TotalCents   int64      `json:"total_cents"`
}

// TotalCents sums the billable amount of all line items in cents.
func TotalCents(lines []LineItem) int64 {
	var dollars float64
	for _, l := range lines {
		cpm := float64(l.CPMMicros) / 1e6
		dollars += float64(l.Impressions) / 1000 * cpm
	}
	return int64(dollars * 100)
}
