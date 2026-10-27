package com.example.lending.ops.part4.review;

/** Cluster-wide map with per-key locks, as used for review-room membership. */
public interface IMap<K, V> {

	V get(K key);

	V getOrDefault(K key, V defaultValue);

	V put(K key, V value);

	void lock(K key);

	void unlock(K key);
}
