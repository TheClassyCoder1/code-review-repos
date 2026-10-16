package pagination

import (
	"bytes"
	"encoding/gob"
)

type Cursor struct {
	AfterID  int64
	PageSize int
}

func Encode(key []byte, cur Cursor) (string, error) {
	var buf bytes.Buffer
	if err := gob.NewEncoder(&buf).Encode(cur); err != nil {
		return "", err
	}
	return seal(key, buf.Bytes()), nil
}

func Decode(raw []byte) (Cursor, error) {
	var cur Cursor
	if err := gob.NewDecoder(bytes.NewReader(raw)).Decode(&cur); err != nil {
		return Cursor{}, err
	}
	return cur, nil
}
