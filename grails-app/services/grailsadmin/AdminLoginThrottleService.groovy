package grailsadmin

/** Small per-address throttle for the single admin account. */
class AdminLoginThrottleService {
    private static final int MAX_FAILURES = 5
    private static final long WINDOW_MILLIS = 15 * 60 * 1000L
    private static final int MAX_ADDRESSES = 10000
    private final Map<String, Deque<Long>> failures = [:]

    synchronized boolean permitted(String address) {
        String key = address ?: 'unknown'
        prune(key)
        if (!failures.containsKey(key) && failures.size() >= MAX_ADDRESSES) {
            failures.keySet().toList().each { prune(it) }
            if (failures.size() >= MAX_ADDRESSES) return false
        }
        !failures.containsKey(key) || failures[key].size() < MAX_FAILURES
    }

    synchronized void failed(String address) {
        String key = address ?: 'unknown'
        prune(key)
        if (!failures.containsKey(key) && failures.size() >= MAX_ADDRESSES) return
        if (!failures.containsKey(key)) failures[key] = new ArrayDeque<Long>()
        failures[key].addLast(System.currentTimeMillis())
    }

    synchronized void succeeded(String address) {
        failures.remove(address ?: 'unknown')
    }

    private void prune(String key) {
        Deque<Long> times = failures[key]
        if (!times) return
        long cutoff = System.currentTimeMillis() - WINDOW_MILLIS
        while (!times.isEmpty() && times.peekFirst() < cutoff) times.removeFirst()
        if (times.isEmpty()) failures.remove(key)
    }
}
