package org.java_websocket.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map.Entry;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.net.SocketFactory;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import org.java_websocket.AbstractWebSocket;
import org.java_websocket.WebSocket;
import org.java_websocket.WebSocketImpl;
import org.java_websocket.drafts.Draft;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.enums.Opcode;
import org.java_websocket.enums.ReadyState;
import org.java_websocket.exceptions.InvalidHandshakeException;
import org.java_websocket.framing.Framedata;
import org.java_websocket.handshake.HandshakeImpl1Client;
import org.java_websocket.handshake.Handshakedata;
import org.java_websocket.handshake.ServerHandshake;
import org.java_websocket.protocols.IProtocol;

public abstract class WebSocketClient extends AbstractWebSocket implements Runnable, WebSocket {

    protected URI uri = null;

    private WebSocketImpl engine = null;

    private Socket socket = null;

    private SocketFactory socketFactory = null;

    private OutputStream ostream;

    private Proxy proxy = Proxy.NO_PROXY;

    private Thread writeThread;

    private Thread connectReadThread;

    private Draft draft;

    private Map<String, String> headers;

    private CountDownLatch connectLatch = new CountDownLatch(1);

    private CountDownLatch closeLatch = new CountDownLatch(1);

    private int connectTimeout = 0;

    private DnsResolver dnsResolver = null;

    public WebSocketClient(URI serverUri) {
        this(serverUri, new Draft_6455());
    }

    public WebSocketClient(URI serverUri, Draft protocolDraft) {
        this(serverUri, protocolDraft, null, 0);
    }

    public WebSocketClient(URI serverUri, Map<String, String> httpHeaders) {
        this(serverUri, new Draft_6455(), httpHeaders);
    }

    public WebSocketClient(URI serverUri, Draft protocolDraft, Map<String, String> httpHeaders) {
        this(serverUri, protocolDraft, httpHeaders, 0);
    }

    public WebSocketClient(URI serverUri, Draft protocolDraft, Map<String, String> httpHeaders, int connectTimeout) {
        if (serverUri == null) {
            throw new IllegalArgumentException();
        } else if (protocolDraft == null) {
            throw new IllegalArgumentException("null as draft is permitted for `WebSocketServer` only!");
        } else {
            this.uri = serverUri;
            this.draft = protocolDraft;
            this.dnsResolver = new DnsResolver() {

                @Override
                public InetAddress resolve(URI uri) throws UnknownHostException {
                    return InetAddress.getByName(uri.getHost());
                }
            };
            if (httpHeaders != null) {
                this.headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
                this.headers.putAll(httpHeaders);
            }
            this.connectTimeout = connectTimeout;
            this.setTcpNoDelay(false);
            this.setReuseAddr(false);
            this.engine = new WebSocketImpl(this, protocolDraft);
        }
    }

    public URI getURI() {
        return this.uri;
    }

    @Override
    public Draft getDraft() {
        return this.draft;
    }

    public Socket getSocket() {
        return this.socket;
    }

    public void addHeader(String key, String value) {
        if (this.headers == null) {
            this.headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        }
        this.headers.put(key, value);
    }

    public String removeHeader(String key) {
        return this.headers == null ? null : this.headers.remove(key);
    }

    public void clearHeaders() {
        this.headers = null;
    }

    public void setDnsResolver(DnsResolver dnsResolver) {
        this.dnsResolver = dnsResolver;
    }

    public void reconnect() {
        this.reset();
        this.connect();
    }

    public boolean reconnectBlocking() throws InterruptedException {
        this.reset();
        return this.connectBlocking();
    }

    private void reset() {
        Thread current = Thread.currentThread();
        if (current != this.writeThread && current != this.connectReadThread) {
            try {
                if (this.engine.getReadyState() == ReadyState.NOT_YET_CONNECTED && this.socket != null) {
                    this.socket.close();
                }
                this.closeBlocking();
                if (this.writeThread != null) {
                    this.writeThread.interrupt();
                    this.writeThread.join();
                    this.writeThread = null;
                }
                if (this.connectReadThread != null) {
                    this.connectReadThread.interrupt();
                    this.connectReadThread.join();
                    this.connectReadThread = null;
                }
                this.draft.reset();
                if (this.socket != null) {
                    this.socket.close();
                    this.socket = null;
                }
            } catch (Exception exception) {
                this.onError(exception);
                this.engine.closeConnection(1006, exception.getMessage());
                return;
            }
            this.connectLatch = new CountDownLatch(1);
            this.closeLatch = new CountDownLatch(1);
            this.engine = new WebSocketImpl(this, this.draft);
        } else {
            throw new IllegalStateException("You cannot initialize a reconnect out of the websocket thread. Use reconnect in another thread to ensure a successful cleanup.");
        }
    }

