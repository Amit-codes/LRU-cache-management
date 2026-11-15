package com.lru.simulation;

import com.lru.cache.CacheManager;
import com.lru.exception.CacheException;
import com.lru.model.CacheCommand;
import com.lru.model.OperationType;
import com.lru.simulation.PolicyComparator.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimulationTest {

    @Test
    @DisplayName("parser understands all commands, ignores comments and is case-insensitive")
    void parsesScript() {
        List<CacheCommand> cmds = CommandParser.parseScript("""
                # comment
                PUT(1, 100)
                get(1)

                // another comment
                PEEK(1);
                REMOVE(1)
                CLEAR()
                RESIZE(4)
                """);
        assertEquals(List.of(OperationType.PUT, OperationType.GET, OperationType.PEEK,
                OperationType.REMOVE, OperationType.CLEAR, OperationType.RESIZE),
                cmds.stream().map(CacheCommand::type).toList());
        assertEquals("1", cmds.get(0).key());
        assertEquals("100", cmds.get(0).value());
        assertEquals(4, cmds.get(5).capacity());
    }

    @Test
    @DisplayName("PUT values may contain commas")
    void valueWithComma() {
        assertEquals("a, b", CommandParser.parseLine("PUT(k, a, b)").value());
    }

    @Test
    @DisplayName("malformed commands give a message that includes the line number")
    void parseErrors() {
        CacheException ex = assertThrows(CacheException.class, () -> CommandParser.parseScript("PUT(1, 1)\nHELLO(2)"));
        assertTrue(ex.getMessage().startsWith("Line 2"));
        assertThrows(CacheException.class, () -> CommandParser.parseLine("PUT(1)"));
        assertThrows(CacheException.class, () -> CommandParser.parseLine("GET()"));
        assertThrows(CacheException.class, () -> CommandParser.parseLine("GET(1, 2)"));
        assertThrows(CacheException.class, () -> CommandParser.parseLine("CLEAR(1)"));
        assertThrows(CacheException.class, () -> CommandParser.parseLine("RESIZE(abc)"));
    }

    @Test
    @DisplayName("the documented capacity-3 demo script evicts key 2")
    void demoScript() {
        CacheManager m = new CacheManager(3);
        boolean evictedTwo = false;
        for (CacheCommand c : CommandParser.parseScript("PUT(1,100)\nPUT(2,200)\nPUT(3,300)\nGET(1)\nPUT(4,400)")) {
            evictedTwo = m.execute(c).evicted().stream().anyMatch(e -> e.key().equals("2"));
        }
        assertTrue(evictedTwo);
    }

    @Test
    @DisplayName("comparator: LRU beats FIFO on a recency-friendly workload")
    void comparatorWorkload() {
        List<Result> results = PolicyComparator.compare(
                PolicyComparator.parseRequests("1 2 3 1 2 4 1 2 5 1 2 3 1 2"), 3);
        Result lru = results.get(0);
        Result fifo = results.get(1);
        assertEquals("LRU", lru.policy());
        assertEquals("FIFO", fifo.policy());
        assertEquals(8, lru.hits());
        assertEquals(4, fifo.hits());
        assertEquals(14, lru.hits() + lru.misses());
    }

    @Test
    @DisplayName("comparator rejects empty input")
    void comparatorValidation() {
        assertThrows(CacheException.class, () -> PolicyComparator.parseRequests("  "));
    }
}
