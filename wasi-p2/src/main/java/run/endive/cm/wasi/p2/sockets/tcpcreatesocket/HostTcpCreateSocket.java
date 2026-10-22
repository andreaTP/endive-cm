package run.endive.cm.wasi.p2.sockets.tcpcreatesocket;

import run.endive.cm.wasi.p2.bindings.wasi.sockets.network.ErrorCode;
import run.endive.cm.wasi.p2.bindings.wasi.sockets.network.ErrorCodeException;
import run.endive.cm.wasi.p2.bindings.wasi.sockets.network.IpAddressFamily;
import run.endive.cm.wasi.p2.bindings.wasi.sockets.tcp.TcpSocket;
import run.endive.cm.wasi.p2.bindings.wasi.sockets.tcpcreatesocket.Host;

/**
 * The host side of {@code wasi:sockets/tcp-create-socket}, which supports no sockets yet.
 *
 * @see <a href="https://github.com/WebAssembly/WASI/blob/v0.2.12/proposals/sockets/wit/tcp-create-socket.wit">wasi:sockets/tcp-create-socket</a>
 */
public class HostTcpCreateSocket implements Host {

    @Override
    public TcpSocket createTcpSocket(IpAddressFamily addressFamily) {
        throw new ErrorCodeException(ErrorCode.NOT_SUPPORTED);
    }
}
