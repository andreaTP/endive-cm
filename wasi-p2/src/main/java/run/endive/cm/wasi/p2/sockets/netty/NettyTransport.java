package run.endive.cm.wasi.p2.sockets.netty;

import java.net.InetAddress;
import java.util.Objects;
import run.endive.cm.wasi.p2.sockets.NameResolver;
import run.endive.cm.wasi.p2.sockets.Transport;

/**
 * The {@link Transport} for an embedder that already runs Netty. Without a transport, {@link
 * run.endive.cm.wasi.p2.WasiPreview2} creates what it needs once the guest is granted network
 * access, and closes it when it closes.
 *
 * <p>Netty is not a dependency this library brings. An embedder granting network access adds
 * {@code io.netty:netty-transport} and {@code io.netty:netty-resolver-dns} from Netty 4.2. On
 * macOS, Netty also needs {@code io.netty:netty-resolver-dns-native-macos} to read the system's
 * DNS servers, and warns once without it.
 */
public final class NettyTransport implements Transport {

    private final NameResolver nameResolver;

    private NettyTransport(Builder builder) {
        this.nameResolver = builder.nameResolver;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public NameResolver nameResolver() {
        return nameResolver;
    }

    public static final class Builder {

        private NameResolver nameResolver;

        private Builder() {}

        /**
         * The Netty resolver for the names the guest looks up, which the embedder closes. By
         * default, a {@link io.netty.resolver.dns.DnsNameResolver} reads the host's resolver
         * configuration and hosts file, and runs on the scheduler.
         */
        public Builder withNameResolver(io.netty.resolver.NameResolver<InetAddress> resolver) {
            this.nameResolver = new NettyNameResolver(Objects.requireNonNull(resolver, "resolver"));
            return this;
        }

        public NettyTransport build() {
            return new NettyTransport(this);
        }
    }
}