    public void connect() {
        if (this.connectReadThread != null) {
            throw new IllegalStateException("WebSocketClient objects are not reuseable");
        } else {
            this.connectReadThread = new Thread(this);
            this.connectReadThread.setDaemon(this.isDaemon());
            this.connectReadThread.setName("WebSocketConnectReadThread-" + this.connectReadThread.getId());
            this.connectReadThread.start();
        }
    }

    public boolean connectBlocking() throws InterruptedException {
        this.connect();
        this.connectLatch.await();
        return this.engine.isOpen();
    }

    public boolean connectBlocking(long timeout, TimeUnit timeUnit) throws InterruptedException {
        this.connect();
        boolean connected = this.connectLatch.await(timeout, timeUnit);
        if (!connected) {
            this.reset();
        }
        return connected && this.engine.isOpen();
    }

    @Override
    public void close() {
        if (this.writeThread != null) {
            this.engine.close(1000);
        }
    }

    public void closeBlocking() throws InterruptedException {
        this.close();
        this.closeLatch.await();
    }

    @Override
    public void send(String text) {
        this.engine.send(text);
    }

    @Override
    public void send(byte[] data) {
        this.engine.send(data);
    }

    @Override
    public <T> T getAttachment() {
        return this.engine.getAttachment();
    }

    @Override
    public <T> void setAttachment(T attachment) {
        this.engine.setAttachment(attachment);
    }

    @Override
    protected Collection<WebSocket> getConnections() {
        return Collections.singletonList(this.engine);
    }

    @Override
    public void sendPing() {
        this.engine.sendPing();
    }

    @Override
    public void run() {
        InputStream istream;
        try {
            boolean upgradeSocketToSSLSocket = this.prepareSocket();
            this.socket.setTcpNoDelay(this.isTcpNoDelay());
            this.socket.setReuseAddress(this.isReuseAddr());
            int receiveBufferSize = this.getReceiveBufferSize();
            if (receiveBufferSize > 0) {
                this.socket.setReceiveBufferSize(receiveBufferSize);
            }
            if (!this.socket.isConnected()) {
                InetSocketAddress addr = this.dnsResolver == null ? InetSocketAddress.createUnresolved(this.uri.getHost(), this.getPort()) : new InetSocketAddress(this.dnsResolver.resolve(this.uri), this.getPort());
                this.socket.connect(addr, this.connectTimeout);
            }
            if (upgradeSocketToSSLSocket && "wss".equals(this.uri.getScheme())) {
                this.upgradeSocketToSSL();
            }
            if (this.socket instanceof SSLSocket) {
                SSLSocket sslSocket = (SSLSocket) this.socket;
                SSLParameters sslParameters = sslSocket.getSSLParameters();
                this.onSetSSLParameters(sslParameters);
                sslSocket.setSSLParameters(sslParameters);
            }
            istream = this.socket.getInputStream();
            this.ostream = this.socket.getOutputStream();
            this.sendHandshake();
        } catch (Exception exception2) {
            this.onWebsocketError(this.engine, exception2);
            this.engine.closeConnection(-1, exception2.getMessage());
            return;
        } catch (InternalError internalError) {
            if (internalError.getCause() instanceof InvocationTargetException && internalError.getCause().getCause() instanceof IOException) {
                IOException cause = (IOException) internalError.getCause().getCause();
                this.onWebsocketError(this.engine, cause);
                this.engine.closeConnection(-1, cause.getMessage());
                return;
            }
            throw internalError;
        }
        if (this.writeThread != null) {
            this.writeThread.interrupt();
            try {
                this.writeThread.join();
            } catch (InterruptedException exception3) {
            }
        }
        this.writeThread = new Thread(new WebSocketClient.WebsocketWriteThread(this));
        this.writeThread.setDaemon(this.isDaemon());
        this.writeThread.start();
        int receiveBufferSize2 = this.getReceiveBufferSize();
        byte[] rawbuffer = new byte[receiveBufferSize2 > 0 ? receiveBufferSize2 : DEFAULT_READ_BUFFER_SIZE];
        try {
            int readBytes;
            while (!this.isClosing() && !this.isClosed() && (readBytes = istream.read(rawbuffer)) != -1) {
                this.engine.decode(ByteBuffer.wrap(rawbuffer, 0, readBytes));
            }
            this.engine.eot();
        } catch (IOException exception4) {
            this.handleIOException(exception4);
        } catch (RuntimeException exception5) {
            this.onError(exception5);
            this.engine.closeConnection(1006, exception5.getMessage());
        }
    }

