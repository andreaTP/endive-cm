package run.endive.cm.wasi.p2.io.poll;

import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.Promise;
import java.util.ArrayList;
import java.util.List;
import run.endive.cm.wasi.p2.bindings.wasi.io.poll.Host;
import run.endive.cm.wasi.p2.bindings.wasi.io.poll.Pollable;
import run.endive.runtime.TrapException;
import run.endive.wasm.WasmEngineException;

/**
 * The host side of {@code wasi:io/poll}, where each pollable is ready once the Netty future it
 * wraps completes.
 *
 * <p>Polling and blocking wait on the calling thread, so neither may run on the event loop that
 * completes the futures.
 *
 * @see <a href="https://github.com/WebAssembly/WASI/blob/v0.2.12/proposals/io/wit/poll.wit">wasi:io/poll</a>
 */
public class HostPoll implements Host {

    private final EventExecutor eventLoopExecutor;

    public HostPoll(EventExecutor eventLoopExecutor) {
        this.eventLoopExecutor = eventLoopExecutor;
    }

    /**
     * Creates a pollable that is ready once {@code future} completes, whether it succeeds or fails. Dropping it
     * leaves the future running, so the future may be shared with whatever else waits on it.
     */
    public Pollable pollable(Future<?> future) {
        return new FuturePollable(future, false);
    }

    /**
     * Creates a pollable that is ready once {@code future} completes, which cancels the future when the guest
     * drops it. Suits a future that exists only to serve this pollable, such as a timer.
     */
    public Pollable ownedPollable(Future<?> future) {
        return new FuturePollable(future, true);
    }

    /**
     * Waits until at least one pollable is ready and returns the index of every ready one.
     *
     * @see <a href="https://github.com/WebAssembly/WASI/blob/v0.2.12/proposals/io/wit/poll.wit">wasi:io/poll</a>
     */
    @Override
    public List<Long> poll(List<Pollable> pollables) {
        checkBlocking();
        if (pollables.isEmpty()) {
            throw new TrapException("poll requires at least one pollable");
        }
        List<Future<?>> futures = new ArrayList<>(pollables.size());
        for (Pollable pollable : pollables) {
            futures.add(futureOf(pollable));
        }

        if (futures.stream().noneMatch(Future::isDone)) {
            await(futures);
        }

        List<Long> ready = new ArrayList<>();
        for (int i = 0; i < futures.size(); i++) {
            if (futures.get(i).isDone()) {
                ready.add((long) i);
            }
        }
        return ready;
    }

    private void await(List<Future<?>> futures) {
        Promise<Void> anyDone = eventLoopExecutor.newPromise();
        GenericFutureListener<Future<Object>> listener = done -> anyDone.trySuccess(null);
        for (Future<?> future : futures) {
            future.addListener(listener);
        }
        try {
            anyDone.awaitUninterruptibly();
        } finally {
            for (Future<?> future : futures) {
                future.removeListener(listener);
            }
        }
    }

    private static Future<?> futureOf(Pollable pollable) {
        if (!(pollable instanceof FuturePollable)) {
            throw new IllegalArgumentException(
                    "pollable " + pollable + " was not created by " + HostPoll.class.getName());
        }
        return ((FuturePollable) pollable).future;
    }

    private void checkBlocking() {
        if (eventLoopExecutor.inEventLoop()) {
            throw new WasmEngineException(
                    "Blocking host operations cannot be executed from within the EventLoop");
        }
    }

    private final class FuturePollable implements Pollable {

        private final Future<?> future;
        private final boolean owned;

        private FuturePollable(Future<?> future, boolean owned) {
            this.future = future;
            this.owned = owned;
        }

        @Override
        public Boolean ready() {
            return future.isDone();
        }

        @Override
        public void block() {
            checkBlocking();
            future.awaitUninterruptibly();
        }

        @Override
        public void drop() {
            if (owned) {
                future.cancel(false);
            }
        }
    }
}
