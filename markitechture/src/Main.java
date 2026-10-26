public class Main {
    public static void main(String[] args) {
        System.out.println("=========================================");
        System.out.println("           HARDWARE INSPECTOR            ");
        System.out.println("=========================================\n");

        // Core Requirement 1: Processor
        ProcessorInfo.report();

        // Core Requirement 2: Memory
        MemoryInfo.report();
    }
}
