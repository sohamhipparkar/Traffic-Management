package org.example;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.*;

public class TrafficUI {

    public static void main(String[] args) throws Exception {

        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/process", (HttpExchange exchange) -> {

            if ("GET".equals(exchange.getRequestMethod())) {

                Map<String, String> params = queryToMap(exchange.getRequestURI().getQuery());

                try {
                    String id = params.get("id");
                    double speed = Double.parseDouble(params.get("speed"));
                    String zone = params.get("zone");
                    boolean isEmergency = Boolean.parseBoolean(params.get("emergency"));

                    SmartTrafficSystem.VehicleEvent event =
                            new SmartTrafficSystem.VehicleEvent(
                                    id, speed, zone, isEmergency,
                                    System.currentTimeMillis()
                            );

                    List<SmartTrafficSystem.VehicleEvent> events = Arrays.asList(event);

                    List<SmartTrafficSystem.ViolationRecord> violations =
                            events.stream()
                                    .filter(SmartTrafficSystem.TrafficRules.violationFilter)
                                    .map(ev -> new SmartTrafficSystem.ViolationRecord(
                                            ev.vehicleId,
                                            ev.speed,
                                            ev.zone,
                                            SmartTrafficSystem.fineCalculator.apply(ev.speed)
                                    ))
                                    .toList();

                    String response;

                    if (violations.isEmpty()) {
                        response = "No violation detected";
                    } else {
                        SmartTrafficSystem.ViolationRecord v = violations.get(0);
                        SmartTrafficSystem.saveViolation(v);
                        response = "Violation: " + v.toString();
                    }

                    exchange.sendResponseHeaders(200, response.length());
                    OutputStream os = exchange.getResponseBody();
                    os.write(response.getBytes());
                    os.close();

                } catch (Exception ex) {
                    String response = "Invalid input";
                    exchange.sendResponseHeaders(400, response.length());
                    exchange.getResponseBody().write(response.getBytes());
                    exchange.close();
                }
            }
        });

        server.start();
        System.out.println("Server running on port " + port);
    }

    private static Map<String, String> queryToMap(String query) {
        Map<String, String> result = new HashMap<>();
        if (query == null) return result;

        for (String param : query.split("&")) {
            String[] pair = param.split("=");
            if (pair.length > 1) {
                result.put(pair[0], pair[1]);
            }
        }
        return result;
    }
}