package com.example.lending.ops.part4.review;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link IMap} whose key locks are leases: {@link #lock(Object)} grants a lease that lapses after
 * {@code leaseTime}, and a waiter finding an expired lease takes it over. A holder that never reaches
 * {@link #unlock(Object)} (e.g. because the code between lock and unlock threw) blocks others for at most one
 * lease period rather than indefinitely.
 */
public class LeasedClusterMap<K, V> implements IMap<K, V> {

	private record Lease(Thread owner, long expiresAtMillis) { }

	private final long leaseMillis;
	private final Map<K, V> values = new ConcurrentHashMap<>();
	private final Map<K, Lease> leases = new HashMap<>();

	public LeasedClusterMap(Duration leaseTime) { this.leaseMillis = Math.max(1L, leaseTime.toMillis()); }

	@Override
	public V get(K key) { return values.get(key); }

	@Override
	public V getOrDefault(K key, V defaultValue) { return values.getOrDefault(key, defaultValue); }

	@Override
	public V put(K key, V value) { return values.put(key, value); }

	@Override
	public void lock(K key) {
		synchronized (leases) {
			long now = System.currentTimeMillis();
			for (Lease current = leases.get(key); current != null && current.owner() != Thread.currentThread()
					&& current.expiresAtMillis() > now; current = leases.get(key), now = System.currentTimeMillis()) {
				try {
					leases.wait(current.expiresAtMillis() - now);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new IllegalStateException("interrupted waiting for lock on " + key, e);
				}
			}
			leases.put(key, new Lease(Thread.currentThread(), now + leaseMillis));
		}
	}

	@Override
	public void unlock(K key) {
		synchronized (leases) {
			Lease current = leases.get(key);
			if (current != null && current.owner() == Thread.currentThread()) {
				leases.remove(key);
				leases.notifyAll();
			}
		}
	}
}
