package org.pk11.rxnetty.router;

import java.util.HashMap;
import java.util.Map;

import io.reactivex.netty.channel.Handler;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;

import rx.Observable;

/**
 * creates a Handler that comes with matched URL params
 */
@FunctionalInterface
public interface Route<I,O> extends Handler<HttpServerRequest<I>, HttpServerResponse<O>> {

   default Observable<Void> handle(HttpServerRequest<I> request, HttpServerResponse<O> response) {
   	   return handle(new HashMap<String,String>(), request, response);
   }
   	
   Observable<Void> handle(Map<String, String> params, HttpServerRequest<I> request, HttpServerResponse<O> response);
}
