package com.fisiogestao.email.service;

import javax.net.SocketFactory;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

/**
 * Fixa a conexao SMTP no endereco ja resolvido e validado. O hostname original
 * continua no JavaMail para SNI e verificacao do certificado TLS, mas nao e
 * resolvido novamente durante a abertura do socket.
 */
final class PinnedSmtpSocketFactory extends SocketFactory {

    private final InetAddress pinnedAddress;

    PinnedSmtpSocketFactory(InetAddress pinnedAddress) {
        this.pinnedAddress = pinnedAddress;
    }

    @Override
    public Socket createSocket() {
        return new PinnedSocket(pinnedAddress);
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {
        return connect(port, null, 0);
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localAddress, int localPort) throws IOException {
        return connect(port, localAddress, localPort);
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        return connect(port, null, 0);
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        return connect(port, localAddress, localPort);
    }

    private Socket connect(int port, InetAddress localAddress, int localPort) throws IOException {
        Socket socket = createSocket();
        if (localAddress != null) {
            socket.bind(new InetSocketAddress(localAddress, localPort));
        }
        socket.connect(new InetSocketAddress(pinnedAddress, port));
        return socket;
    }

    static final class PinnedSocket extends Socket {

        private final InetAddress pinnedAddress;

        private PinnedSocket(InetAddress pinnedAddress) {
            this.pinnedAddress = pinnedAddress;
        }

        @Override
        public void connect(SocketAddress endpoint) throws IOException {
            super.connect(pinnedEndpoint(endpoint));
        }

        @Override
        public void connect(SocketAddress endpoint, int timeout) throws IOException {
            super.connect(pinnedEndpoint(endpoint), timeout);
        }

        InetSocketAddress pinnedEndpoint(SocketAddress requestedEndpoint) {
            if (!(requestedEndpoint instanceof InetSocketAddress requested)) {
                throw new IllegalArgumentException("Endpoint SMTP invalido.");
            }
            return new InetSocketAddress(pinnedAddress, requested.getPort());
        }
    }
}
