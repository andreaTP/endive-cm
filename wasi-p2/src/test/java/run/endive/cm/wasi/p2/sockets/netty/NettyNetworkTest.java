package run.endive.cm.wasi.p2.sockets.netty;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import run.endive.cm.wasi.p2.io.poll.HostPoll;

class NettyNetworkTest {

    @Test
    void closingLeavesTheSchedulerRunning() {
        EventLoopGroup group = new MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory());
        try {
            NettyNetwork.start(new HostPoll(), group, null).close();

            assertFalse(group.isShuttingDown());
        } finally {
            group.shutdownGracefully(0, 1, TimeUnit.SECONDS);
        }
    }

    @Test
    void aSchedulerOutsideNettyIsRefused() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        try {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> NettyNetwork.start(new HostPoll(), scheduler, null));
        } finally {
            scheduler.shutdownNow();
        }
    }

    @Test
    void anEventLoopGroupWithoutNioIsRefused() {
        EventLoopGroup group = new DefaultEventLoopGroup(1);
        try {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> NettyNetwork.start(new HostPoll(), group, null));
        } finally {
            group.shutdownGracefully(0, 1, TimeUnit.SECONDS);
        }
    }
}
