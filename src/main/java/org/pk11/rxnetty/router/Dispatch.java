package org.pk11.rxnetty.router;

import io.reactivex.netty.channel.Handler;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import io.reactivex.netty.protocol.http.server.RequestHandler;

import jauter.Routed;

import rx.Observable;

/*
 * Creates a special RequestHandler that hooks up jauter's Router with rxnetty.
 * If a handler is interested in the routing information, then Router should be implemented,
 * otherwise the router falls back to dispatch the request as usual (i.e. via a standard RequestHandler).
 *
 * Example:
 *
 * {@code
 *	HttpServer<ByteBuf, ByteBuf> server = RxNetty.createHttpServer(8080, using(
 *		new Router<ByteBuf, ByteBuf>()
 *      .GET("/public", new ClassPathFileRequestHandler("public"))
 *		.GET("/articles/:id", withParams(params, request, response)-> {
 *	  							response.setStatus(HttpResponseStatus.OK);
 *               				response.writeString("Path Requested =>: " + request.getPath() + '\n');
 *               				return response.close();}));
 * }
 *
 */

public class Dispatch<I, O> implements RequestHandler<I, O> {

	private final Router<I, O> r;

	private Dispatch(Router<I, O> r) {
		this.r = r;
	}

	/*
	 * DSL for creating an rxnetty-router
	 * 
	 * @param user supplied router
	 */
	public static <I, O> Dispatch<I, O> using(Router<I, O> r) {
		return new Dispatch<I, O>(r);
	}

	/**
	 * provides a lambda friendly API for defining a Route
	 * 
	 * @param route
	 */
	public static <I, O> Handler<HttpServerRequest<I>, HttpServerResponse<O>> withParams(Route<I, O> route) {
		return route;
	}

	@Override
	public Observable<Void> handle(HttpServerRequest<I> request, HttpServerResponse<O> response) {
		Routed<Handler<HttpServerRequest<I>, HttpServerResponse<O>>> routed = r.route(request.getHttpMethod(),
				request.getPath());
		if (routed.target() instanceof Route) {
			return ((Route<I, O>) routed.target()).handle(routed.params(), request, response);
		} else
			return routed.target().handle(request, response);
	}
}
