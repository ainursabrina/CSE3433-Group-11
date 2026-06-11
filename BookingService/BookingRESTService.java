package service;

import dao.BookingDAO;
import model.Booking;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.http.*;
import java.net.URI;
import java.util.List;

@Path("/bookings")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BookingRESTService {

    BookingDAO dao = new BookingDAO();

    @GET
    public Response getAll() {
        try {
            return Response.ok(dao.getAll()).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError()
                .entity("{\"error\":\"" + e.getMessage() + "\"}").build();
        }
    }

    @GET
    @Path("/user/{userId}")
    public Response getByUser(@PathParam("userId") int userId) {
        try {
            return Response.ok(dao.getByUser(userId)).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError()
                .entity("{\"error\":\"" + e.getMessage() + "\"}").build();
        }
    }

    @POST
    public Response create(Booking b) {
        try {
            if (b.getFacilityId() == 0 || b.getBookingDate() == null) {
                return Response.status(400)
                    .entity("{\"error\":\"facilityId and bookingDate must be filled in\"}")
                    .build();
            }

            // get facility name from FacilityService
            String facilityName = getFacilityName(b.getFacilityId());
            b.setFacilityName(facilityName);

            // check conflict
            boolean conflict = dao.checkConflict(
                b.getFacilityId(), b.getBookingDate(),
                b.getStartTime(), b.getEndTime()
            );
            if (conflict) {
                return Response.status(409)
                    .entity("{\"error\":\"This slot has been booked\"}")
                    .build();
            }

            dao.create(b);

            // Notify user
            callNotification(b.getUserId(),
                "Booking request for " + facilityName +
                " on " + b.getBookingDate() + " is under review.");

            return Response.status(201)
                .entity("{\"message\":\"Booking has been sent\"}").build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError()
                .entity("{\"error\":\"" + e.getMessage() + "\"}").build();
        }
    }

    @PUT
    @Path("/{id}/status")
    public Response updateStatus(
            @PathParam("id") int id,
            @QueryParam("status") String status,
            @QueryParam("userId") int userId) {
        try {
            dao.updateStatus(id, status);
            String msg = status.equals("approved")
                ? "Your booking has been APPROVED!"
                : "Your booking has been REJECTED.";
            callNotification(userId, msg);
            return Response.ok("{\"message\":\"Status updated\"}").build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError()
                .entity("{\"error\":\"" + e.getMessage() + "\"}").build();
        }
    }

    @PUT
    @Path("/{id}/cancel")
    public Response cancel(
            @PathParam("id") int id,
            @QueryParam("userId") int userId) {
        try {
            dao.cancel(id);
            callNotification(userId, "Your booking has been cancelled.");
            return Response.ok("{\"message\":\"Booking cancelled\"}").build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError()
                .entity("{\"error\":\"" + e.getMessage() + "\"}").build();
        }
    }

    // Call FacilityService to get facility name
    private String getFacilityName(int facilityId) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(
                    "http://localhost:8080/FacilityService/api/facilities/" + facilityId))
                .GET().build();
            HttpResponse<String> res = client.send(
                req, HttpResponse.BodyHandlers.ofString());
            String body = res.body();
            // parse nama dari JSON
            int start = body.indexOf("\"name\":\"") + 8;
            int end = body.indexOf("\"", start);
            return start > 7 ? body.substring(start, end) : "Unknown";
        } catch (Exception e) {
            return "Unknown";
        }
    }

    // Call NotificationService
    private void callNotification(int userId, String message) {
        try {
            String json = "{\"userId\":" + userId +
                          ",\"message\":\"" + message + "\"}";
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(
                    "http://localhost:8080/NotificationService/api/notifications"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
            client.sendAsync(req, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            System.out.println("Notification failed: " + e.getMessage());
        }
    }
}