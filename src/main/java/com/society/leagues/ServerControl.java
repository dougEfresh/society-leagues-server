package com.society.leagues;

import com.society.leagues.conf.RestAppConfig;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;

@Component
public class ServerControl {
    static Logger logger = LoggerFactory.getLogger(ServerControl.class);
    @Autowired RestAppConfig app;
    @Value("${daemon:false}")
    boolean daemon;
    @Value("${server.port:8080}")
    int port;
    HttpServer server;
    Thread serverThread;

    public void run(String ...args) throws Exception {
        logger.info("Starting Society League REST service");
        startServer();
        logger.info("Application started.\n" +
                "Try accessing " + getBaseURI() + " in the browser.\n" +
                "Hit crt-c to stop the application...");

        serverThread  = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(100000);
                    } catch (InterruptedException e) {
                        server.shutdown();
                    }
                }
            }
        });
        serverThread.setDaemon(daemon);
        serverThread.start();
    }

    public void startServer() throws Exception {
        server = GrizzlyHttpServerFactory.createHttpServer(
                getBaseURI(),
                app);

        server.start();
    }

    public URI getBaseURI() {
        return UriBuilder.fromUri("http://localhost/").port(port).build();
    }

    public int getPort() {
        for (NetworkListener networkListener : server.getListeners()) {
            return networkListener.getPort();
        }
        return port;
    }

    public void shutdown() {
        serverThread.interrupt();
    }
}
