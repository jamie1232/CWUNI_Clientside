package uk.ac.westminster.smartcampus;

import java.io.IOException;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

public class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
    private static final String BASE_URI = "http://0.0.0.0:8080/api/v1/";

    public static void main(String[] args) throws IOException {
        ResourceConfig rc = new ResourceConfig().packages("uk.ac.westminster.smartcampus");
        HttpServer server = GrizzlyHttpServerFactory.createHttpServer(URI.create(BASE_URI), rc);

        LOGGER.info(() -> "Smart Campus API started at " + BASE_URI);
        LOGGER.info("Press Ctrl+C to stop the server.");

        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            LOGGER.log(Level.SEVERE, "Server interrupted", e);
        } finally {
            server.shutdownNow();
        }
    }
}