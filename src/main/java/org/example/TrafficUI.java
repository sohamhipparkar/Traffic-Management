package org.example;

import javax.swing.*;
import java.awt.GridLayout;
import java.awt.event.*;
import java.util.*;


public class TrafficUI {

    public static void main(String[] args) {

        JFrame frame = new JFrame("Smart Traffic System");
        frame.setSize(500, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Layout
        frame.setLayout(new GridLayout(6, 2));

        JTextField vehicleField = new JTextField();
        JTextField speedField = new JTextField();
        JTextField zoneField = new JTextField();
        JCheckBox emergencyBox = new JCheckBox("Emergency Vehicle");

        JTextArea outputArea = new JTextArea();
        outputArea.setEditable(false);

        JButton processBtn = new JButton("Process Event");

        // Add Components
        frame.add(new JLabel("Vehicle ID:"));
        frame.add(vehicleField);

        frame.add(new JLabel("Speed:"));
        frame.add(speedField);

        frame.add(new JLabel("Zone:"));
        frame.add(zoneField);

        frame.add(emergencyBox);
        frame.add(new JLabel(""));

        frame.add(processBtn);

        frame.add(new JScrollPane(outputArea));

        // Button Logic
        processBtn.addActionListener(e -> {
            try {
                String id = vehicleField.getText();
                double speed = Double.parseDouble(speedField.getText());
                String zone = zoneField.getText();
                boolean isEmergency = emergencyBox.isSelected();

                SmartTrafficSystem.VehicleEvent event =
                        new SmartTrafficSystem.VehicleEvent(
                                id, speed, zone, isEmergency,
                                System.currentTimeMillis()
                        );

                List<SmartTrafficSystem.VehicleEvent> events =
                        Arrays.asList(event);

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

                if (violations.isEmpty()) {
                    outputArea.setText("No violation detected.");
                } else {
                    SmartTrafficSystem.ViolationRecord v = violations.get(0);
                    outputArea.setText(v.toString());

                    // SAVE TO DATABASE
                    SmartTrafficSystem.saveViolation(v);
                    outputArea.append("\n✅ Saved to database!");
                }

            } catch (Exception ex) {
                outputArea.setText("Invalid input!");
            }
        });

        frame.setVisible(true);
    }
}

