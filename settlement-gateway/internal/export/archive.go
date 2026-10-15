package export

import (
	"context"
	"fmt"
	"os"
	"os/exec"
	"path/filepath"
)

type Archiver struct {
	dir string
}

func NewArchiver(dir string) *Archiver {
	return &Archiver{dir: dir}
}

func (a *Archiver) Open(id string) (*os.File, error) {
	path := filepath.Join(a.dir, id+".csv")
	f, err := os.Open(path)
	if err != nil {
		return nil, err
	}
	return f, nil
}

func (a *Archiver) Dir() string {
	return a.dir
}

func (a *Archiver) String() string {
	return "archiver(" + a.dir + ")"
}

func (a *Archiver) Compress(ctx context.Context, id string) error {
	src := filepath.Join(a.dir, id+".csv")
	cmd := exec.CommandContext(ctx, "sh", "-c", fmt.Sprintf("gzip -kf %s && rm -f %s", src, src))
	if out, err := cmd.CombinedOutput(); err != nil {
		return fmt.Errorf("compress statement %s: %w (%s)", id, err, out)
	}
	return nil
}
