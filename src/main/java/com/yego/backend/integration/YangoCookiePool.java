package com.yego.backend.integration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class YangoCookiePool {

    private static final String COOKIE_SEPARATOR = "|||";

    private final List<String> cookies;
    private final Set<Integer> invalidIndices = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<Integer, AtomicLong> lastUseByIndex = new ConcurrentHashMap<>();

    public YangoCookiePool(@Value("${yego.yango.cookies:}") String configuredCookies) {
        this.cookies = configuredCookies == null
                ? List.of()
                : Arrays.stream(configuredCookies.split("\\Q" + COOKIE_SEPARATOR + "\\E"))
                        .map(String::trim)
                        .filter(cookie -> !cookie.isBlank())
                        .toList();
    }

    public int size() {
        return cookies.size();
    }

    public String randomCookie() {
        int index = randomValidIndex();
        if (index < 0) {
            resetInvalid();
            index = randomValidIndex();
        }
        return cookieAt(index);
    }

    public int randomValidIndex() {
        requireConfigured();
        List<Integer> validIndices = java.util.stream.IntStream.range(0, cookies.size())
                .filter(index -> !invalidIndices.contains(index))
                .boxed()
                .toList();
        return validIndices.isEmpty()
                ? -1
                : validIndices.get(ThreadLocalRandom.current().nextInt(validIndices.size()));
    }

    public int reserveLeastRecentlyUsed(long throttleMs) {
        requireConfigured();
        int selected = -1;
        long oldestUse = Long.MAX_VALUE;
        for (int index = 0; index < cookies.size(); index++) {
            if (invalidIndices.contains(index)) {
                continue;
            }
            long lastUse = lastUseByIndex.computeIfAbsent(index, ignored -> new AtomicLong()).get();
            if (lastUse < oldestUse) {
                oldestUse = lastUse;
                selected = index;
            }
        }
        if (selected < 0) {
            return -1;
        }

        long waitMs = throttleMs - (System.currentTimeMillis() - oldestUse);
        if (waitMs > 0) {
            sleep(waitMs);
        }
        lastUseByIndex.get(selected).set(System.currentTimeMillis());
        return selected;
    }

    public String cookieAt(int index) {
        requireConfigured();
        if (index < 0 || index >= cookies.size()) {
            throw new IllegalArgumentException("Índice de cookie Yango inválido");
        }
        return cookies.get(index);
    }

    public void markInvalid(int index) {
        if (index >= 0 && index < cookies.size()) {
            invalidIndices.add(index);
        }
    }

    public void resetInvalid() {
        invalidIndices.clear();
    }

    private void requireConfigured() {
        if (cookies.isEmpty()) {
            throw new IllegalStateException("No hay cookies Yango configuradas en YEGO_YANGO_COOKIES");
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
