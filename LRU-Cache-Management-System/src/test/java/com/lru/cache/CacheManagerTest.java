package com.lru.cache;

import com.lru.exception.CacheException;
import com.lru.exception.InvalidCapacityException;
import com.lru.exception.InvalidKeyException;
import com.lru.exception.InvalidValueException;
import com.lru.model.CacheCommand;
import com.lru.model.CacheOperation;
import com.lru.model.CacheStatistics;
import com.lru.model.OperationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CacheManagerTest {

    @Test
    @DisplayName("hits, misses and hit ratio are tracked for GET only")
    void hitAndMissStatistics() {
        CacheManager m = new CacheManager(3);
        m.put("a", "1");
        m.get("a");          // hit
        m.get("a");          // hit
        m.get("b");          // miss
        m.peek("a");         // not counted
        m.peek("zzz");       // not counted

        CacheStatistics s = m.statistics();
        assertEquals(2, s.getHits());
        assertEquals(1, s.getMisses());
        assertEquals(3, s.getTotalRequests());
        assertEquals(2.0 / 3.0, s.getHitRatio(), 1e-9);
    }

    @Test
    @DisplayName("hit ratio is 0 before any request (no division by zero)")
    void hitRatioWithoutRequests() {
        assertEquals(0.0, new CacheManager(2).statistics().getHitRatio());
    }

    @Test
    @DisplayName("evictions are counted and reported in the operation")
    void evictionsAreReported() {
        CacheManager m = new CacheManager(2);
        m.put("a", "1");
        m.put("b", "2");
        CacheOperation op = m.put("c", "3");

        assertTrue(op.causedEviction());
        assertEquals("a", op.evicted().get(0).key());
        assertEquals(1, m.statistics().getEvictions());
        assertTrue(op.message().contains("EVICTED"));
    }

    @Test
    @DisplayName("demo scenario through the manager: key 2 is evicted")
    void demoScenario() {
        CacheManager m = new CacheManager(3);
        m.put("1", "100");
        m.put("2", "200");
        m.put("3", "300");
        m.get("1");
        CacheOperation op = m.put("4", "400");
        assertEquals("2", op.evicted().get(0).key());
        assertEquals(List.of("4", "1", "3"), m.snapshot().stream().map(CacheEntry::key).toList());
    }

    @Test
    @DisplayName("GET on a missing key is a failed operation, not an exception")
    void getMissingIsNotAnError() {
        CacheOperation op = new CacheManager(2).get("nope");
        assertFalse(op.success());
        assertEquals(OperationType.GET, op.type());
    }

    @Test
    @DisplayName("removing a missing key reports failure instead of throwing")
    void removeMissing() {
        assertFalse(new CacheManager(2).remove("ghost").success());
    }

    @Test
    @DisplayName("keys and values are trimmed")
    void trimsInput() {
        CacheManager m = new CacheManager(2);
        m.put("  a  ", "  1 ");
        assertTrue(m.get("a").success());
        assertEquals("1", m.peek("a").value());
    }

    @Test
    @DisplayName("invalid input raises meaningful exceptions")
    void validation() {
        CacheManager m = new CacheManager(2);
        assertThrows(InvalidKeyException.class, () -> m.put("", "v"));
        assertThrows(InvalidKeyException.class, () -> m.get("   "));
        assertThrows(InvalidKeyException.class, () -> m.remove(null));
        assertThrows(InvalidValueException.class, () -> m.put("k", " "));
        assertThrows(InvalidCapacityException.class, () -> m.resize(0));
        assertThrows(InvalidCapacityException.class, () -> new CacheManager(-3));
        assertThrows(CacheException.class, () -> CacheManager.parseCapacity("abc"));
        assertThrows(CacheException.class, () -> CacheManager.parseCapacity("-2"));
        assertEquals(12, CacheManager.parseCapacity(" 12 "));
        assertEquals(0, m.size());
    }

    @Test
    @DisplayName("resize evicts and reports the entries it removed")
    void resizeReportsEvictions() {
        CacheManager m = new CacheManager(3);
        m.put("a", "1");
        m.put("b", "2");
        m.put("c", "3");
        CacheOperation op = m.resize(1);
        assertEquals(2, op.evicted().size());
        assertEquals(1, m.capacity());
        assertEquals(2, m.statistics().getEvictions());
    }

    @Test
    @DisplayName("history keeps newest first and listeners fire for every operation")
    void historyAndListeners() {
        CacheManager m = new CacheManager(2);
        List<OperationType> seen = new ArrayList<>();
        m.addListener(op -> seen.add(op.type()));
        m.put("a", "1");
        m.get("a");
        m.clear();

        assertEquals(List.of(OperationType.PUT, OperationType.GET, OperationType.CLEAR), seen);
        assertEquals(OperationType.CLEAR, m.history().get(0).type());
        assertEquals(3, m.history().size());
    }

    @Test
    @DisplayName("history is capped so memory use stays bounded")
    void historyIsBounded() {
        CacheManager m = new CacheManager(2);
        for (int i = 0; i < 500; i++) {
            m.put("k" + i, "v");
        }
        assertEquals(200, m.history().size());
    }

    @Test
    @DisplayName("utilization = size / capacity")
    void utilization() {
        CacheManager m = new CacheManager(4);
        m.put("a", "1");
        assertEquals(0.25, m.utilization(), 1e-9);
    }

    @Test
    @DisplayName("execute dispatches every command type")
    void executeCommands() {
        CacheManager m = new CacheManager(2);
        assertTrue(m.execute(CacheCommand.put("a", "1")).success());
        assertTrue(m.execute(CacheCommand.get("a")).success());
        assertTrue(m.execute(CacheCommand.peek("a")).success());
        assertTrue(m.execute(CacheCommand.resize(5)).success());
        assertTrue(m.execute(CacheCommand.remove("a")).success());
        assertTrue(m.execute(CacheCommand.clear()).success());
        assertEquals(5, m.capacity());
    }

    @Test
    @DisplayName("statistics can be reset")
    void resetStatistics() {
        CacheManager m = new CacheManager(1);
        m.put("a", "1");
        m.put("b", "2");
        m.get("b");
        m.resetStatistics();
        assertEquals(0, m.statistics().getHits());
        assertEquals(0, m.statistics().getEvictions());
    }
}
