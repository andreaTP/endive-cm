package run.endive.cm.wasi.p2.sockets.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.InetSocketAddress;
import org.junit.jupiter.api.Test;
import run.endive.cm.wasi.p2.bindings.wasi.sockets.network.ErrorCode;
import run.endive.cm.wasi.p2.bindings.wasi.sockets.network.ErrorCodeException;
import run.endive.cm.wasi.p2.bindings.wasi.sockets.network.Network;
import run.endive.cm.wasi.p2.sockets.NetworkPolicy;
import run.endive.cm.wasi.p2.sockets.NetworkPolicy.AddressUse;
import run.endive.cm.wasi.p2.sockets.instancenetwork.HostInstanceNetwork;

class HostNetworkTest {

    private static final InetSocketAddress LOOPBACK = new InetSocketAddress("127.0.0.1", 8080);

    @Test
    void anAllowedAddressPasses() {
        HostNetwork network = new HostNetwork(NetworkPolicy.allowAll());

        network.checkAddress(LOOPBACK, AddressUse.TCP_BIND);
    }

    @Test
    void aDeniedAddressIsAccessDenied() {
        HostNetwork network = new HostNetwork(NetworkPolicy.denyAll());

        ErrorCodeException e =
                assertThrows(
                        ErrorCodeException.class,
                        () -> network.checkAddress(LOOPBACK, AddressUse.TCP_BIND));

        assertEquals(ErrorCode.ACCESS_DENIED, e.error());
    }

    @Test
    void theInstanceNetworkGrantsWhatItsPolicyAllows() {
        NetworkPolicy policy = NetworkPolicy.allowAll();

        Network network = new HostInstanceNetwork(policy).instanceNetwork();

        assertSame(policy, HostNetwork.of(network).policy());
    }

    @Test
    void aNetworkFromElsewhereIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> HostNetwork.of(new Network() {}));
    }
}
