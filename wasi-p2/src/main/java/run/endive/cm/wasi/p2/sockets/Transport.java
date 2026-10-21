package run.endive.cm.wasi.p2.sockets;

import run.endive.cm.wasi.p2.sockets.netty.NettyTransport;

/**
 * What {@code wasi:sockets} uses beyond the scheduler, for an embedder that supplies its own.
 * {@link NettyTransport} is the implementation for an embedder that already runs Netty.
 */
public interface Transport {

    /**
     * What resolves the names the guest looks up, which the embedder closes. It returns {@code
     * null} for the host's default, which reads the host's resolver configuration and hosts file.
     */
    NameResolver nameResolver();
}
