package uk.ac.westminster.smartcampus.resource;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import uk.ac.westminster.smartcampus.datastore.InMemoryDataStore;
import uk.ac.westminster.smartcampus.exception.LinkedResourceNotFoundException;
import uk.ac.westminster.smartcampus.model.Room;
import uk.ac.westminster.smartcampus.model.Sensor;

@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

    private final InMemoryDataStore store = InMemoryDataStore.getInstance();

    @GET
    public Response getSensors(@QueryParam("type") String type) {
        List<Sensor> all = new ArrayList<>(store.getSensors().values());
        if (type == null || type.isBlank()) {
            return Response.ok(all).build();
        }
        List<Sensor> filtered = all.stream()
                .filter(s -> Objects.equals(type, s.getType()))
                .toList();
        return Response.ok(filtered).build();
    }

    @GET
    @Path("{sensorId}")
    public Response getSensor(@PathParam("sensorId") String sensorId) {
        Sensor sensor = store.getSensors().get(sensorId);
        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"code\":\"NOT_FOUND\",\"message\":\"Sensor not found.\",\"details\":\"Sensor " + sensorId + " does not exist.\"}")
                    .build();
        }
        return Response.ok(sensor).build();
    }

    @POST
    public Response createSensor(Sensor sensor, @Context UriInfo uriInfo) {
        if (sensor.getRoomId() == null || sensor.getRoomId().isBlank()) {
            throw new LinkedResourceNotFoundException("roomId is required and must reference an existing room.");
        }

        Room room = store.getRooms().get(sensor.getRoomId());
        if (room == null) {
            throw new LinkedResourceNotFoundException("Room " + sensor.getRoomId() + " does not exist.");
        }

        if (sensor.getId() == null || sensor.getId().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Sensor id is required")
                    .build();
        }

        store.getSensors().put(sensor.getId(), sensor);
        // Link sensor to room
        room.getSensorIds().add(sensor.getId());

        URI location = uriInfo.getAbsolutePathBuilder()
                .path(sensor.getId())
                .build();
        return Response.created(location).entity(sensor).build();
    }

    @Path("{sensorId}/readings")
    public SensorReadingResource readings(@PathParam("sensorId") String sensorId) {
        // Sub-resource locator pattern
        Sensor sensor = store.getSensors().get(sensorId);
        if (sensor == null) {
            throw new LinkedResourceNotFoundException("Sensor " + sensorId + " does not exist.");
        }
        return new SensorReadingResource(sensorId);
    }

    @DELETE
    @Path("{sensorId}")
    public Response deleteSensor(@PathParam("sensorId") String sensorId) {
        Sensor sensor = store.getSensors().remove(sensorId);
        if (sensor == null) {
            // Idempotent: deleting a non-existing sensor still returns 204
            return Response.noContent().build();
        }

        // Remove sensor id from its room
        Room room = store.getRooms().get(sensor.getRoomId());
        if (room != null && room.getSensorIds() != null) {
            room.getSensorIds().remove(sensorId);
        }

        return Response.noContent().build();
    }
}