    private void upgradeSocketToSSL() throws NoSuchAlgorithmException, KeyManagementException, IOException {
        SSLSocketFactory factory;
        if (this.socketFactory instanceof SSLSocketFactory) {
            factory = (SSLSocketFactory) this.socketFactory;
        } else {
            factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        }
        this.socket = factory.createSocket(this.socket, this.uri.getHost(), this.getPort(), true);
    }

    private boolean prepareSocket() throws IOException {
        boolean upgradeSocketToSSLSocket = false;
        if (this.proxy != Proxy.NO_PROXY) {
            this.socket = new Socket(this.proxy);
            upgradeSocketToSSLSocket = true;
        } else if (this.socketFactory != null) {
            this.socket = this.socketFactory.createSocket();
        } else if (this.socket == null) {
            this.socket = new Socket(this.proxy);
            upgradeSocketToSSLSocket = true;
        } else if (this.socket.isClosed()) {
            throw new IOException();
        }
        return upgradeSocketToSSLSocket;
    }

    protected void onSetSSLParameters(SSLParameters sslParameters) {
        sslParameters.setEndpointIdentificationAlgorithm("HTTPS");
    }

    private int getPort() {
        int port = this.uri.getPort();
        String scheme = this.uri.getScheme();
        if ("wss".equals(scheme)) {
            return port == -1 ? 443 : port;
        } else if ("ws".equals(scheme)) {
            return port == -1 ? 80 : port;
        } else {
            throw new IllegalArgumentException("unknown scheme: " + scheme);
        }
    }

    private void sendHandshake() throws InvalidHandshakeException {
        String part1 = this.uri.getRawPath();
        String part2 = this.uri.getRawQuery();
        String path;
        if (part1 != null && part1.length() != 0) {
            path = part1;
        } else {
            path = "/";
        }
        if (part2 != null) {
            path = path + '?' + part2;
        }
        int port = this.getPort();
        String host = this.uri.getHost() + (port != 80 && port != 443 ? ":" + port : "");
        HandshakeImpl1Client handshake = new HandshakeImpl1Client();
        handshake.setResourceDescriptor(path);
        handshake.put("Host", host);
        if (this.headers != null) {
            for (Entry<String, String> kv : this.headers.entrySet()) {
                handshake.put(kv.getKey(), kv.getValue());
            }
        }
        this.engine.startHandshake(handshake);
    }

    @Override
    public ReadyState getReadyState() {
        return this.engine.getReadyState();
    }

    @Override
    public final void onWebsocketMessage(WebSocket conn, String message) {
        this.onMessage(message);
    }

    @Override
    public final void onWebsocketMessage(WebSocket conn, ByteBuffer blob) {
        this.onMessage(blob);
    }

    @Override
    public final void onWebsocketOpen(WebSocket conn, Handshakedata handshake) {
        this.startConnectionLostTimer();
        this.onOpen((ServerHandshake) handshake);
        this.connectLatch.countDown();
    }

    @Override
    public final void onWebsocketClose(WebSocket conn, int code, String reason, boolean remote) {
        this.stopConnectionLostTimer();
        if (this.writeThread != null) {
            this.writeThread.interrupt();
        }
        this.onClose(code, reason, remote);
        this.connectLatch.countDown();
        this.closeLatch.countDown();
    }

    @Override
    public final void onWebsocketError(WebSocket conn, Exception ex) {
        this.onError(ex);
    }

    @Override
    public final void onWriteDemand(WebSocket conn) {
    }

    @Override
    public void onWebsocketCloseInitiated(WebSocket conn, int code, String reason) {
        this.onCloseInitiated(code, reason);
    }

    @Override
    public void onWebsocketClosing(WebSocket conn, int code, String reason, boolean remote) {
        this.onClosing(code, reason, remote);
    }

    public void onCloseInitiated(int code, String reason) {
    }

    public void onClosing(int code, String reason, boolean remote) {
    }

    public WebSocket getConnection() {
        return this.engine;
    }

    @Override
    public InetSocketAddress getLocalSocketAddress(WebSocket conn) {
        return this.socket != null ? (InetSocketAddress) this.socket.getLocalSocketAddress() : null;
    }

