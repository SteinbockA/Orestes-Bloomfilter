package orestes.bloomfilter.cachesketch;

import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

/**
 * Created on 04.10.17.
 *
 * @author Konstantin Simon Maria Möllers
 */
public interface ExpirationQueue<T> extends Iterable<T> {
    /**
     * Adds an item with a time to live (TTL) to the queue.
     *
     * @param item    The item to add to the queue
     * @param ttl     The time to live of the item
     * @param ttlUnit The unit of the ttl
     * @return whether the item has been added
     */
    default boolean addTTL(T item, long ttl, TimeUnit ttlUnit) {
        return add(new ExpiringItem<>(item, System.nanoTime() + NANOSECONDS.convert(ttl, ttlUnit)));
    }

    /**
     * Adds an item with a time to live (TTL) to the queue.
     *
     * @param item The item to add to the queue
     * @param ttl  The time to live of the item, in nanoseconds
     * @return whether the item has been added
     */
    default boolean addTTL(T item, long ttl) {
        return addTTL(item, ttl, NANOSECONDS);
    }

    /**
     * Adds an item with an expiration timestamp to the queue.
     *
     * @param item      The item to add to the queue
     * @param timestamp The timestamp when the item expires, in nanoseconds since {@link System#nanoTime()}
     * @return whether the item has been added
     */
    default boolean addExpiration(T item, long timestamp) {
        return add(new ExpiringItem<>(item, timestamp));
    }

    /**
     * Returns the queue's size.
     *
     * @return the size of this queue
     */
    int size();

    /**
     * Adds an expiring item to the queue.
     *
     * @param item The item to add to the queue
     * @return whether the item has been added
     */
    boolean add(ExpiringItem<T> item);

    /**
     * Returns the items in the queue which are not expired yet.
     *
     * @return a queue of non-expired items
     */
    Collection<ExpiringItem<T>> getNonExpired();

    /**
     * Clears the queue.
     */
    void clear();

    /**
     * Checks whether this queue contains a given item.
     *
     * @param item The item to check
     * @return true, if item is contained
     */
    boolean contains(T item);

    /**
     * Removes the given item from this queue.
     *
     * @param item The item to remove
     * @return true, if item has been removed
     */
    boolean remove(T item);

    /**
     * Returns a stream containing all entries of this stream.
     *
     * @return all entries of a stream
     */
    Stream<ExpiringItem<T>> streamEntries();

    @Override
    default Iterator<T> iterator() {
        return getNonExpired().stream().map(ExpiringItem::getItem).iterator();
    }

    /**
     * An item of the ExpirationQueue
     *
     * @param <T> The type of the item held by the queue
     */
    class ExpiringItem<T> implements Delayed {
        private final T item;
        private final long expiration;

        /**
         * Creates an ExpiringItem.
         *
         * @param item       The actual item which expires
         * @param expiration The expiration timestamp, relative to {@link java.lang.System#nanoTime()}
         */
        public ExpiringItem(T item, long expiration) {
            this.item = item;
            this.expiration = expiration;
        }

        /**
         * @return the actual item which expires
         */
        public T getItem() {
            return item;
        }

        /**
         * @return the timestamp when the item expires
         */
        public long getExpiration() {
            return expiration;
        }

        /**
         * Creates an absolute expiring item which contains the expiration absolute to a given point in time.
         *
         * @param time The time point to be absolute to.
         * @param unit The time point's unit.
         * @return A new expiring item instance.
         */
        public ExpiringItem<T> toAbsolute(long time, TimeUnit unit) {
            return new ExpiringItem<>(item, expiration + NANOSECONDS.convert(time, unit));
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return unit.convert(expiration - System.nanoTime(), NANOSECONDS);
        }

        @Override
        public int compareTo(Delayed delayed) {
            return Long.compare(getDelay(NANOSECONDS), delayed.getDelay(NANOSECONDS));
        }

        @Override
        public String toString() {
            return getItem() + " expires in " + getDelay(TimeUnit.SECONDS) + "s";
        }
    }
}
