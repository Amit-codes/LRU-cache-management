package com.lru.model;

/** Mutable counters describing how well a cache is performing. */
public class CacheStatistics {

    private long hits;
    private long misses;
    private long evictions;
    private long puts;

    public void recordHit()      { hits++; }
    public void recordMiss()     { misses++; }
    public void recordEviction() { evictions++; }
    public void recordPut()      { puts++; }

    public long getHits()      { return hits; }
    public long getMisses()    { return misses; }
    public long getEvictions() { return evictions; }
    public long getPuts()      { return puts; }

    /** GET requests only (PEEK is deliberately not counted because it is not a real access). */
    public long getTotalRequests() {
        return hits + misses;
    }

    /** Hit ratio in the range 0.0 - 1.0; 0 when there have been no requests yet. */
    public double getHitRatio() {
        long total = getTotalRequests();
        return total == 0 ? 0.0 : (double) hits / total;
    }

    public void reset() {
        hits = 0;
        misses = 0;
        evictions = 0;
        puts = 0;
    }

    @Override
    public String toString() {
        return String.format("hits=%d, misses=%d, evictions=%d, hitRatio=%.1f%%",
                hits, misses, evictions, getHitRatio() * 100);
    }
}
