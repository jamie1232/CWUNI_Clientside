package uk.ac.westminster.smartcampus.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.HashMap;
import java.util.Map;

@Path("/")
public class DiscoveryResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response discover() {
        Map<String, Object> body = new HashMap<>();
        body.put("apiVersion", "v1");
        body.put("description", "Smart Campus Sensor & Room Management API");
        body.put("contact", "smartcampus-support@example.com");

        Map<String, String> resources = new HashMap<>();
        resources.put("rooms", "/api/v1/rooms");
        resources.put("sensors", "/api/v1/sensors");
        body.put("resources", resources);

        return Response.ok(body).build();
    }
}