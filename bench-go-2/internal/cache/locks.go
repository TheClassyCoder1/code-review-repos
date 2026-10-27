package cache

import "sync"

type KeyedMutex struct {
	mu    sync.Mutex
	locks map[int64]*keyLock
}

type keyLock struct {
	mu   sync.Mutex
	refs int
}

func NewKeyedMutex() *KeyedMutex {
	return &KeyedMutex{locks: make(map[int64]*keyLock)}
}

func (k *KeyedMutex) Lock(key int64) func() {
	k.mu.Lock()
	l, ok := k.locks[key]
	if !ok {
		l = &keyLock{}
		k.locks[key] = l
	}
	l.refs++
	k.mu.Unlock()

	l.mu.Lock()
	return func() {
		l.mu.Unlock()
		k.mu.Lock()
		l.refs--
		if l.refs == 0 {
			delete(k.locks, key)
		}
		k.mu.Unlock()
	}
}
