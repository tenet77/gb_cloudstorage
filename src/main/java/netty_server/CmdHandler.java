package netty_server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class CmdHandler extends SimpleChannelInboundHandler<String> {

        @Override
        public void channelRead0(ChannelHandlerContext ctx, String msg) { // (2)
            ctx.writeAndFlush(msg);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) { // (4)
            cause.printStackTrace();
            ctx.close();
        }

}
