package com.fisiogestao.email.service;

import com.fisiogestao.shared.web.BusinessException;
import org.springframework.stereotype.Component;

import java.net.IDN;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Set;

@Component
public class SmtpEndpointValidator {

    private static final Set<Integer> PORTAS_PERMITIDAS = Set.of(465, 587);

    public String validar(String host, Integer port) {
        return validarEResolver(host, port).host();
    }

    public SmtpEndpoint validarEResolver(String host, Integer port) {
        if (host == null || host.isBlank()) {
            throw new BusinessException("Informe o host SMTP.");
        }
        if (port == null || !PORTAS_PERMITIDAS.contains(port)) {
            throw new BusinessException("Use somente as portas SMTP seguras 465 ou 587.");
        }

        String normalizedHost;
        try {
            normalizedHost = IDN.toASCII(host.trim(), IDN.USE_STD3_ASCII_RULES).toLowerCase(Locale.ROOT);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Host SMTP invalido.");
        }
        if (normalizedHost.equals("localhost") || normalizedHost.endsWith(".localhost")
                || normalizedHost.endsWith(".local") || normalizedHost.endsWith(".internal")) {
            throw new BusinessException("O host SMTP informado nao e permitido.");
        }

        try {
            InetAddress[] addresses = InetAddress.getAllByName(normalizedHost);
            for (InetAddress address : addresses) {
                if (isEnderecoNaoPublico(address)) {
                    throw new BusinessException("O host SMTP informado nao e permitido.");
                }
            }
            return new SmtpEndpoint(normalizedHost, port, addresses[0]);
        } catch (UnknownHostException ex) {
            throw new BusinessException("Nao foi possivel resolver o host SMTP informado.");
        }
    }

    private boolean isEnderecoNaoPublico(InetAddress address) {
        if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                || address.isSiteLocalAddress() || address.isMulticastAddress()) {
            return true;
        }
        byte[] bytes = address.getAddress();
        if (address instanceof Inet6Address) {
            return bytes.length == 16 && (bytes[0] & 0xfe) == 0xfc;
        }
        if (bytes.length != 4) {
            return true;
        }
        int first = bytes[0] & 0xff;
        int second = bytes[1] & 0xff;
        return first == 0 || first == 127 || first == 10
                || (first == 100 && second >= 64 && second <= 127)
                || (first == 169 && second == 254)
                || (first == 172 && second >= 16 && second <= 31)
                || (first == 192 && second == 168);
    }

    public record SmtpEndpoint(String host, int port, InetAddress pinnedAddress) {
    }
}
