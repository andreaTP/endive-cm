package run.endive.cm.wasi.p2.io.streams;

import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import java.io.ByteArrayOutputStream;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import run.endive.cm.wasi.p2.io.poll.HostPoll;

/**
 * An output stream holding what the guest writes until a consumer takes it. The consumer may run on
 * any thread, such as a worker writing to a blocking Java stream. Neither side ever waits on the
 * other.
 *
 * <p>The guest may buffer up to {@code capacity} bytes. A flush completes once the consumer has
 * taken everything written before it, and the guest may not write in the meantime. The consumer
 * learns there is something to take through {@code onWritten}.
 *
 * <p>The stream keeps each array written to it rather than copying it.
 */
public class BufferedHostOutputStream extends HostOutputStream {

    private static final Runnable NOTHING_WRITTEN = () -> {};

    private final EventExecutor eventLoopExecutor;
    private final int capacity;
    private final Runnable onWritten;

    private final Queue<byte[]> chunks = new ConcurrentLinkedQueue<>();
    private final AtomicLong drained = new AtomicLong();
    private final AtomicReference<End> end = new AtomicReference<>();

    /** Only the guest updates this, so the update needs no atomic operation. */
    private volatile long written;

    private volatile long flushedThrough;
    private volatile Promise<Void> readiness;

    /**
     * @param capacity how many bytes the guest may buffer before it has to wait for the consumer
     * @param onWritten run after the guest writes or flushes
     */
    public BufferedHostOutputStream(
            EventExecutor eventLoopExecutor, HostPoll poll, int capacity, Runnable onWritten) {
        super(eventLoopExecutor, poll);
        this.eventLoopExecutor = eventLoopExecutor;
        this.capacity = capacity;
        this.onWritten = onWritten == null ? NOTHING_WRITTEN : onWritten;
        this.readiness = eventLoopExecutor.<Void>newPromise().setSuccess(null);
    }

    /**
     * Takes everything the guest has written, which completes a flush once all it covers is taken
     * and frees the buffer for more writes.
     */
    public byte[] drain() {
        byte[] first = chunks.poll();
        byte[] bytes;
        if (first == null) {
            bytes = new byte[0];
        } else if (chunks.isEmpty()) {
            bytes = first;
        } else {
            ByteArrayOutputStream joined = new ByteArrayOutputStream();
            joined.writeBytes(first);
            for (byte[] chunk = chunks.poll(); chunk != null; chunk = chunks.poll()) {
                joined.writeBytes(chunk);
            }
            bytes = joined.toByteArray();
        }
        drained.addAndGet(bytes.length);
        if (isReady()) {
            readiness.trySuccess(null);
        }
        return bytes;
    }

    /** Ends the stream, which the guest sees on its next operation. */
    public void close() {
        end.compareAndSet(null, End.CLOSED);
        readiness.trySuccess(null);
    }

    /** Fails the stream with {@code cause}, which the guest sees on its next operation. */
    public void fail(Throwable cause) {
        end.compareAndSet(null, new End(cause));
        readiness.trySuccess(null);
    }

    @Override
    public Future<?> readiness() {
        return readiness;
    }

    @Override
    protected long writeCapacity() {
        checkOpen();
        long taken = drained.get();
        return taken < flushedThrough ? 0 : capacity - (written - taken);
    }

    @Override
    @SuppressWarnings("NonAtomicOperationOnVolatileField")
    protected void writeBytes(byte[] contents) {
        checkOpen();
        if (contents.length == 0) {
            return;
        }
        written = written + contents.length;
        chunks.add(contents);
        if (written - drained.get() >= capacity) {
            notReady();
        }
        onWritten.run();
    }

    @Override
    protected void startFlush() {
        checkOpen();
        if (written == drained.get()) {
            return;
        }
        flushedThrough = written;
        notReady();
        onWritten.run();
    }

    /** Reports a failure once, after which the stream is closed. */
    private void checkOpen() {
        End ended = end.get();
        if (ended == null) {
            return;
        }
        if (ended.failure != null) {
            end.set(End.CLOSED);
            throw StreamErrors.failed(ended.failure);
        }
        throw StreamErrors.closed();
    }

    /** Whether the guest may write, with no flush pending and room in the buffer, or has ended. */
    private boolean isReady() {
        long taken = drained.get();
        return end.get() != null || (taken >= flushedThrough && written - taken < capacity);
    }

    /**
     * Makes {@link #readiness} pending. A consumer that drains or ends the stream meanwhile
     * completes either the old promise or the new one, which the check after publishing it
     * catches.
     */
    private void notReady() {
        if (!readiness.isDone()) {
            return;
        }
        Promise<Void> pending = eventLoopExecutor.newPromise();
        readiness = pending;
        if (isReady()) {
            pending.trySuccess(null);
        }
    }

    /** How the stream ended, with the failure the guest has yet to see, if any. */
    private static final class End {

        static final End CLOSED = new End(null);

        final Throwable failure;

        End(Throwable failure) {
            this.failure = failure;
        }
    }
}
