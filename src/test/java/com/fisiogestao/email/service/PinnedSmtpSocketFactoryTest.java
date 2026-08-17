package com.fisiogestao.email.service;

import org.eclipse.angus.mail.util.SocketFetcher;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PinnedSmtpSocketFactoryTest {

    @Test
    void deveIgnorarEnderecoReresolvidoEPreservarSomenteAPorta() throws Exception {
        InetAddress validado = InetAddress.getByAddress(new byte[]{(byte) 203, 0, 113, 10});
        var socket = (PinnedSmtpSocketFactory.PinnedSocket)
                new PinnedSmtpSocketFactory(validado).createSocket();

        InetSocketAddress destino = socket.pinnedEndpoint(
                new InetSocketAddress(InetAddress.getLoopbackAddress(), 587));

        assertEquals(validado, destino.getAddress());
        assertEquals(587, destino.getPort());
    }

    @Test
    void angusMailDeveConectarNoIpFixadoSemResolverNovamenteOHostname() throws Exception {
        InetAddress loopbackFicticio = InetAddress.getLoopbackAddress();
        try (ServerSocket server = new ServerSocket(0, 1, loopbackFicticio)) {
            Properties properties = new Properties();
            properties.put("mail.smtp.socketFactory", new PinnedSmtpSocketFactory(loopbackFicticio));
            properties.put("mail.smtp.socketFactory.fallback", "false");
            properties.put("mail.smtp.connectiontimeout", "1000");

            try (Socket client = SocketFetcher.getSocket(
                    "host-que-nao-deve-ser-resolvido.invalid",
                    server.getLocalPort(), properties, "mail.smtp", false);
                 Socket accepted = server.accept()) {
                assertEquals(loopbackFicticio, client.getInetAddress());
                assertEquals(loopbackFicticio, accepted.getLocalAddress());
            }
        }
    }

    @Test
    void sslDiretoDeveUsarIpFixadoERejeitarServidorSemTlsValido() throws Exception {
        InetAddress loopbackFicticio = InetAddress.getLoopbackAddress();
        try (ServerSocket server = new ServerSocket(0, 1, loopbackFicticio);
             var executor = Executors.newSingleThreadExecutor()) {
            var accepted = executor.submit(() -> {
                try (Socket socket = server.accept()) {
                    return socket.getLocalAddress();
                }
            });
            Properties properties = new Properties();
            properties.put("mail.smtp.socketFactory", new PinnedSmtpSocketFactory(loopbackFicticio));
            properties.put("mail.smtp.socketFactory.fallback", "false");
            properties.put("mail.smtp.connectiontimeout", "1000");
            properties.put("mail.smtp.timeout", "1000");
            properties.put("mail.smtp.ssl.checkserveridentity", "true");

            assertThrows(Exception.class, () -> SocketFetcher.getSocket(
                    "host-que-nao-deve-ser-resolvido.invalid",
                    server.getLocalPort(), properties, "mail.smtp", true));
            assertEquals(loopbackFicticio, accepted.get(2, TimeUnit.SECONDS));
        }
    }
}
