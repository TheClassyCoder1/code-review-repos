package storage

import (
	"crypto/sha256"
	"encoding/hex"
	"errors"
	"fmt"
	"io"
	"os"
	"path/filepath"
	"strings"
)

var ErrDigestMismatch = errors.New("digest mismatch")

type BlobStore struct {
	root     string
	maxBytes int64
}

func (b *BlobStore) Write(expected string, r io.Reader) (int64, error) {
	tmp, err := os.CreateTemp(filepath.Join(b.root, "uploads"), "blob-*")
	if err != nil {
		return 0, err
	}
	defer os.Remove(tmp.Name())
	h := sha256.New()
	n, err := io.Copy(io.MultiWriter(tmp, h), io.LimitReader(r, b.maxBytes+1))
	if cerr := tmp.Close(); err == nil {
		err = cerr
	}
	if err != nil {
		return 0, err
	}
	if n > b.maxBytes {
		return 0, fmt.Errorf("blob exceeds %d bytes", b.maxBytes)
	}
	if "sha256:"+hex.EncodeToString(h.Sum(nil)) != expected {
		return 0, ErrDigestMismatch
	}
	hexPart := strings.TrimPrefix(expected, "sha256:")
	dst := filepath.Join(b.root, "blobs", "sha256", hexPart[:2], hexPart)
	if err := os.MkdirAll(filepath.Dir(dst), 0o755); err != nil {
		return 0, err
	}
	return n, os.Rename(tmp.Name(), dst)
}

func New(root string, maxBytes int64) *BlobStore {
	return &BlobStore{
		root:     root,
		maxBytes: maxBytes,
	}
}

func (b *BlobStore) Open(digest string) (*os.File, int64, error) {
	hexPart := strings.TrimPrefix(digest, "sha256:")
	f, err := os.Open(filepath.Join(b.root, "blobs", "sha256", hexPart[:2], hexPart))
	if err != nil {
		return nil, 0, err
	}
	info, err := f.Stat()
	if err != nil {
		f.Close()
		return nil, 0, err
	}
	return f, info.Size(), nil
}
