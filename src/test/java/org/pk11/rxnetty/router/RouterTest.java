package org.pk11.rxnetty.router;

import static org.pk11.rxnetty.router.Dispatch.using;
import static org.pk11.rxnetty.router.Dispatch.withParams;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import io.reactivex.netty.protocol.http.server.HttpServer;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import io.reactivex.netty.protocol.http.server.RequestHandler;
import io.reactivex.netty.protocol.http.server.file.ClassPathFileRequestHandler;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

import rx.Observable;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;

public class RouterTest {

	public static class HelloHandler implements RequestHandler<ByteBuf, ByteBuf> {
		public Observable<Void> handle(HttpServerRequest<ByteBuf> request, HttpServerResponse<ByteBuf> response) {
			response.setStatus(HttpResponseStatus.OK);
			response.writeString("Hello!");
			return response.close();
		}
	}

	public static class Handler404 implements RequestHandler<ByteBuf, ByteBuf> {
		public Observable<Void> handle(HttpServerRequest<ByteBuf> request, HttpServerResponse<ByteBuf> response) {
			response.setStatus(HttpResponseStatus.NOT_FOUND);
			response.writeString("Not found!");
			return response.close();
		}
	}

	public HttpServer<ByteBuf, ByteBuf> newServer() {
		return RxNetty.createHttpServer(0, using(
					new Router<ByteBuf, ByteBuf>()
					.GET("/hello", new HelloHandler())
					.GET("/article/:id", withParams( (params, request, response)->{
						response.setStatus(HttpResponseStatus.OK);
            			response.writeString("params:"+ params.get("id"));
            			return response.close();
					}))
					.GET("/public/:*", new ClassPathFileRequestHandler("www"))
					.notFound(new Handler404())
					));
	}

	@Test
	public void shouldReturnHello() throws Exception {
		final CountDownLatch finishLatch = new CountDownLatch(1);
		HttpServer<ByteBuf, ByteBuf> server = newServer().start();

		HttpClientResponse<ByteBuf> response = RxNetty.createHttpClient("localhost", server.getServerPort())
				.submit(HttpClientRequest.createGet("/hello")).finallyDo(new Action0() {
					@Override
					public void call() {
						finishLatch.countDown();
					}
				}).toBlocking().toFuture().get(10, TimeUnit.SECONDS);
		finishLatch.await(1, TimeUnit.MINUTES);
		Assert.assertTrue(response.getStatus().code() == 200);

	}

	@Test
	public void shouldReturnAsset() throws Exception {
		final CountDownLatch finishLatch = new CountDownLatch(1);
		HttpServer<ByteBuf, ByteBuf> server = newServer().start();

		HttpClientResponse<ByteBuf> response = RxNetty.createHttpClient("localhost", server.getServerPort())
				.submit(HttpClientRequest.createGet("/public/index.html")).finallyDo(new Action0() {
					@Override
					public void call() {
						finishLatch.countDown();
					}
				}).toBlocking().toFuture().get(10, TimeUnit.SECONDS);
		finishLatch.await(1, TimeUnit.MINUTES);
		Assert.assertTrue(response.getStatus().code() == 200);

	}

	@Test
	public void shouldCaptureParam() throws Exception {
		HttpServer<ByteBuf, ByteBuf> server = newServer().start();

		Observable<HttpClientResponse<ByteBuf>> response = RxNetty
				.createHttpClient("localhost", server.getServerPort()).submit(
						HttpClientRequest.createGet("/article/yay"));
		final List<String> result = new ArrayList<String>();
		response.flatMap(new Func1<HttpClientResponse<ByteBuf>, Observable<String>>() {
			@Override
			public Observable<String> call(HttpClientResponse<ByteBuf> response) {
				return response.getContent().map(new Func1<ByteBuf, String>() {
					@Override
					public String call(ByteBuf byteBuf) {
						return byteBuf.toString(Charset.defaultCharset());
					}
				});
			}
		}).toBlocking().forEach(new Action1<String>() {

			@Override
			public void call(String t1) {
				result.add(t1);
			}
		});
		Assert.assertTrue(result.get(0).equals("params:yay"));

	}

	@Test
	public void shouldReturn404ForWrongAssetLink() throws Exception {
		final CountDownLatch finishLatch = new CountDownLatch(1);
		HttpServer<ByteBuf, ByteBuf> server = newServer().start();

		HttpClientResponse<ByteBuf> response = RxNetty.createHttpClient("localhost", server.getServerPort())
				.submit(HttpClientRequest.createGet("/public/index.html1")).finallyDo(new Action0() {
					@Override
					public void call() {
						finishLatch.countDown();
					}
				}).toBlocking().toFuture().get(10, TimeUnit.SECONDS);
		finishLatch.await(1, TimeUnit.MINUTES);
		Assert.assertTrue(response.getStatus().code() == 404);

	}

	@Test
	public void shouldReturn404ForWrongResource() throws Exception {
		final CountDownLatch finishLatch = new CountDownLatch(1);
		HttpServer<ByteBuf, ByteBuf> server = newServer().start();

		HttpClientResponse<ByteBuf> response = RxNetty.createHttpClient("localhost", server.getServerPort())
				.submit(HttpClientRequest.createGet("sdfsdfd")).finallyDo(new Action0() {
					@Override
					public void call() {
						finishLatch.countDown();
					}
				}).toBlocking().toFuture().get(10, TimeUnit.SECONDS);
		finishLatch.await(1, TimeUnit.MINUTES);
		Assert.assertTrue(response.getStatus().code() == 404);

	}

}
