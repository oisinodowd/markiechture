import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ProcessorInfo {

    // Simple container to hold a core's reading at a point in time
    private static class CpuSnapshot {
        String name; // e.g. "cpu0"
        long total;  // sum of all tick numbers
        long idle;   // idle + iowait ticks

        CpuSnapshot(String name, long total, long idle) {
            this.name = name;
            this.total = total;
            this.idle = idle;
        }
    }

    // 1. Reads /proc/cpuinfo to find the CPU model name
    public static String getModelName() {
        String model = "Unknown (Model name not found)";
        try (BufferedReader reader = new BufferedReader(new FileReader("/proc/cpuinfo"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("model name")) {
                    int colonIndex = line.indexOf(':');
                    if (colonIndex != -1) { //Checking if coon index is -1 because indexOf returns -1 if char is not found
                        model = line.substring(colonIndex + 1).trim();
                        break; // Found the model name, stop reading
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading /proc/cpuinfo: " + e.getMessage());
        }
        return model;
    }

    // 2. Reads /proc/cpuinfo and counts lines starting with "processor" to get core count
    public static int getCoreCount() {
        int cores = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader("/proc/cpuinfo"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("processor")) {
                    cores++;
                }
            }
        } catch (IOException e) {
            System.out.println("Error counting cores: " + e.getMessage());
        }
        return cores;
    }

    // 3. Reads /proc/stat once and grabs total & idle ticks for each core
    private static List<CpuSnapshot> readCpuSnapshots() {
        List<CpuSnapshot> snapshots = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader("/proc/stat"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.trim().split("\\s+");
                // Look for per-core lines like "cpu0", "cpu1", etc. (skip overall "cpu")
                if (parts[0].startsWith("cpu") && !parts[0].equals("cpu")) {
                    long total = 0;
                    // Sum all tick counters from parts[1] onwards
                    for (int i = 1; i < parts.length; i++) {
                        total += Long.parseLong(parts[i]);
                    }
                    // Idle ticks are position 4 (idle) and 5 (iowait)
                    long idle = Long.parseLong(parts[4]) + Long.parseLong(parts[5]);
                    snapshots.add(new CpuSnapshot(parts[0], total, idle));
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading /proc/stat: " + e.getMessage());
        }
        return snapshots;
    }

    // 4. Reads /proc/stat twice (1 second apart), computes load, and prints the report
    public static void report() {
        System.out.println("=== Processor Information ===");
        System.out.println("Model: " + getModelName());
        System.out.println("Cores: " + getCoreCount());

        System.out.println("\nMeasuring CPU load over 1 second...");
        List<CpuSnapshot> first = readCpuSnapshots();

        try {
            Thread.sleep(1000); // Wait 1 second
        } catch (InterruptedException e) {
            System.out.println("Sleep interrupted: " + e.getMessage());
        }

        List<CpuSnapshot> second = readCpuSnapshots();

        if (first.isEmpty() || second.isEmpty() || first.size() != second.size()) {
            System.out.println("Could not calculate CPU load.");
            return;
        }

        System.out.println("\nPer-Core Load:");
        double totalLoad = 0.0;

        for (int i = 0; i < first.size(); i++) {
            CpuSnapshot snap1 = first.get(i);
            CpuSnapshot snap2 = second.get(i);

            long totalDelta = snap2.total - snap1.total;
            long idleDelta = snap2.idle - snap1.idle;
            long busyDelta = totalDelta - idleDelta;

            double loadPercent = (totalDelta > 0) ? ((double) busyDelta / totalDelta) * 100.0 : 0.0;
            totalLoad += loadPercent;

            System.out.printf("  %-6s: %6.2f%%%n", snap1.name, loadPercent);
        }

        double averageLoad = totalLoad / first.size();
        System.out.println("-----------------------------");
        System.out.printf("Average CPU Load: %.2f%%%n%n", averageLoad);
    }
}
