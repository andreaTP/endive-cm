package run.endive.cm.wasi.p2.sockets.ipnamelookup;

import run.endive.cm.wasi.p2.bindings.wasi.sockets.ipnamelookup.Host;
import run.endive.cm.wasi.p2.bindings.wasi.sockets.ipnamelookup.ResolveAddressStream;
import run.endive.cm.wasi.p2.bindings.wasi.sockets.network.ErrorCode;
import run.endive.cm.wasi.p2.bindings.wasi.sockets.network.ErrorCodeException;
import run.endive.cm.wasi.p2.bindings.wasi.sockets.network.Network;

/**
 * The host side of {@code wasi:sockets/ip-name-lookup}, which resolves no names yet.
 *
 * @see <a href="https://github.com/WebAssembly/WASI/blob/v0.2.12/proposals/sockets/wit/ip-name-lookup.wit">wasi:sockets/ip-name-lookup</a>
 */
public class HostIpNameLookup implements Host {

    @Override
    public ResolveAddressStream resolveAddresses(Network network, String name) {
        throw new ErrorCodeException(ErrorCode.NOT_SUPPORTED);
    }
}
