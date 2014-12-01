rxnetty-router
==============

A tiny HTTP router for [RxNetty] (https://github.com/ReactiveX/RxNetty). 

rxnetty-router currently requires java8 and it's using [jauter] (https://github.com/sinetja/jauter) under the hood.

How to Install
==============
maven:
```
<repositories>
    <repository>
        <id>org.pk11</id>
        <url>http://pk11-scratch.googlecode.com/svn/trunk</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>org.pk11.rxnetty</groupId>
        <artifactId>rxnetty-router</artifactId>
        <version>0.1</version>
    </dependency>
</dependencies>
```

Example
=======

```java
import static org.pk11.rxnetty.router.Dispatch.using;
import static org.pk11.rxnetty.router.Dispatch.withParams;
(...)
HttpServer<ByteBuf, ByteBuf> server = RxNetty.createHttpServer(0, using(
					new Router<ByteBuf, ByteBuf>() 
					.GET("/hello", new HelloHandler())
					.GET("/article/:id", withParams( (params, request, response)->{
						response.setStatus(HttpResponseStatus.OK);
            			response.writeString("params:"+ params.get("id"));
            			return response.close();
					}))
					.GET("/public/:*", new ClassPathFileRequestHandler("www"))
					.notFound(new Handler404())
					)).start();

```

See [RouterTest](https://github.com/pk11/rxnetty-router/blob/master/src/test/java/org/pk11/rxnetty/router/RouterTest.java) for a full example.

