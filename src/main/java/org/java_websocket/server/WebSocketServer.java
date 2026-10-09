package org.java_websocket.server;

import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.java_websocket.AbstractWebSocket;
import org.java_websocket.SocketChannelIOHelper;
import org.java_websocket.WebSocket;
import org.java_websocket.WebSocketFactory;
import org.java_websocket.WebSocketImpl;
import org.java_websocket.WebSocketServerFactory;
import org.java_websocket.WrappedByteChannel;
import org.java_websocket.drafts.Draft;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.java_websocket.exceptions.WrappedIOException;
import org.java_websocket.framing.Framedata;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.handshake.Handshakedata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class WebSocketServer extends AbstractWebSocket implements Runnable {

    private static final int AVAILABLE_PROCESSORS = Runtime.getRuntime().availableProcessors();

    private final Logger log = LoggerFactory.getLogger(WebSocketServer.class);

    private final Collection<WebSocket> connections;

    private final InetSocketAddress address;

    private ServerSocketChannel server;

    private Selector selector;

    private List<Draft> drafts;

    private Thread selectorthread;

    private final AtomicBoolean isclosed = new AtomicBoolean(false);

    protected List<WebSocketServer.WebSocketWorker> decoders;

    private List<WebSocketImpl> iqueue;

    private BlockingQueue<ByteBuffer> buffers;

    private int queueinvokes = 0;

    private final AtomicInteger queuesize = new AtomicInteger(0);

    private WebSocketServerFactory wsf = new DefaultWebSocketServerFactory();

    private int maxPendingConnections = -1;

    public WebSocketServer() {
        this(new InetSocketAddress(80), AVAILABLE_PROCESSORS, null);
    }

    public WebSocketServer(InetSocketAddress address) {
        this(address, AVAILABLE_PROCESSORS, null);
    }

    public WebSocketServer(InetSocketAddress address, int decodercount) {
        this(address, decodercount, null);
    }

    public WebSocketServer(InetSocketAddress address, List<Draft> drafts) {
        this(address, AVAILABLE_PROCESSORS, drafts);
    }

    public WebSocketServer(InetSocketAddress address, int decodercount, List<Draft> drafts) {
        this(address, decodercount, drafts, new HashSet<>());
    }

    private static InetSocketAddress checkAddressOfExistingChannel(ServerSocketChannel existingChannel) {
        assert existingChannel.isOpen();
        SocketAddress addr;
        try {
            addr = existingChannel.getLocalAddress();
        } catch (IOException exception2) {
            throw new IllegalArgumentException("Could not get address of channel passed to WebSocketServer, make sure it is bound", exception2);
        }
        if (addr == null) {
            throw new IllegalArgumentException("Could not get address of channel passed to WebSocketServer, make sure it is bound");
        } else {
            return (InetSocketAddress) addr;
        }
    }

    public WebSocketServer(ServerSocketChannel existingChannel) {
        this(checkAddressOfExistingChannel(existingChannel));
        this.server = existingChannel;
    }

    public WebSocketServer(InetSocketAddress address, int decodercount, List<Draft> drafts, Collection<WebSocket> connectionscontainer) {
        if (address != null && decodercount >= 1 && connectionscontainer != null) {
            if (drafts == null) {
                this.drafts = Collections.emptyList();
            } else {
                this.drafts = drafts;
            }
            this.address = address;
            this.connections = connectionscontainer;
            this.setTcpNoDelay(false);
            this.setReuseAddr(false);
            this.iqueue = new LinkedList<>();
            this.decoders = new ArrayList<>(decodercount);
            this.buffers = new LinkedBlockingQueue<>();
            for (int i = 0; i < decodercount; i++) {
                WebSocketServer.WebSocketWorker ex = new WebSocketServer.WebSocketWorker();
                this.decoders.add(ex);
            }
        } else {
            throw new IllegalArgumentException("address and connectionscontainer must not be null and you need at least 1 decoder");
        }
    }

    public void start() {
        if (this.selectorthread != null) {
            throw new IllegalStateException(this.getClass().getName() + " can only be started once.");
        } else {
            Thread t = new Thread(this);
            t.setDaemon(this.isDaemon());
            t.start();
        }
    }

    public void stop(int timeout) throws InterruptedException {
        this.stop(timeout, "");
    }

    public void stop(int timeout, String closeMessage) throws InterruptedException {
        if (this.isclosed.compareAndSet(false, true)) {
            List<WebSocket> socketsToClose;
            synchronized (this.connections) {
                socketsToClose = new ArrayList<>(this.connections);
            }
            for (WebSocket ws : socketsToClose) {
                ws.close(1001, closeMessage);
            }
            this.wsf.close();
            synchronized (this) {
                if (this.selectorthread != null && this.selector != null) {
                    this.selector.wakeup();
                    this.selectorthread.join(timeout);
                }
            }
        }
    }

    public void stop() throws InterruptedException {
        this.stop(0);
    }

    @Override
    public Collection<WebSocket> getConnections() {
        synchronized (this.connections) {
            return Collections.unmodifiableCollection(new ArrayList<>(this.connections));
        }
    }

    public InetSocketAddress getAddress() {
        return this.address;
    }

    public int getPort() {
        int port = this.getAddress().getPort();
        if (port == 0 && this.server != null) {
            port = this.server.socket().getLocalPort();
        }
        return port;
    }

    @Override
    public void setDaemon(boolean daemon) {
        super.setDaemon(daemon);
        for (WebSocketServer.WebSocketWorker w : this.decoders) {
            if (w.isAlive()) {
                throw new IllegalStateException("Cannot call setDaemon after server is already started!");
            }
            w.setDaemon(daemon);
        }
    }

    public List<Draft> getDraft() {
        return Collections.unmodifiableList(this.drafts);
    }

    public void setMaxPendingConnections(int numberOfConnections) {
        this.maxPendingConnections = numberOfConnections;
    }

    public int getMaxPendingConnections() {
        return this.maxPendingConnections;
    }

    @Override
    public void run() {
        if (this.doEnsureSingleThread()) {
            if (this.doSetupSelectorAndServerThread()) {
                try {
                    int shutdownCount = 5;
                    int selectTimeout = 0;
                    while (!this.selectorthread.isInterrupted() && shutdownCount != 0) {
                        SelectionKey key = null;
                        try {
                            if (this.isclosed.get()) {
                                selectTimeout = 5;
                            }
                            int keyCount = this.selector.select(selectTimeout);
                            if (keyCount == 0 && this.isclosed.get()) {
                                shutdownCount--;
                            }
                            Set<SelectionKey> keys = this.selector.selectedKeys();
                            Iterator<SelectionKey> i = keys.iterator();
                            while (i.hasNext()) {
                                key = i.next();
                                if (key.isValid()) {
                                    if (key.isAcceptable()) {
                                        this.doAccept(key, i);
                                    } else if ((!key.isReadable() || this.doRead(key, i)) && key.isWritable()) {
                                        this.doWrite(key);
                                    }
                                }
                            }
                            this.doAdditionalRead();
                        } catch (CancelledKeyException exception3) {
                        } catch (ClosedByInterruptException exception4) {
                            return;
                        } catch (WrappedIOException exception5) {
                            this.handleIOException(key, exception5.getConnection(), exception5.getIOException());
                        } catch (IOException exception6) {
                            this.handleIOException(key, null, exception6);
                        } catch (InterruptedException exception7) {
                            Thread.currentThread().interrupt();
                        }
                    }
                } catch (RuntimeException exception8) {
                    this.handleFatal(null, exception8);
                } finally {
                    this.doServerShutdown();
                }
            }
        }
    }

    private void doAdditionalRead() throws InterruptedException, IOException {
        while (!this.iqueue.isEmpty()) {
            WebSocketImpl conn = this.iqueue.remove(0);
            WrappedByteChannel c = (WrappedByteChannel) conn.getChannel();
            ByteBuffer buf = this.takeBuffer();
            try {
                if (SocketChannelIOHelper.readMore(buf, conn, c)) {
                    this.iqueue.add(conn);
                }
                if (buf.hasRemaining()) {
                    conn.inQueue.put(buf);
                    this.queue(conn);
                } else {
                    this.pushBuffer(buf);
                }
            } catch (IOException exception9) {
                this.pushBuffer(buf);
                throw exception9;
            }
        }
    }

    private void doAccept(SelectionKey key, Iterator<SelectionKey> i) throws IOException, InterruptedException {
        if (!this.onConnect(key)) {
            key.cancel();
        } else {
            SocketChannel channel = this.server.accept();
            if (channel != null) {
                channel.configureBlocking(false);
                Socket socket = channel.socket();
                socket.setTcpNoDelay(this.isTcpNoDelay());
                socket.setKeepAlive(true);
                WebSocketImpl w = this.wsf.createWebSocket(this, this.drafts);
                w.setSelectionKey(channel.register(this.selector, 1, w));
                try {
                    w.setChannel(this.wsf.wrapChannel(channel, w.getSelectionKey()));
                    i.remove();
                    this.allocateBuffers(w);
                } catch (IOException exception10) {
                    if (w.getSelectionKey() != null) {
                        w.getSelectionKey().cancel();
                    }
                    this.handleIOException(w.getSelectionKey(), null, exception10);
                }
            }
        }
    }

    private boolean doRead(SelectionKey key, Iterator<SelectionKey> i) throws InterruptedException, WrappedIOException {
        WebSocketImpl conn = (WebSocketImpl) key.attachment();
        ByteBuffer buf = this.takeBuffer();
        if (conn.getChannel() == null) {
            key.cancel();
            this.handleIOException(key, conn, new IOException());
            return false;
        } else {
            try {
                if (SocketChannelIOHelper.read(buf, conn, conn.getChannel())) {
                    if (buf.hasRemaining()) {
                        conn.inQueue.put(buf);
                        this.queue(conn);
                        i.remove();
                        if (conn.getChannel() instanceof WrappedByteChannel && ((WrappedByteChannel) conn.getChannel()).isNeedRead()) {
                            this.iqueue.add(conn);
                        }
                    } else {
                        this.pushBuffer(buf);
                    }
                } else {
                    this.pushBuffer(buf);
                }
                return true;
            } catch (IOException exception11) {
                this.pushBuffer(buf);
                throw new WrappedIOException(conn, exception11);
            }
        }
    }

    private void doWrite(SelectionKey key) throws WrappedIOException {
        WebSocketImpl conn = (WebSocketImpl) key.attachment();
        try {
            if (SocketChannelIOHelper.batch(conn, conn.getChannel()) && key.isValid()) {
                key.interestOps(1);
            }
        } catch (IOException exception12) {
            throw new WrappedIOException(conn, exception12);
        }
    }

    private boolean doSetupSelectorAndServerThread() {
        this.selectorthread.setName("WebSocketSelector-" + this.selectorthread.getId());
        try {
            if (this.server == null) {
                this.server = ServerSocketChannel.open();
            }
            this.server.configureBlocking(false);
            ServerSocket socket = this.server.socket();
            int receiveBufferSize = this.getReceiveBufferSize();
            if (receiveBufferSize > 0) {
                socket.setReceiveBufferSize(receiveBufferSize);
            }
            socket.setReuseAddress(this.isReuseAddr());
            if (!socket.isBound()) {
                socket.bind(this.address, this.getMaxPendingConnections());
            }
            this.selector = Selector.open();
            this.server.register(this.selector, this.server.validOps());
            this.startConnectionLostTimer();
            for (WebSocketServer.WebSocketWorker ex : this.decoders) {
                ex.start();
            }
            this.onStart();
            return true;
        } catch (IOException exception13) {
            this.handleFatal(null, exception13);
            return false;
        }
    }

    private boolean doEnsureSingleThread() {
        synchronized (this) {
            if (this.selectorthread != null) {
                throw new IllegalStateException(this.getClass().getName() + " can only be started once.");
            } else {
                this.selectorthread = Thread.currentThread();
                return !this.isclosed.get();
            }
        }
    }

    private void doServerShutdown() {
        this.stopConnectionLostTimer();
        if (this.decoders != null) {
            for (WebSocketServer.WebSocketWorker w : this.decoders) {
                w.interrupt();
            }
        }
        if (this.selector != null) {
            try {
                this.selector.close();
            } catch (IOException exception14) {
                this.log.error("IOException during selector.close", (Throwable) exception14);
                this.onError(null, exception14);
            }
        }
        if (this.server != null) {
            try {
                this.server.close();
            } catch (IOException exception15) {
                this.log.error("IOException during server.close", (Throwable) exception15);
                this.onError(null, exception15);
            }
        }
    }

    protected void allocateBuffers(WebSocket c) throws InterruptedException {
        if (this.queuesize.get() < 2 * this.decoders.size() + 1) {
            this.queuesize.incrementAndGet();
            this.buffers.put(this.createBuffer());
        }
    }

    protected void releaseBuffers(WebSocket c) throws InterruptedException {
    }

    public ByteBuffer createBuffer() {
        int receiveBufferSize = this.getReceiveBufferSize();
        return ByteBuffer.allocate(receiveBufferSize > 0 ? receiveBufferSize : DEFAULT_READ_BUFFER_SIZE);
    }

    protected void queue(WebSocketImpl ws) throws InterruptedException {
        if (ws.getWorkerThread() == null) {
            ws.setWorkerThread(this.decoders.get(this.queueinvokes % this.decoders.size()));
            this.queueinvokes++;
        }
        ws.getWorkerThread().put(ws);
    }

    private ByteBuffer takeBuffer() throws InterruptedException {
        return this.buffers.take();
    }

    private void pushBuffer(ByteBuffer buf) throws InterruptedException {
        if (this.buffers.size() <= this.queuesize.intValue()) {
            this.buffers.put(buf);
        }
    }

    private void handleIOException(SelectionKey key, WebSocket conn, IOException ex) {
        if (key != null) {
            key.cancel();
        }
        if (conn != null) {
            conn.closeConnection(1006, ex.getMessage());
        } else if (key != null) {
            SelectableChannel channel = key.channel();
            if (channel != null && channel.isOpen()) {
                try {
                    channel.close();
                } catch (IOException exception16) {
                }
                this.log.trace("Connection closed because of exception", (Throwable) ex);
            }
        }
    }

    private void handleFatal(WebSocket conn, Exception e) {
        this.log.error("Shutdown due to fatal error", (Throwable) e);
        this.onError(conn, e);
        String causeMessage = e.getCause() != null ? " caused by " + e.getCause().getClass().getName() : "";
        String errorMessage = "Got error on server side: " + e.getClass().getName() + causeMessage;
        try {
            this.stop(0, errorMessage);
        } catch (InterruptedException exception17) {
            Thread.currentThread().interrupt();
            this.log.error("Interrupt during stop", (Throwable) e);
            this.onError(null, exception17);
        }
        if (this.decoders != null) {
            for (WebSocketServer.WebSocketWorker w : this.decoders) {
                w.interrupt();
            }
        }
        if (this.selectorthread != null) {
            this.selectorthread.interrupt();
        }
    }

    @Override
    public final void onWebsocketMessage(WebSocket conn, String message) {
        this.onMessage(conn, message);
    }

    @Override
    public final void onWebsocketMessage(WebSocket conn, ByteBuffer blob) {
        this.onMessage(conn, blob);
    }

    @Override
    public final void onWebsocketOpen(WebSocket conn, Handshakedata handshake) {
        if (this.addConnection(conn)) {
            this.onOpen(conn, (ClientHandshake) handshake);
        }
    }

    @Override
    public final void onWebsocketClose(WebSocket conn, int code, String reason, boolean remote) {
        this.selector.wakeup();
        try {
            if (this.removeConnection(conn)) {
                this.onClose(conn, code, reason, remote);
            }
        } finally {
            try {
                this.releaseBuffers(conn);
            } catch (InterruptedException exception18) {
                Thread.currentThread().interrupt();
            }
        }
    }

    protected boolean removeConnection(WebSocket ws) {
        boolean removed = false;
        synchronized (this.connections) {
            if (this.connections.contains(ws)) {
                removed = this.connections.remove(ws);
            } else {
                this.log.trace("Removing connection which is not in the connections collection! Possible no handshake received! {}", ws);
            }
        }
        if (this.isclosed.get() && this.connections.isEmpty()) {
            this.selectorthread.interrupt();
        }
        return removed;
    }

    protected boolean addConnection(WebSocket ws) {
        if (!this.isclosed.get()) {
            synchronized (this.connections) {
                return this.connections.add(ws);
            }
        } else {
            ws.close(1001);
            return true;
        }
    }

    @Override
    public final void onWebsocketError(WebSocket conn, Exception ex) {
        this.onError(conn, ex);
    }

    @Override
    public final void onWriteDemand(WebSocket w) {
        WebSocketImpl conn = (WebSocketImpl) w;
        try {
            conn.getSelectionKey().interestOps(5);
        } catch (CancelledKeyException exception19) {
            conn.outQueue.clear();
        }
        this.selector.wakeup();
    }

    @Override
    public void onWebsocketCloseInitiated(WebSocket conn, int code, String reason) {
        this.onCloseInitiated(conn, code, reason);
    }

    @Override
    public void onWebsocketClosing(WebSocket conn, int code, String reason, boolean remote) {
        this.onClosing(conn, code, reason, remote);
    }

    public void onCloseInitiated(WebSocket conn, int code, String reason) {
    }

    public void onClosing(WebSocket conn, int code, String reason, boolean remote) {
    }

    public final void setWebSocketFactory(WebSocketServerFactory wsf) {
        if (this.wsf != null) {
            this.wsf.close();
        }
        this.wsf = wsf;
    }

    public final WebSocketFactory getWebSocketFactory() {
        return this.wsf;
    }

    protected boolean onConnect(SelectionKey key) {
        return true;
    }

    private Socket getSocket(WebSocket conn) {
        WebSocketImpl impl = (WebSocketImpl) conn;
        return ((SocketChannel) impl.getSelectionKey().channel()).socket();
    }

    @Override
    public InetSocketAddress getLocalSocketAddress(WebSocket conn) {
        return (InetSocketAddress) this.getSocket(conn).getLocalSocketAddress();
    }

    @Override
    public InetSocketAddress getRemoteSocketAddress(WebSocket conn) {
        return (InetSocketAddress) this.getSocket(conn).getRemoteSocketAddress();
    }

    public abstract void onOpen(WebSocket webSocket, ClientHandshake clientHandshake);

    public abstract void onClose(WebSocket webSocket2, int intValue2, String text2, boolean enabled);

    public abstract void onMessage(WebSocket webSocket3, String text3);

    public abstract void onError(WebSocket webSocket4, Exception exception20);

    public abstract void onStart();

    public void onMessage(WebSocket conn, ByteBuffer message) {
    }

    public void broadcast(String text) {
        this.broadcast(text, this.connections);
    }

    public void broadcast(byte[] data) {
        this.broadcast(data, this.connections);
    }

    public void broadcast(ByteBuffer data) {
        this.broadcast(data, this.connections);
    }

    public void broadcast(byte[] data, Collection<WebSocket> clients) {
        if (data != null && clients != null) {
            this.broadcast(ByteBuffer.wrap(data), clients);
        } else {
            throw new IllegalArgumentException();
        }
    }

    public void broadcast(ByteBuffer data, Collection<WebSocket> clients) {
        if (data != null && clients != null) {
            this.doBroadcast(data, clients);
        } else {
            throw new IllegalArgumentException();
        }
    }

    public void broadcast(String text, Collection<WebSocket> clients) {
        if (text != null && clients != null) {
            this.doBroadcast(text, clients);
        } else {
            throw new IllegalArgumentException();
        }
    }

    private void doBroadcast(Object data, Collection<WebSocket> clients) {
        String strData = null;
        if (data instanceof String) {
            strData = (String) data;
        }
        ByteBuffer byteData = null;
        if (data instanceof ByteBuffer) {
            byteData = (ByteBuffer) data;
        }
        if (strData != null || byteData != null) {
            Map<Draft, List<Framedata>> draftFrames = new HashMap<>();
            List<WebSocket> clientCopy;
            synchronized (clients) {
                clientCopy = new ArrayList<>(clients);
            }
            for (WebSocket client : clientCopy) {
                if (client != null) {
                    Draft draft = client.getDraft();
                    this.fillFrames(draft, draftFrames, strData, byteData);
                    try {
                        client.sendFrame(draftFrames.get(draft));
                    } catch (WebsocketNotConnectedException exception21) {
                    }
                }
            }
        }
    }

    private void fillFrames(Draft draft, Map<Draft, List<Framedata>> draftFrames, String strData, ByteBuffer byteData) {
        if (!draftFrames.containsKey(draft)) {
            List<Framedata> frames = null;
            if (strData != null) {
                frames = draft.createFrames(strData, false);
            }
            if (byteData != null) {
                frames = draft.createFrames(byteData, false);
            }
            if (frames != null) {
                draftFrames.put(draft, frames);
            }
        }
    }

    public class WebSocketWorker extends Thread {

        private BlockingQueue<WebSocketImpl> iqueue = new LinkedBlockingQueue<>();

        public WebSocketWorker() {
            this.setName("WebSocketWorker-" + this.getId());
            this.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {

                @Override
                public void uncaughtException(Thread t, Throwable e) {
                    WebSocketServer.this.log.error("Uncaught exception in thread {}: {}", t.getName(), e);
                }
            });
        }

        public void put(WebSocketImpl ws) throws InterruptedException {
            this.iqueue.put(ws);
        }

        @Override
        public void run() {
            WebSocketImpl ws = null;
            try {
                while (true) {
                    ws = this.iqueue.take();
                    ByteBuffer buf = ws.inQueue.poll();
                    assert buf != null;
                    this.doDecode(ws, buf);
                    ws = null;
                }
            } catch (InterruptedException exception22) {
                Thread.currentThread().interrupt();
            } catch (ThreadDeath | LinkageError | VirtualMachineError threadDeathLinkageErrorVirtualMachineError) {
                WebSocketServer.this.log.error("Got fatal error in worker thread {}", this.getName());
                Exception exception = new Exception(threadDeathLinkageErrorVirtualMachineError);
                WebSocketServer.this.handleFatal(ws, exception);
            } catch (Throwable exception23) {
                WebSocketServer.this.log.error("Uncaught exception in thread {}: {}", this.getName(), exception23);
                if (ws != null) {
                    Exception exception25 = new Exception(exception23);
                    WebSocketServer.this.onWebsocketError(ws, exception25);
                    ws.close();
                }
            }
        }

        private void doDecode(WebSocketImpl ws, ByteBuffer buf) throws InterruptedException {
            try {
                ws.decode(buf);
            } catch (Exception exception24) {
                WebSocketServer.this.log.error("Error while reading from remote connection", (Throwable) exception24);
            } finally {
                WebSocketServer.this.pushBuffer(buf);
            }
        }
    }
}
