package run.endive.cm.wasi.p2.sockets;

/**
 * The {@code wasi:sockets} interfaces a guest is given. {@link HostSocketsFactory} creates them to
 * suit the network policy.
 */
public interface HostSockets extends AutoCloseable {

    run.endive.cm.wasi.p2.bindings.wasi.sockets.ipnamelookup.Host ipNameLookup();

    run.endive.cm.wasi.p2.bindings.wasi.sockets.tcpcreatesocket.Host tcpCreateSocket();

    run.endive.cm.wasi.p2.bindings.wasi.sockets.udpcreatesocket.Host udpCreateSocket();

    /** Releases what this created, leaving the scheduler and what the embedder gave running. */
    @Override
    void close();
}
