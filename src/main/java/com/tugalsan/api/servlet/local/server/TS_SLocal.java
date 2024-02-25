package com.tugalsan.api.servlet.local.server;

import com.tugalsan.api.log.server.TS_Log;
import com.tugalsan.api.optional.client.TGS_Optional;
import com.tugalsan.api.optional.client.TGS_OptionalBoolean;
import com.tugalsan.api.runnable.client.TGS_RunnableType1;
import com.tugalsan.api.thread.server.TS_ThreadWait;
import com.tugalsan.api.thread.server.sync.TS_ThreadSyncTrigger;
import com.tugalsan.api.unsafe.client.TGS_UnSafe;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;

public class TS_SLocal {

    final private static TS_Log d = TS_Log.of(true, TS_SLocal.class);

    public static void main(String... s) {
        var jobIsServer = false;
        var killTrigger = TS_ThreadSyncTrigger.of();
        var socketFile = Path.of("d:\\%s.socket".formatted(TS_SLocal.class.getName()));
        if (jobIsServer) {
            runServer(killTrigger, socketFile, receivedText -> {
                d.ci("main", "server", receivedText);
            });
        } else {
            var op = runClient(killTrigger, socketFile, "'Msg from client!");
            if (op.payload) {
                d.ci("main", "client", "sent successful");
            } else {
                d.ce("main", "client", op);
            }
        }
    }

    private static TGS_OptionalBoolean runClient(TS_ThreadSyncTrigger threadKiller, Path socketFile, String msg) {
        return TGS_UnSafe.call(() -> {
            var socketAddress = UnixDomainSocketAddress.of(socketFile);
            var openedChannel = SocketChannel.open(StandardProtocolFamily.UNIX);
            openedChannel.connect(socketAddress);
            return write(threadKiller, openedChannel, msg);
        }, e -> {
            d.ct("runClient", e);
            return TGS_OptionalBoolean.ofFalse(e);
        });
    }

    private static void runServer(TS_ThreadSyncTrigger threadKiller, Path socketFile, TGS_RunnableType1<String> receivedText) {
        TGS_UnSafe.run(() -> {
            Files.deleteIfExists(socketFile);
            var socketAddress = UnixDomainSocketAddress.of(socketFile);
            var serverChannel = ServerSocketChannel.open(StandardProtocolFamily.UNIX);
            serverChannel.bind(socketAddress);
            d.ci("runServer", "waiting...!");
            var acceptedChannel = serverChannel.accept();
            d.ci("runServer", "accepted!");
            while (threadKiller.hasNotTriggered()) {
                var op = read(threadKiller, acceptedChannel);
                if (op.payload.isEmpty()) {
                    d.ce("runServer", op.info);
                    d.ci("runServer", "waiting for new...!");
                    acceptedChannel = serverChannel.accept();
                    d.ci("runServer", "new accepted!");
                    continue;
                } else {
                    receivedText.run(op.payload.get());
                }
                TS_ThreadWait.milliseconds100();
            }
        }, e -> d.ct("runServer", e));
    }

    private static TGS_OptionalBoolean write(TS_ThreadSyncTrigger threadKiller, SocketChannel openedChannel, String msg) {
        return TGS_UnSafe.call(() -> {
            var buffer = ByteBuffer.allocate(1024);
            buffer.clear();
            buffer.put(msg.getBytes());
            buffer.flip();
            while (buffer.hasRemaining() && threadKiller.hasNotTriggered()) {
                openedChannel.write(buffer);
            }
            return TGS_OptionalBoolean.ofTrue();
        }, e -> {
            d.ct("write", e);
            return TGS_OptionalBoolean.ofFalse(e);
        });
    }

    private static TGS_Optional<String> read(TS_ThreadSyncTrigger threadKiller, SocketChannel channel) {
        return TGS_UnSafe.call(() -> {
            var buffer = ByteBuffer.allocate(1024);
            var bytesRead = channel.read(buffer);
            if (bytesRead < 0) {
                return TGS_Optional.of("");
            }
            var bytes = new byte[bytesRead];
            buffer.flip();
            buffer.get(bytes);
            var message = new String(bytes);
            return TGS_Optional.of(message);
        }, e -> {
            d.ct("read", e);
            return TGS_Optional.ofEmpty(e);
        });
    }
}
