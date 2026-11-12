package edu.itc.salesreport.ingest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Random;

public final class SalesDataGenerator {
    private static final List<String> BRANCHES = List.of("PNH", "REP", "BTB");
    private static final List<String> CATEGORIES = List.of("Grocery", "Beverages", "Household", "Personal Care", "Electronics", "Stationery");
    private static final List<String> PAYMENTS = List.of("CASH", "CARD", "KHQR");

    public static void main(String[] args) {
        if (args.length == 4) {
            YearMonth month = YearMonth.parse(args[0]);
            int rowsPerDay = Integer.parseInt(args[1]);
            long seed = Long.parseLong(args[2]);
            if (rowsPerDay <= 0) {
                throw new IllegalArgumentException("rows-per-day must be positive");
            }
            Path rootDir = Path.of(args[3]);
            try {
                generateMonth(rootDir, month, rowsPerDay, seed);
                long rowCount = (long) rowsPerDay * month.lengthOfMonth()
                        * BRANCHES.size();
                System.out.printf("Generated %d rows in %d files under %s%n",
                        rowCount,
                        month.lengthOfMonth() * BRANCHES.size(), rootDir);
            } catch (IOException e) {
                System.err.println("Failed to generate data: " + e.getMessage());
                System.exit(1);
            }
            return;
        }
        if (args.length != 1) {
            System.err.println("Usage: java edu.itc.salesreport.ingest.SalesDataGenerator <output-dir>");
            System.err.println("   or: java edu.itc.salesreport.ingest.SalesDataGenerator <yyyy-MM> <rows-per-day> <seed> <output-dir>");
            System.exit(1);
        }
        Path rootDir = Path.of(args[0]);
        try {
            generateMonth(rootDir, YearMonth.of(2026, 8));
            generateMonth(rootDir, YearMonth.of(2026, 9));
            System.out.println("Sample data generated successfully in " + rootDir);
        } catch (IOException e) {
            System.err.println("Failed to generate data: " + e.getMessage());
        }
    }

    public static void generateMonth(Path rootDir, YearMonth yearMonth) throws IOException {
        generateMonth(rootDir, yearMonth, 0, yearMonth.hashCode());
    }

    public static void generateMonth(Path rootDir, YearMonth yearMonth,
            int rowsPerBranchDay, long seed) throws IOException {
        if (rowsPerBranchDay < 0) {
            throw new IllegalArgumentException("rows-per-day cannot be negative");
        }
        Path monthDir = Files.createDirectories(rootDir.resolve(yearMonth.toString()));
        Random random = new Random(seed);
        int days = yearMonth.lengthOfMonth();

        for (int day = 1; day <= days; day++) {
            LocalDate date = yearMonth.atDay(day);
            for (String branch : BRANCHES) {
                Path file = monthDir.resolve(branch + "-" + date + ".csv");
                StringBuilder sb = new StringBuilder("branch,date,receipt_no,sku,product_name,category,quantity,unit_price,discount,payment_method\n");
                
                int rows = rowsPerBranchDay == 0
                        ? 15 + random.nextInt(15) : rowsPerBranchDay;
                for (int r = 1; r <= rows; r++) {
                    String receiptNo = String.format("%s-%06d", branch, r);
                    String sku = "SKU-" + (1000 + random.nextInt(5000));
                    String productName = "Product " + sku;
                    String category = CATEGORIES.get(random.nextInt(CATEGORIES.size()));
                    int qty = 1 + random.nextInt(5);
                    double unitPrice = 1.0 + (random.nextDouble() * 20.0);
                    double discount = random.nextDouble() < 0.2 ? 0.50 : 0.00;
                    String payment = PAYMENTS.get(random.nextInt(PAYMENTS.size()));

                    sb.append(String.format("%s,%s,%s,%s,%s,%s,%d,%.2f,%.2f,%s\n",
                            branch, date, receiptNo, sku, productName, category, qty, unitPrice, discount, payment));
                }
                Files.writeString(file, sb.toString());
            }
        }
    }
}