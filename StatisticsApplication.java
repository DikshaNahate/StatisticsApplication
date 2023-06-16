package com.statisticsApplication;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StatisticsApplication {
    private static final String DB_URL = "jdbc:mysql://localhost/statistics_db";
    private static final String DB_USERNAME = "root";
    private static final String DB_PASSWORD = "root";

    public static void main(String[] args) {
        try {
            CommandLineArgs cmdArgs = parseCommandLineArgs(args);
            if (cmdArgs != null) {
                if (cmdArgs.getInput() != null) {
                    processInput(cmdArgs.getInput());
                } else if (cmdArgs.getFile() != null) {
                    processFile(cmdArgs.getFile());
                } else {
                    printHelp();
                }
            }
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
            printHelp();
        }
    }

    private static CommandLineArgs parseCommandLineArgs(String[] args) {
        CommandLineArgs cmdArgs = new CommandLineArgs();
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if ("--input".equals(arg) && i + 1 < args.length) {
                cmdArgs.setInput(args[i + 1]);
                i++;
            } else if ("--file".equals(arg) && i + 1 < args.length) {
                cmdArgs.setFile(args[i + 1]);
                i++;
            } else {
                return null; // Invalid argument
            }
        }
        return cmdArgs;
    }

    private static void processInput(String input) {
        List<Integer> numbers = parseInput(input);
        if (numbers != null) {
            double meanValue = computeMean(numbers);
            double medianValue = computeMedian(numbers);
            int id = storeStatistics(meanValue, medianValue);
            displayStatistics(meanValue, medianValue);
            System.out.println("Statistics stored in the database with ID: " + id);
        } else {
            System.out.println("Invalid input format.");
        }
    }

    private static List<Integer> parseInput(String input) {
        List<Integer> numbers = new ArrayList<>();
        String[] numberStrings = input.split(",");
        try {
            for (String numStr : numberStrings) {
                numbers.add(Integer.parseInt(numStr.trim()));
            }
            return numbers;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static double computeMean(List<Integer> numbers) {
        int sum = 0;
        for (int num : numbers) {
            sum += num;
        }
        return (double) sum / numbers.size();
    }

    private static double computeMedian(List<Integer> numbers) {
        int size = numbers.size();
        int[] sortedNumbers = numbers.stream().mapToInt(Integer::intValue).sorted().toArray();
        if (size % 2 == 0) {
            int midIndex1 = size / 2 - 1;
            int midIndex2 = size / 2;
            return (double) (sortedNumbers[midIndex1] + sortedNumbers[midIndex2]) / 2;
        } else {
            int midIndex = size / 2;
            return (double) sortedNumbers[midIndex];
        }
    }

    private static int storeStatistics(double meanValue, double medianValue) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement("INSERT INTO Statistics (Mean, Median) VALUES (?, ?)",
                     Statement.RETURN_GENERATED_KEYS)) {
            stmt.setDouble(1, meanValue);
            stmt.setDouble(2, medianValue);
            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            int id = -1;
            if (rs.next()) {
                id = rs.getInt(1);
            }
            return id;
        } catch (SQLException e) {
            System.out.println("Error storing statistics in the database: " + e.getMessage());
            return -1;
        }
    }

    private static void displayStatistics(double meanValue, double medianValue) {
        System.out.println("Mean: " + meanValue);
        System.out.println("Median: " + medianValue);
    }

    private static void processFile(String filePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String input = reader.readLine();
            if (input != null) {
                processInput(input);
            } else {
                System.out.println("Empty input file.");
            }
        } catch (IOException e) {
            System.out.println("Error reading input file: " + e.getMessage());
        }
    }

    private static void printHelp() {
        System.out.println("Usage: java StatisticsApplication --input <numbers> --file <file-path>");
    }

    private static class CommandLineArgs {
        private String input;
        private String file;

        public String getInput() {
            return input;
        }

        public void setInput(String input) {
            this.input = input;
        }

        public String getFile() {
            return file;
        }

        public void setFile(String file) {
            this.file = file;
        }
    }
}
