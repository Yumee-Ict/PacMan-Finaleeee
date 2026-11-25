package p;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

// Reads, ranks, and saves the five fastest game completion times.
final class Leaderboard {
    static final int LIMIT = 5;

    private static final String NAME_PREFIX = "name.";
    private static final String TIME_PREFIX = "time.";
    private static final Preferences STORE = Preferences.userNodeForPackage(Leaderboard.class);

    private Leaderboard() {
    }

    // Read stored records and return them ordered from fastest to slowest.
    static List<Entry> getEntries() {
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < LIMIT; i++) {
            String name = STORE.get(NAME_PREFIX + i, null);
            long timeMillis = STORE.getLong(TIME_PREFIX + i, -1);
            if (name == null && timeMillis == -1) {
                continue;
            }
            if (name == null || timeMillis < 0) {
                throw new IllegalStateException("Leaderboard data is incomplete.");
            }
            entries.add(new Entry(name, timeMillis));
        }
        entries.sort(Comparator.comparingLong(entry -> entry.timeMillis));
        return entries;
    }

    // A time qualifies if there is room or it beats the current slowest time.
    static boolean qualifies(List<Entry> entries, long timeMillis) {
        return entries.size() < LIMIT
                || timeMillis < entries.get(entries.size() - 1).timeMillis;
    }

    // Add a record, keep only the best five, and write them to saved preferences.
    static List<Entry> addEntry(String name, long timeMillis) throws BackingStoreException {
        List<Entry> entries = getEntries();
        entries.add(new Entry(name, timeMillis));
        entries.sort(Comparator.comparingLong(entry -> entry.timeMillis));
        if (entries.size() > LIMIT) {
            entries.remove(entries.size() - 1);
        }

        for (int i = 0; i < LIMIT; i++) {
            if (i < entries.size()) {
                Entry entry = entries.get(i);
                STORE.put(NAME_PREFIX + i, entry.name);
                STORE.putLong(TIME_PREFIX + i, entry.timeMillis);
            } else {
                STORE.remove(NAME_PREFIX + i);
                STORE.remove(TIME_PREFIX + i);
            }
        }
        STORE.flush();
        return entries;
    }

    // Turn milliseconds into minutes, seconds, and hundredths for display.
    static String formatTime(long timeMillis) {
        long minutes = timeMillis / 60_000;
        long seconds = timeMillis / 1_000 % 60;
        long hundredths = timeMillis / 10 % 100;
        return String.format("%02d:%02d.%02d", minutes, seconds, hundredths);
    }

    // One saved leaderboard result: the player's name and completion time.
    static final class Entry {
        final String name;
        final long timeMillis;

        Entry(String name, long timeMillis) {
            this.name = name;
            this.timeMillis = timeMillis;
        }
    }
}
