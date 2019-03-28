/*
 * Copyright 2019 Alex Thomson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.lxgaming.keepalive.handler;

import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelOutboundHandler;
import io.netty.channel.ChannelPromise;

import java.net.SocketAddress;

public abstract class AbstractHandler extends ChannelHandlerAdapter implements ChannelInboundHandler, ChannelOutboundHandler {
    
    @Override
    public final void bind(ChannelHandlerContext ctx, SocketAddress localAddress, ChannelPromise promise) {
        ctx.bind(localAddress, promise);
    }
    
    @Override
    public final void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) {
        ctx.connect(remoteAddress, localAddress, promise);
    }
    
    @Override
    public final void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) {
        ctx.disconnect(promise);
    }
    
    @Override
    public final void close(ChannelHandlerContext ctx, ChannelPromise promise) {
        ctx.close(promise);
    }
    
    @Override
    public final void deregister(ChannelHandlerContext ctx, ChannelPromise promise) {
        ctx.deregister(promise);
    }
    
    @Override
    public final void read(ChannelHandlerContext ctx) {
        ctx.read();
    }
    
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        ctx.write(msg, promise);
    }
    
    @Override
    public final void flush(ChannelHandlerContext ctx) {
        ctx.flush();
    }
    
    @Override
    public final void channelRegistered(ChannelHandlerContext ctx) {
        ctx.fireChannelRegistered();
    }
    
    @Override
    public final void channelUnregistered(ChannelHandlerContext ctx) {
        ctx.fireChannelUnregistered();
    }
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        ctx.fireChannelActive();
    }
    
    @Override
    public final void channelInactive(ChannelHandlerContext ctx) {
        ctx.fireChannelInactive();
    }
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ctx.fireChannelRead(msg);
    }
    
    @Override
    public final void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.fireChannelReadComplete();
    }
    
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        ctx.fireUserEventTriggered(evt);
    }
    
    @Override
    public final void channelWritabilityChanged(ChannelHandlerContext ctx) {
        ctx.fireChannelWritabilityChanged();
    }
    
    @Override
    protected final void ensureNotSharable() {
        super.ensureNotSharable();
    }
    
    @Override
    public final boolean isSharable() {
        return super.isSharable();
    }
    
    @Override
    public final void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        super.handlerAdded(ctx);
    }
    
    @Override
    public final void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        super.handlerRemoved(ctx);
    }
    
    @Override
    public final void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        try {
            super.exceptionCaught(ctx, cause);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}