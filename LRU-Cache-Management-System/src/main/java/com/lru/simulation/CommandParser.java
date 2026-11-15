package com.lru.simulation;

import com.lru.exception.CacheException;
import com.lru.model.CacheCommand;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns text such as {@code PUT(1, 100)} into {@link CacheCommand}s.
 * Blank lines and lines starting with {@code #} or {@code //} are ignored.
 */
public final class CommandParser {

    private static final Pattern LINE =
            Pattern.compile("^(PUT|GET|REMOVE|PEEK|CLEAR|RESIZE)\\s*\\((.*)\\)\\s*;?$", Pattern.CASE_INSENSITIVE);

    private CommandParser() {
    }

    public static List<CacheCommand> parseScript(String script) {
        List<CacheCommand> commands = new ArrayList<>();
        String[] lines = script.split("\\R");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) {
                continue;
            }
            try {
                commands.add(parseLine(line));
            } catch (CacheException e) {
                throw new CacheException("Line " + (i + 1) + ": " + e.getMessage());
            }
        }
        return commands;
    }

    public static CacheCommand parseLine(String line) {
        Matcher m = LINE.matcher(line.trim());
        if (!m.matches()) {
            throw new CacheException("Cannot understand '" + line.trim()
                    + "'. Use PUT(k, v), GET(k), PEEK(k), REMOVE(k), CLEAR() or RESIZE(n)");
        }
        String name = m.group(1).toUpperCase();
        String args = m.group(2).trim();

        switch (name) {
            case "PUT": {
                String[] parts = args.split(",", 2);          // the value may itself contain commas
                if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
                    throw new CacheException("PUT needs a key and a value: PUT(key, value)");
                }
                return CacheCommand.put(parts[0].trim(), parts[1].trim());
            }
            case "GET":
                return CacheCommand.get(singleArg(name, args));
            case "PEEK":
                return CacheCommand.peek(singleArg(name, args));
            case "REMOVE":
                return CacheCommand.remove(singleArg(name, args));
            case "CLEAR":
                if (!args.isEmpty()) {
                    throw new CacheException("CLEAR takes no arguments: CLEAR()");
                }
                return CacheCommand.clear();
            default: // RESIZE
                try {
                    int capacity = Integer.parseInt(singleArg(name, args));
                    return CacheCommand.resize(capacity);
                } catch (NumberFormatException e) {
                    throw new CacheException("RESIZE needs a whole number: RESIZE(5)");
                }
        }
    }

    private static String singleArg(String name, String args) {
        if (args.isBlank() || args.contains(",")) {
            throw new CacheException(name + " needs exactly one argument: " + name + "(key)");
        }
        return args.trim();
    }
}
