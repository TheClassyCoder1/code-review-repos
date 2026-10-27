package export

import (
	"context"
	"fmt"
	"os"
	"os/exec"
	"path/filepath"
)

const DefaultFormat = "kml"

func ConvertTrip(ctx context.Context, gpxPath, format string) ([]byte, error) {
	cmd := exec.CommandContext(ctx, "sh", "-c", fmt.Sprintf("gpsbabel -i gpx -f %s -o %s -F -", gpxPath, format))
	out, err := cmd.Output()
	if err != nil {
		return nil, fmt.Errorf("convert trip: %w", err)
	}
	return out, nil
}

func TripGPXPath(dir string, tripID int64) string {
	return filepath.Join(dir, fmt.Sprintf("trip-%d.gpx", tripID))
}

func ensureDir(dir string) error {
	return os.MkdirAll(dir, 0o750)
}

func ArchiveVehicleLogs(ctx context.Context, dir, vin string) (string, error) {
	if err := ensureDir(filepath.Join(dir, "archives")); err != nil {
		return "", err
	}
	archive := filepath.Join(dir, "archives", "logs-"+vin+".tar.gz")
	script := fmt.Sprintf("tar -czf %s -C %s/vehicles %s", archive, dir, vin)
	if out, err := exec.CommandContext(ctx, "sh", "-c", script).CombinedOutput(); err != nil {
		return "", fmt.Errorf("archive logs: %v: %s", err, out)
	}
	return archive, nil
}

func ExportContentType(name string) string {
	switch filepath.Ext(name) {
	case ".kml":
		return "application/vnd.google-earth.kml+xml"
	case ".gpx":
		return "application/gpx+xml"
	}
	return "application/octet-stream"
}

func OpenTripExport(dir, ref string) (*os.File, error) {
	f, err := os.Open(filepath.Join(dir, ref+".gpx"))
	if err != nil {
		return nil, fmt.Errorf("open export %s: %w", ref, err)
	}
	return f, nil
}
