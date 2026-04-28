package org.example;

/*
 * Design a traffic data processing engine using java streams + functional interfaces
 * to handle real time vehicle event data
 * Input Data Structure
 *
 * For solving any high level problem
 * First we need to understand the background and then on the basis of the background
 * we will decide the data structures for our input output and the entire data pipeline
 * along with the database and cloud infrastructure.
 *
 * Now we will see the system requirement
 *
 *
 * */

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

public class SmartTrafficSystem {

    //Data Model

    static class VehicleEvent

    {
        String vehicleId;
        double speed;
        String zone;
        boolean isEmergencyVehicle;
        long timestamp;

        public VehicleEvent(String vehicleId, double speed, String zone, boolean isEmergencyVehicle, long timestamp)
        {
            this.vehicleId = vehicleId;
            this.speed = speed;
            this.zone = zone;
            this.isEmergencyVehicle = isEmergencyVehicle;
            this.timestamp = timestamp;
        }
    }// Vehicle Event ends

    static class ViolationRecord
    {
        String vehicleId;
        double speed;
        String zone;
        int fine;

        public ViolationRecord(String vehicleId, double speed, String zone, int fine)
        {
            this.vehicleId = vehicleId;
            this.speed = speed;
            this.zone = zone;
            this.fine = fine;
        }

        @Override
        public String toString()
        {
            return "Vehicle: " + vehicleId + "| Speed" + speed + "| fine: " + fine + "| zone: " + zone;
        }

    }// Vehicle Record ends

    //speed limit rule
    static class TrafficRules
    {
        static Predicate<VehicleEvent> violationFilter = event -> event.speed > 80 && !event.isEmergencyVehicle;
    }

    static Function<Double, Integer> fineCalculator = speed -> {
        if(speed > 120) return 5000;
        else if (speed > 100) return 2000;
        else return 1000;
    };

    public static void saveViolation(ViolationRecord record) {
        String sql = "INSERT INTO violations (vehicle_id, speed, zone, fine) VALUES (?, ?, ?, ?)";

        try (java.sql.Connection conn = DBConnection.getConnection();
             java.sql.PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setString(1, record.vehicleId);
            pst.setDouble(2, record.speed);
            pst.setString(3, record.zone);
            pst.setInt(4, record.fine);
            pst.executeUpdate();

            System.out.println("Saved to DB: " + record.vehicleId);

        } catch (java.sql.SQLException e) {
            System.out.println("DB Error: " + e.getMessage());
        }
    }

    public static void main(String[] args)
    {

        java.sql.Connection conn = DBConnection.getConnection();
        if (conn != null) {
            System.out.println("DB Connected!");
        }

        List<VehicleEvent> events = Arrays.asList(
                new VehicleEvent("MH12AB1234", 95, "PUNE_WEST", true, System.currentTimeMillis()),
                new VehicleEvent("MH12WE1234", 120, "PUNE_WEST", false, System.currentTimeMillis()),
                new VehicleEvent("MH12ER1234", 135, "PUNE_NORTH", false, System.currentTimeMillis()),
                new VehicleEvent("MH12RT1234", 69, "PUNE_WEST", false, System.currentTimeMillis()),
                new VehicleEvent("MH12FG1234", 45, "PUNE_EAST", true, System.currentTimeMillis())
        );

        List<ViolationRecord> violations = events.parallelStream()
                .filter(Objects::nonNull)
                .filter(TrafficRules.violationFilter)
                .map( event -> {
                    String vehicleId = Optional.ofNullable(event.vehicleId)
                            .orElse("UNKNOWN");

                    String zone = Optional.ofNullable(event.zone)
                            .orElse("UNKNOWN");

                    int fine = fineCalculator.apply(event.speed);

                    return new ViolationRecord(vehicleId, event.speed, zone, fine);

                })
                .collect(Collectors.toList());

        //Consumer Logging
        Consumer<ViolationRecord> logger = System.out::println;
        violations.forEach(logger);

        //Aggregation using reduce
        int totalFine = violations.stream()
                .map(v -> v.fine)
                .reduce(0, Integer::sum);

        long totalViolations = violations.stream().count();


    }
}



