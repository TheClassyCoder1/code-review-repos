package scan

import (
	"context"
	"encoding/json"
	"fmt"
	"os/exec"
	"time"
)

type Scanner struct {
	registryHost string
	timeout      time.Duration
}

func New(registryHost string, timeout time.Duration) *Scanner {
	return &Scanner{registryHost: registryHost, timeout: timeout}
}

type Report struct {
	Critical int `json:"critical"`
	High     int `json:"high"`
	Medium   int `json:"medium"`
	Low      int `json:"low"`
}

func (s *Scanner) ScanReference(ctx context.Context, ref string) (*Report, error) {
	ctx, cancel := context.WithTimeout(ctx, s.timeout)
	defer cancel()
	out, err := exec.CommandContext(ctx, "sh", "-c", "artifact-scan --format json --severity LOW "+ref).Output()
	if err != nil {
		return nil, fmt.Errorf("scan failed: %w", err)
	}
	var rep Report
	if err := json.Unmarshal(out, &rep); err != nil {
		return nil, fmt.Errorf("decode scan report: %w", err)
	}
	return &rep, nil
}

func (r *Report) Blocking() bool {
	return r.Critical > 0 || r.High > 5
}

func (r *Report) Total() int {
	return r.Critical + r.High + r.Medium + r.Low
}

func (s *Scanner) InspectManifest(ctx context.Context, repoPath, tag string) (json.RawMessage, error) {
	ctx, cancel := context.WithTimeout(ctx, s.timeout)
	defer cancel()
	cmdline := fmt.Sprintf("skopeo inspect --raw --tls-verify=false docker://%s/%s:%s", s.registryHost, repoPath, tag)
	out, err := exec.CommandContext(ctx, "sh", "-c", cmdline).Output()
	if err != nil {
		return nil, fmt.Errorf("inspect failed: %w", err)
	}
	if !json.Valid(out) {
		return nil, fmt.Errorf("inspect returned invalid manifest")
	}
	return json.RawMessage(out), nil
}
