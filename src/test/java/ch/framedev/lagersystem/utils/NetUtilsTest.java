package ch.framedev.lagersystem.utils;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class NetUtilsTest {

    @Test
    @DisplayName("getClientIp: null exchange returns null")
    void getClientIp_nullExchange_returnsNull() {
        assertNull(NetUtils.getClientIp(null));
    }

    @Test
    @DisplayName("getClientIp: prefers first X-Forwarded-For value")
    void getClientIp_prefersFirstXForwardedForValue() {
        FakeHttpExchange exchange = new FakeHttpExchange(new InetSocketAddress("10.0.0.10", 8080));
        exchange.getRequestHeaders().add("X-Forwarded-For", "203.0.113.7, 10.0.0.10");

        assertEquals("203.0.113.7", NetUtils.getClientIp(exchange));
    }

    @Test
    @DisplayName("getClientIp: uses X-Real-IP when present")
    void getClientIp_usesXRealIp() {
        FakeHttpExchange exchange = new FakeHttpExchange(new InetSocketAddress("10.0.0.10", 8080));
        exchange.getRequestHeaders().add("X-Real-IP", "198.51.100.5");

        assertEquals("198.51.100.5", NetUtils.getClientIp(exchange));
    }

    @Test
    @DisplayName("getClientIp: parses Forwarded header")
    void getClientIp_parsesForwardedHeader() {
        FakeHttpExchange exchange = new FakeHttpExchange(new InetSocketAddress("10.0.0.10", 8080));
        exchange.getRequestHeaders().add("Forwarded", "for=192.0.2.60;proto=http;by=203.0.113.43");

        assertEquals("192.0.2.60", NetUtils.getClientIp(exchange));
    }

    @Test
    @DisplayName("getClientIp: strips quotes from Forwarded header")
    void getClientIp_stripsQuotesFromForwardedHeader() {
        FakeHttpExchange exchange = new FakeHttpExchange(new InetSocketAddress("10.0.0.10", 8080));
        exchange.getRequestHeaders().add("Forwarded", "for=\"192.0.2.61\";proto=https");

        assertEquals("192.0.2.61", NetUtils.getClientIp(exchange));
    }

    @Test
    @DisplayName("getClientIp: falls back to remote address")
    void getClientIp_fallsBackToRemoteAddress() {
        FakeHttpExchange exchange = new FakeHttpExchange(new InetSocketAddress("127.0.0.1", 8080));

        assertEquals("127.0.0.1", NetUtils.getClientIp(exchange));
    }

    @Test
    @DisplayName("getClientIp: returns null when no headers and no remote address exist")
    void getClientIp_returnsNullWhenNoRemoteAddressExists() {
        FakeHttpExchange exchange = new FakeHttpExchange(null);

        assertNull(NetUtils.getClientIp(exchange));
    }

    private static final class FakeHttpExchange extends HttpExchange {
        private final Headers requestHeaders = new Headers();
        private final Headers responseHeaders = new Headers();
        private final InetSocketAddress remoteAddress;
        private final ByteArrayOutputStream responseBody = new ByteArrayOutputStream();

        private FakeHttpExchange(InetSocketAddress remoteAddress) {
            this.remoteAddress = remoteAddress;
        }

        @Override
        public Headers getRequestHeaders() {
            return requestHeaders;
        }

        @Override
        public Headers getResponseHeaders() {
            return responseHeaders;
        }

        @Override
        public URI getRequestURI() {
            return URI.create("/");
        }

        @Override
        public String getRequestMethod() {
            return "GET";
        }

        @Override
        public HttpContext getHttpContext() {
            return null;
        }

        @Override
        public void close() {
        }

        @Override
        public InputStream getRequestBody() {
            return new ByteArrayInputStream(new byte[0]);
        }

        @Override
        public OutputStream getResponseBody() {
            return responseBody;
        }

        @Override
        public void sendResponseHeaders(int rCode, long responseLength) throws IOException {
        }

        @Override
        public InetSocketAddress getRemoteAddress() {
            return remoteAddress;
        }

        @Override
        public int getResponseCode() {
            return 200;
        }

        @Override
        public InetSocketAddress getLocalAddress() {
            return new InetSocketAddress("127.0.0.1", 0);
        }

        @Override
        public String getProtocol() {
            return "HTTP/1.1";
        }

        @Override
        public Object getAttribute(String name) {
            return null;
        }

        @Override
        public void setAttribute(String name, Object value) {
        }

        @Override
        public void setStreams(InputStream i, OutputStream o) {
        }

        @Override
        public HttpPrincipal getPrincipal() {
            return null;
        }
    }
}
