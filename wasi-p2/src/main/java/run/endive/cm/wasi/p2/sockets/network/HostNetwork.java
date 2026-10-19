package run.endive.cm.wasi.p2.sockets.network;

import run.endive.cm.wasi.p2.bindings.wasi.sockets.network.Network;

/**
 * The host side of the {@code wasi:sockets} {@code network} resource, which grants a guest access
 * to the network.
 *
 * @see <a href="https://github.com/WebAssembly/WASI/blob/v0.2.12/proposals/sockets/wit/network.wit">wasi:sockets/network</a>
 */
public class HostNetwork implements Network {}
