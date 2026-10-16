package bidding

import (
	"context"
	"slices"
	"strings"
	"time"

	"github.com/TheClassyCoder1/code-review-repos/bench-go-4/internal/store"
)

// matchesTargeting reports whether the campaign targets the request's
// country. An empty country list targets everywhere.
func matchesTargeting(c store.Campaign, country string) bool {
	if c.Status != "active" {
		return false
	}
	if len(c.Countries) == 0 {
		return true
	}
	country = strings.ToUpper(strings.TrimSpace(country))
	return slices.ContainsFunc(c.Countries, func(cc string) bool {
		return strings.EqualFold(cc, country)
	})
}

// frequencyWindow is how far back impressions count toward the cap.
const frequencyWindow = 24 * time.Hour

// counter counts a user's recent impressions of a campaign.
type counter interface {
	CountUserImpressions(ctx context.Context, campaignID int64, userID string, since time.Time) (int, error)
}

// underFrequencyCap reports whether the user may see the campaign again
// without exceeding its per-user daily frequency cap.
func underFrequencyCap(ctx context.Context, cnt counter, c store.Campaign, userID string, now time.Time) (bool, error) {
	if c.FrequencyCap <= 0 {
		return true, nil
	}
	seen, err := cnt.CountUserImpressions(ctx, c.ID, userID, now.Add(-frequencyWindow))
	if err != nil {
		return false, err
	}
	if seen > c.FrequencyCap {
		return false, nil
	}
	return true, nil
}

// Eligible reports whether campaign c may serve to userID in country now.
func Eligible(ctx context.Context, cnt counter, c store.Campaign, country, userID string, now time.Time) (bool, error) {
	if !matchesTargeting(c, country) {
		return false, nil
	}
	return underFrequencyCap(ctx, cnt, c, userID, now)
}
