import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class MemoryInfo {

    // Reads /proc/meminfo, calculates used memory and percentage, and prints the report
    public static void report() {
        long memTotalKB = -1;      // Sentinel value to detect if field was found
        long memAvailableKB = -1;

        try (BufferedReader reader = new BufferedReader(new FileReader("/proc/meminfo"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Example line: "MemTotal:       32685880 kB"
                if (line.startsWith("MemTotal:")) {
                    String[] parts = line.split("\\s+");
                    memTotalKB = Long.parseLong(parts[1]);
                } else if (line.startsWith("MemAvailable:")) {
                    // Example line: "MemAvailable:   26075660 kB"
                    String[] parts = line.split("\\s+");
                    memAvailableKB = Long.parseLong(parts[1]);
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading /proc/meminfo: " + e.getMessage());
            return;
        }

        // Defensive check: ensure both values were successfully read
        if (memTotalKB == -1 || memAvailableKB == -1) {
            System.out.println("Could not parse memory information from /proc/meminfo.");
            return;
        }

        // Calculations
        long memUsedKB = memTotalKB - memAvailableKB;
        double percentUsed = ((double) memUsedKB / memTotalKB) * 100.0;

        // Convert kB to GiB (dividing by 1024 twice: kB -> MiB -> GiB)
        double totalGiB = memTotalKB / 1024.0 / 1024.0;
        double usedGiB = memUsedKB / 1024.0 / 1024.0;
        double availableGiB = memAvailableKB / 1024.0 / 1024.0;

        // Output presentation
        System.out.println("=== Memory Information ===");
        System.out.printf("Total Memory:     %.2f GiB (%,d kB)%n", totalGiB, memTotalKB);
        System.out.printf("Used Memory:      %.2f GiB (%,d kB)%n", usedGiB, memUsedKB);
        System.out.printf("Available Memory: %.2f GiB (%,d kB)%n", availableGiB, memAvailableKB);
        System.out.printf("Memory In Use:    %.1f%%%n%n", percentUsed);
    }
}
