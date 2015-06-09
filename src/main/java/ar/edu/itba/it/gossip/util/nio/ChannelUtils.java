package ar.edu.itba.it.gossip.util.nio;

import java.io.IOException;
import java.nio.channels.SocketChannel;

public abstract class ChannelUtils {
    public static void closeQuietly(SocketChannel channel) {
        try {
            channel.close();
        } catch (IOException ignore) {
        }
    }
}
