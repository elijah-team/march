// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.io

import com.intellij.openapi.diagnostic.debug
import com.intellij.openapi.diagnostic.logger
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.QueryStringDecoder

@Deprecated("The class doesn't provide real value. Consider implementing [org.jetbrains.ide.HttpRequestHandler] instead.", level = DeprecationLevel.ERROR)
abstract class DelegatingHttpRequestHandlerBase : SimpleChannelInboundHandlerAdapter<FullHttpRequest>() {
  override fun messageReceived(context: ChannelHandlerContext, message: FullHttpRequest) {
    logger<BuiltInServer>().debug { "\n\nIN HTTP: $message\n\n" }

    if (!process(context, message, QueryStringDecoder(message.uri()))) {
      createStatusResponse(HttpResponseStatus.NOT_FOUND, message).send(context.channel(), message)
    }
  }

  protected abstract fun process(context: ChannelHandlerContext, request: FullHttpRequest, urlDecoder: QueryStringDecoder): Boolean

  override fun exceptionCaught(context: ChannelHandlerContext, cause: Throwable) {
    NettyUtil.logAndClose(cause, logger<BuiltInServer>(), context.channel())
  }
}
