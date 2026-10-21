package run.endive.cm.wasi.p2.sockets.netty;

import io.netty.channel.IoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.resolver.dns.DnsNameResolver;
import io.netty.resolver.dns.DnsNameResolverBuilder;
import java.util.concurrent.ScheduledExecutorService;
import run.endive.cm.wasi.p2.io.poll.HostPoll;
import run.endive.cm.wasi.p2.sockets.HostSockets;
import run.endive.cm.wasi.p2.sockets.NameResolver;
import run.endive.cm.wasi.p2.sockets.Transport;
import run.endive.cm.wasi.p2.sockets.ipnamelookup.HostIpNameLookup;
import run.endive.cm.wasi.p2.sockets.tcpcreatesocket.HostTcpCreateSocket;
import run.endive.cm.wasi.p2.sockets.udpcreatesocket.HostUdpCreateSocket;

/**
 * The {@code wasi:sockets} interfaces for a guest granted network access, running on Netty.
 *
 * <p>Only {@link run.endive.cm.wasi.p2.sockets.HostSocketsFactory} creates one, after checking that
 * Netty is present. It runs on the host's scheduler, which has to be a Netty event loop group. It
 * owns whatever the embedder left it to create, and releases that on {@link #close()}.
 */
public final class NettyNetwork implements HostSockets {

    private final DnsNameResolver defaultNameResolver;
    private final HostIpNameLookup ipNameLookup;
    private final HostTcpCreateSocket tcpCreateSocket;
    private final HostUdpCreateSocket udpCreateSocket;

    private NettyNetwork(
            HostPoll poll, IoEventLoopGroup eventLoopGroup, NameResolver nameResolver) {
        this.defaultNameResolver =
                nameResolver == null ? defaultNameResolver(eventLoopGroup) : null;
        this.ipNameLookup =
                new HostIpNameLookup(
                        poll,
                        nameResolver == null
                                ? new NettyNameResolver(defaultNameResolver)
                                : nameResolver);
        this.tcpCreateSocket = new HostTcpCreateSocket();
        this.udpCreateSocket = new HostUdpCreateSocket();
    }

    /**
     * @param poll the poll everything this creates subscribes through
     * @param scheduler the host's Netty event loop group, which the caller shuts down
     * @param transport what else the embedder supplies, or {@code null} to create everything
     * @throws IllegalArgumentException if {@code scheduler} is not a group running NIO channels
     */
    public static NettyNetwork start(
            HostPoll poll, ScheduledExecutorService scheduler, Transport transport) {
        return new NettyNetwork(
                poll,
                nioEventLoopGroup(scheduler),
                transport == null ? null : transport.nameResolver());
    }

    @Override
    public run.endive.cm.wasi.p2.bindings.wasi.sockets.ipnamelookup.Host ipNameLookup() {
        return ipNameLookup;
    }

    @Override
    public run.endive.cm.wasi.p2.bindings.wasi.sockets.tcpcreatesocket.Host tcpCreateSocket() {
        return tcpCreateSocket;
    }

    @Override
    public run.endive.cm.wasi.p2.bindings.wasi.sockets.udpcreatesocket.Host udpCreateSocket() {
        return udpCreateSocket;
    }

    /** Closes the resolver, unless the embedder gave it. */
    @Override
    public void close() {
        if (defaultNameResolver != null) {
            defaultNameResolver.close();
        }
    }

    private static IoEventLoopGroup nioEventLoopGroup(ScheduledExecutorService scheduler) {
        if (scheduler instanceof IoEventLoopGroup
                && ((IoEventLoopGroup) scheduler).isIoType(NioIoHandler.class)) {
            return (IoEventLoopGroup) scheduler;
        }
        throw new IllegalArgumentException(
                "Granting the guest network access needs a scheduler that is a Netty event loop"
                        + " group running NIO channels, such as a MultiThreadIoEventLoopGroup"
                        + " created with NioIoHandler.newFactory(). Got "
                        + scheduler.getClass().getName()
                        + ".");
    }

    private static DnsNameResolver defaultNameResolver(IoEventLoopGroup group) {
        return new DnsNameResolverBuilder(group.next())
                .datagramChannelType(NioDatagramChannel.class)
                .socketChannelType(NioSocketChannel.class)
                .build();
    }
}
