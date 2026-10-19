package run.endive.cm.wasi.p2.sockets.udpcreatesocket;

import run.endive.cm.wasi.p2.bindings.wasi.sockets.network.ErrorCode;
import run.endive.cm.wasi.p2.bindings.wasi.sockets.network.ErrorCodeException;
import run.endive.cm.wasi.p2.bindings.wasi.sockets.network.IpAddressFamily;
import run.endive.cm.wasi.p2.bindings.wasi.sockets.udp.UdpSocket;
import run.endive.cm.wasi.p2.bindings.wasi.sockets.udpcreatesocket.Host;

/**
 * The host side of {@code wasi:sockets/udp-create-socket}, which supports no sockets yet.
 *
 * @see <a href="https://github.com/WebAssembly/WASI/blob/v0.2.12/proposals/sockets/wit/udp-create-socket.wit">wasi:sockets/udp-create-socket</a>
 */
public class HostUdpCreateSocket implements Host {

    @Override
    public UdpSocket createUdpSocket(IpAddressFamily addressFamily) {
        throw new ErrorCodeException(ErrorCode.NOT_SUPPORTED);
    }
}