    @Override
    public InetSocketAddress getRemoteSocketAddress(WebSocket conn) {
        return this.socket != null ? (InetSocketAddress) this.socket.getRemoteSocketAddress() : null;
    }

    public abstract void onOpen(ServerHandshake serverHandshake);

    public abstract void onMessage(String text2);

    public abstract void onClose(int intValue, String text3, boolean enabled);

    public abstract void onError(Exception exception6);

    public void onMessage(ByteBuffer bytes) {
    }

    public void setProxy(Proxy proxy) {
        if (proxy == null) {
            throw new IllegalArgumentException();
        } else {
            this.proxy = proxy;
        }
    }

    @Deprecated
    public void setSocket(Socket socket) {
        if (this.socket != null) {
            throw new IllegalStateException("socket has already been set");
        } else {
            this.socket = socket;
        }
    }

    public void setSocketFactory(SocketFactory socketFactory) {
        this.socketFactory = socketFactory;
    }

    @Override
    public void sendFragmentedFrame(Opcode op, ByteBuffer buffer, boolean fin) {
        this.engine.sendFragmentedFrame(op, buffer, fin);
    }

    @Override
    public boolean isOpen() {
        return this.engine.isOpen();
    }

    @Override
    public boolean isFlushAndClose() {
        return this.engine.isFlushAndClose();
    }

    @Override
    public boolean isClosed() {
        return this.engine.isClosed();
    }

    @Override
    public boolean isClosing() {
        return this.engine.isClosing();
    }

    @Override
    public boolean hasBufferedData() {
        return this.engine.hasBufferedData();
    }

    @Override
    public void close(int code) {
        this.engine.close(code);
    }

    @Override
    public void close(int code, String message) {
        this.engine.close(code, message);
    }

    @Override
    public void closeConnection(int code, String message) {
        this.engine.closeConnection(code, message);
    }

    @Override
    public void send(ByteBuffer bytes) {
        this.engine.send(bytes);
    }

    @Override
    public void sendFrame(Framedata framedata) {
        this.engine.sendFrame(framedata);
    }

    @Override
    public void sendFrame(Collection<Framedata> frames) {
        this.engine.sendFrame(frames);
    }

    @Override
    public InetSocketAddress getLocalSocketAddress() {
        return this.engine.getLocalSocketAddress();
    }

    @Override
    public InetSocketAddress getRemoteSocketAddress() {
        return this.engine.getRemoteSocketAddress();
    }

    @Override
    public String getResourceDescriptor() {
        return this.uri.getPath();
    }

    @Override
    public boolean hasSSLSupport() {
        return this.socket instanceof SSLSocket;
    }

    @Override
    public SSLSession getSSLSession() {
        if (!this.hasSSLSupport()) {
            throw new IllegalArgumentException("This websocket uses ws instead of wss. No SSLSession available.");
        } else {
            return ((SSLSocket) this.socket).getSession();
        }
    }

    @Override
    public IProtocol getProtocol() {
        return this.engine.getProtocol();
    }

    private void handleIOException(IOException e) {
        if (e instanceof SSLException) {
            this.onError(e);
        }
        this.engine.eot();
    }

    private class WebsocketWriteThread implements Runnable {

        private final WebSocketClient webSocketClient;

        WebsocketWriteThread(WebSocketClient webSocketClient) {
            this.webSocketClient = webSocketClient;
        }

        @Override
        public void run() {
            Thread.currentThread().setName("WebSocketWriteThread-" + Thread.currentThread().getId());
            try {
                this.runWriteData();
            } catch (IOException exception7) {
                WebSocketClient.this.handleIOException(exception7);
            } finally {
                this.closeSocket();
            }
        }

        private void runWriteData() throws IOException {
            try {
                while (!Thread.interrupted()) {
                    ByteBuffer buffer = WebSocketClient.this.engine.outQueue.take();
                    WebSocketClient.this.ostream.write(buffer.array(), 0, buffer.limit());
                    WebSocketClient.this.ostream.flush();
                }
            } catch (InterruptedException exception8) {
                for (ByteBuffer buffer : WebSocketClient.this.engine.outQueue) {
                    WebSocketClient.this.ostream.write(buffer.array(), 0, buffer.limit());
                    WebSocketClient.this.ostream.flush();
                }
                Thread.currentThread().interrupt();
            }
        }

        private void closeSocket() {
            try {
                if (WebSocketClient.this.socket != null) {
                    WebSocketClient.this.socket.close();
                }
            } catch (IOException exception9) {
                WebSocketClient.this.onWebsocketError(this.webSocketClient, exception9);
            }
        }
    }
}
