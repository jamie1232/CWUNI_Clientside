package uk.ac.westminster.smartcampus.resource;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import uk.ac.westminster.smartcampus.datastore.InMemoryDataStore;
import uk.ac.westminster.smartcampus.exception.RoomNotEmptyException;
import uk.ac.westminster.smartcampus.model.Room;

@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoomResource {

    private final InMemoryDataStore store = InMemoryDataStore.getInstance();

    @GET
    public Response getAllRooms() {
        Map<String, Room> rooms = store.getRooms();
        List<Room> list = new ArrayList<>(rooms.values());
        return Response.ok(list).build();
    }

    @POST
    public Response createRoom(Room room, @Context UriInfo uriInfo) {
        if (room.getId() == null || room.getId().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Room id is required")
                    .build();
        }
        store.getRooms().put(room.getId(), room);

        URI location = uriInfo.getAbsolutePathBuilder()
                .path(room.getId())
                .build();
        return Response.created(location).entity(room).build();
    }

    @GET
    @Path("{roomId}")
    public Response getRoom(@PathParam("roomId") String roomId) {
        Room room = store.getRooms().get(roomId);
        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(room).build();
    }

    @DELETE
    @Path("{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        Room room = store.getRooms().get(roomId);
        if (room == null) {
            // Idempotent: deleting a non-existing room still returns 204
            return Response.noContent().build();
        }
        if (room.getSensorIds() != null && !room.getSensorIds().isEmpty()) {
            throw new RoomNotEmptyException("Room " + roomId + " still has active sensors.");
        }
        store.getRooms().remove(roomId);
        return Response.noContent().build();
    }
}