# Markitechture — Java Concept Notes: Processor & Memory

> [!IMPORTANT]
> These notes cover every Java concept you need for **Core Requirements 1 (Processor)** and **2 (Memory)**.
> They assume you know basics (variables, loops, `if/else`, methods, classes) but need to level up on file I/O, string parsing, and timing.

---

## Part 1: The Processor Requirement

### 1.1 What the Brief Asks You to Do

1. Read `/proc/cpuinfo` → report the **processor model** and **number of cores**
2. Read `/proc/stat` **twice** (one second apart) → calculate **CPU load per core** and **average load**

---

### 1.2 Understanding the Data Files

#### `/proc/cpuinfo` — What It Looks Like

```
processor	: 0
vendor_id	: GenuineIntel
model name	: 12th Gen Intel(R) Core(TM) i7-12700KF
cpu cores	: 12
...

processor	: 1
vendor_id	: GenuineIntel
model name	: 12th Gen Intel(R) Core(TM) i7-12700KF
...
```

- There is **one block per logical processor** (logical = including hyperthreads), separated by blank lines.
- The `processor` line gives the number (0, 1, 2, …). **Counting these lines gives you the number of logical CPUs.**
- The `model name` line gives the human-readable CPU name.
- The `cpu cores` line tells you physical cores (but the brief says to count `processor` lines).

> [!NOTE]
> On ARM machines (arm64), there is **no `model name` line**. The brief says to fall back to `CPU implementer` and `CPU part` fields, or just print "model not available". Your code should handle this gracefully — don't assume the line exists.

#### `/proc/stat` — What It Looks Like

```
cpu  77644 142 22446 3695108 5368 10632 2959 0 0 0
cpu0 2566 1 934 185221 75 269 1229 0 0 0
cpu1 840 0 376 189415 33 81 255 0 0 0
...
```

Each `cpuN` line has **10 numbers** after the label. They represent time (in "jiffies") that this core has spent in different states **since boot**:

| Position | Field     | Meaning                           | Busy or Idle? |
|----------|-----------|-----------------------------------|---------------|
| 1        | user      | Normal user processes             | **Busy**      |
| 2        | nice      | Low-priority user processes       | **Busy**      |
| 3        | system    | Kernel / OS work                  | **Busy**      |
| 4        | idle      | Doing nothing                     | **Idle**      |
| 5        | iowait    | Waiting for disk I/O              | **Idle**      |
| 6        | irq       | Hardware interrupts               | **Busy**      |
| 7        | softirq   | Software interrupts               | **Busy**      |
| 8        | steal     | Time stolen by virtualisation     | **Busy**      |
| 9        | guest     | Running a virtual CPU             | **Busy**      |
| 10       | guest_nice| Running a niced virtual CPU       | **Busy**      |

**Key formula:**
```
total  = user + nice + system + idle + iowait + irq + softirq + steal + guest + guest_nice
busy   = total - idle - iowait
```

Since these are cumulative since boot, a single reading is useless for "current" load. You need **two readings** and then:

```
load % = (busy₂ - busy₁) / (total₂ - total₁) × 100
```

---

### 1.3 Java Concepts You Need for the Processor

#### A) Reading a File Line by Line — `BufferedReader`

This is the most fundamental skill for this project. Linux "files" like `/proc/cpuinfo` are read exactly like normal text files.

```java
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class Example {
    public static void main(String[] args) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader("/proc/cpuinfo"));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
            reader.close();
        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
        }
    }
}
```

**What's happening:**
- `FileReader` opens the file for reading characters.
- `BufferedReader` wraps it to give you the `readLine()` method, which returns one line at a time, or `null` when the file ends.
- The `while` loop reads every line until `null`.
- `IOException` is a **checked exception** — Java forces you to handle it with `try/catch` (or declare `throws`).

> [!TIP]
> **try-with-resources** is a cleaner pattern that auto-closes the reader:
> ```java
> try (BufferedReader reader = new BufferedReader(new FileReader("/proc/cpuinfo"))) {
>     String line;
>     while ((line = reader.readLine()) != null) {
>         // process line
>     }
> } catch (IOException e) {
>     System.out.println("Error: " + e.getMessage());
> }
> ```
> The reader is automatically closed when the `try` block finishes, even if an exception is thrown. This is the recommended approach.

#### B) String Methods You'll Use Constantly

When you have a line like `"model name\t: 12th Gen Intel(R) Core(TM) i7-12700KF"`, you need to extract the value. Here are the string methods that matter:

##### `startsWith(String prefix)`
Check if a line begins with a certain label:
```java
if (line.startsWith("model name")) {
    // this is the line we want
}
```

##### `contains(String s)`
Check if a string contains a substring anywhere:
```java
if (line.contains("model name")) { ... }
```

##### `split(String regex)`
Split a string into an array of parts. This is **critical** for parsing `/proc/stat`.

```java
// Splitting by whitespace
String line = "cpu0 2566 1 934 185221 75 269 1229 0 0 0";
String[] parts = line.split("\\s+");
// parts[0] = "cpu0"
// parts[1] = "2566"
// parts[2] = "1"
// ... and so on
```

`"\\s+"` is a **regular expression** meaning "one or more whitespace characters" (spaces, tabs). You don't need to deeply understand regex — just know these common patterns:

| Pattern    | Meaning                          | Use case                        |
|------------|----------------------------------|---------------------------------|
| `"\\s+"`   | One or more whitespace chars     | Splitting `/proc/stat` lines    |
| `":"`      | A literal colon                  | Splitting `key : value` lines   |
| `"\\t"`    | A tab character                  | Splitting tab-separated fields  |

##### `trim()`
Remove leading and trailing whitespace:
```java
String value = "   12th Gen Intel(R) Core(TM) i7-12700KF   ";
String clean = value.trim(); // "12th Gen Intel(R) Core(TM) i7-12700KF"
```

##### `substring(int beginIndex)` / `substring(int begin, int end)`
Extract part of a string:
```java
String line = "model name\t: 12th Gen Intel(R) Core(TM) i7-12700KF";
int colonPos = line.indexOf(':');
String value = line.substring(colonPos + 1).trim();
// value = "12th Gen Intel(R) Core(TM) i7-12700KF"
```

##### Practical Pattern: Extracting a Value from a `key : value` Line

This pattern appears **everywhere** in this project:

```java
String line = "model name\t: 12th Gen Intel(R) Core(TM) i7-12700KF";

// Method 1: split on ":"
String[] parts = line.split(":");
String value = parts[1].trim();  // "12th Gen Intel(R) Core(TM) i7-12700KF"

// Method 2: indexOf + substring (safer if value contains ":")
int colonIndex = line.indexOf(':');
String value2 = line.substring(colonIndex + 1).trim();
```

> [!WARNING]
> Method 1 (`split(":")`) will break if the value itself contains a colon. Method 2 (`indexOf`) only splits on the **first** colon and is safer. For `/proc/cpuinfo` either works, but it's good to be aware.

#### C) Converting Strings to Numbers — `Integer.parseInt()` and `Long.parseLong()`

The values in `/proc/stat` are numbers stored as text. You need to convert them:

```java
String text = "185221";
int number = Integer.parseInt(text);     // for smaller numbers
long bigNumber = Long.parseLong(text);   // for larger numbers (safer for cumulative counts)
```

> [!IMPORTANT]
> Use `long` (and `Long.parseLong()`) for the `/proc/stat` values. These are cumulative since boot and can exceed the range of `int` (max ~2.1 billion) on a machine that's been running a long time.

#### D) Sleeping for One Second — `Thread.sleep()`

To measure CPU load, you need to pause between two readings:

```java
try {
    Thread.sleep(1000); // 1000 milliseconds = 1 second
} catch (InterruptedException e) {
    // Thread was interrupted — usually fine to ignore for this project
}
```

`Thread.sleep()` throws a **checked exception** (`InterruptedException`), so you must handle it.

#### E) Arrays vs ArrayLists

You'll need to store data for each core. You have two main choices:

##### Fixed-size arrays (when you know the size)
```java
// If you already know there are 20 cores:
double[] loads = new double[20];
loads[0] = 5.2;
loads[1] = 3.8;
```

##### ArrayList (when the size is unknown / dynamic)
```java
import java.util.ArrayList;

ArrayList<String> coreNames = new ArrayList<>();
coreNames.add("cpu0");
coreNames.add("cpu1");
System.out.println(coreNames.size()); // 2
System.out.println(coreNames.get(0)); // "cpu0"
```

**Key differences:**

| Feature              | Array (`int[]`)           | ArrayList (`ArrayList<Integer>`) |
|----------------------|---------------------------|----------------------------------|
| Size                 | Fixed at creation         | Grows automatically              |
| Syntax to add        | `arr[i] = value;`        | `list.add(value);`               |
| Syntax to read       | `arr[i]`                 | `list.get(i)`                    |
| Can hold primitives? | Yes (`int[]`, `double[]`) | No — uses wrappers (`Integer`, `Double`) |
| Length / size         | `arr.length`             | `list.size()`                    |

> [!TIP]
> For this project, `ArrayList` is the better choice because you don't know the number of cores upfront. You read the file, and for each core you find, you add it to the list.

#### F) Putting It Together: The CPU Load Algorithm

Here's the conceptual flow for measuring CPU load:

```
1. Read /proc/stat → for each cpuN line, parse the 10 numbers, calculate total₁ and idle₁
2. Sleep 1 second
3. Read /proc/stat again → calculate total₂ and idle₂
4. For each core:
     busy_diff = (total₂ - idle₂) - (total₁ - idle₁)
     total_diff = total₂ - total₁
     load = (busy_diff / total_diff) × 100
5. Average = sum of all core loads / number of cores
```

Here's a sketch (not complete code — you should write your own):

```java
// Parsing one line from /proc/stat
// Line: "cpu0 2566 1 934 185221 75 269 1229 0 0 0"
public static long[] parseCpuLine(String line) {
    String[] parts = line.trim().split("\\s+");
    // parts[0] is the label ("cpu0"), parts[1..10] are the numbers
    long[] values = new long[parts.length - 1];
    for (int i = 1; i < parts.length; i++) {
        values[i - 1] = Long.parseLong(parts[i]);
    }
    return values;
}

// Calculating total and idle from the 10 values
public static long getTotal(long[] values) {
    long total = 0;
    for (long v : values) {
        total += v;
    }
    return total;
}

public static long getIdle(long[] values) {
    // idle is index 3, iowait is index 4
    return values[3] + values[4];
}
```

> [!NOTE]
> **Enhanced for-loop** (`for (long v : values)`) — this iterates over every element in the array. It's equivalent to `for (int i = 0; i < values.length; i++) { long v = values[i]; ... }` but cleaner.

---

## Part 2: The Memory Requirement

### 2.1 What the Brief Asks You to Do

Report **total memory**, **used memory**, and the **percentage in use**.

### 2.2 Understanding `/proc/meminfo`

```
MemTotal:       32685880 kB
MemFree:        21040916 kB
MemAvailable:   27002508 kB
Buffers:            1812 kB
Cached:          6780968 kB
...
```

- **MemTotal** — total physical RAM installed (in kB)
- **MemAvailable** — how much memory is available for new programs (in kB). This is the right field to use, not `MemFree`.

> [!NOTE]
> **Why `MemAvailable` and not `MemFree`?** Linux aggressively uses "free" memory for caches and buffers (to speed up disk access). This memory *can* be reclaimed if a program needs it. `MemFree` only counts truly unused RAM, so it looks misleadingly low. `MemAvailable` accounts for reclaimable memory and gives a realistic picture.

**The calculations:**
```
used    = MemTotal - MemAvailable
percent = (used / MemTotal) × 100
```

### 2.3 Java Concepts for Memory

You already know file reading and string parsing from Part 1. Here are the additional concepts:

#### A) Unit Conversion — kB to GiB

The values in `/proc/meminfo` are in **kB** (kilobytes). To display human-readable output like "16 GiB", you need to convert:

```java
long totalKB = 32685880;
double totalGB = totalKB / 1024.0 / 1024.0; // Convert kB → MB → GB
// totalGB ≈ 31.17 GiB
```

> [!NOTE]
> **kB vs KiB, GB vs GiB** — In computing, memory is measured in powers of 2:
> - 1 KiB = 1024 bytes
> - 1 MiB = 1024 KiB = 1,048,576 bytes
> - 1 GiB = 1024 MiB = 1,073,741,824 bytes
>
> `/proc/meminfo` says "kB" but actually means **KiB** (1024 bytes). So dividing by 1024 twice gives you GiB. The brief's example uses "GiB", so use that unit.

**Why `1024.0` and not `1024`?** This is **integer vs floating-point division**:

```java
int a = 5;
int b = 2;
System.out.println(a / b);      // prints 2 (integer division — truncates!)
System.out.println(a / 2.0);    // prints 2.5 (floating-point division)
```

If both operands are integers, Java does **integer division** and throws away the decimal part. Using `1024.0` (a `double`) forces floating-point division.

#### B) Formatting Output — `String.format()` and `printf()`

To control decimal places and alignment:

```java
double percent = 17.35;
double totalGiB = 31.17;
double usedGiB = 5.42;

// String.format() — returns a formatted String
String msg = String.format("Memory: %.1f GiB used of %.1f GiB (%.1f%%)", usedGiB, totalGiB, percent);
// "Memory: 5.4 GiB used of 31.2 GiB (17.4%)"

// printf() — prints directly (same format syntax)
System.out.printf("Memory: %.1f GiB used of %.1f GiB (%.1f%%)%n", usedGiB, totalGiB, percent);
```

**Common format specifiers:**

| Specifier | Meaning                     | Example                    |
|-----------|-----------------------------|----------------------------|
| `%d`      | Integer                     | `String.format("%d", 42)` → `"42"` |
| `%f`      | Floating point (default 6dp)| `String.format("%f", 3.14)` → `"3.140000"` |
| `%.1f`    | Float with 1 decimal place  | `String.format("%.1f", 3.14)` → `"3.1"` |
| `%.2f`    | Float with 2 decimal places | `String.format("%.2f", 3.14)` → `"3.14"` |
| `%s`      | String                      | `String.format("%s", "hi")` → `"hi"` |
| `%%`      | Literal `%` sign            | `String.format("50%%")` → `"50%"` |
| `%n`      | Newline (platform-correct)  | Use instead of `\n` in `printf` |

#### C) Defensive Parsing — Handling Missing Lines

Not every machine will have the same lines in these files. Good practice:

```java
long memTotal = -1;  // sentinel value = "not found yet"
long memAvailable = -1;

try (BufferedReader reader = new BufferedReader(new FileReader("/proc/meminfo"))) {
    String line;
    while ((line = reader.readLine()) != null) {
        if (line.startsWith("MemTotal:")) {
            // "MemTotal:       32685880 kB"
            String[] parts = line.split("\\s+");
            memTotal = Long.parseLong(parts[1]);
        } else if (line.startsWith("MemAvailable:")) {
            String[] parts = line.split("\\s+");
            memAvailable = Long.parseLong(parts[1]);
        }
    }
} catch (IOException e) {
    System.out.println("Error reading /proc/meminfo: " + e.getMessage());
}

if (memTotal == -1 || memAvailable == -1) {
    System.out.println("Could not read memory information.");
} else {
    long usedKB = memTotal - memAvailable;
    double percentUsed = (usedKB * 100.0) / memTotal;
    System.out.printf("Total: %.2f GiB | Used: %.2f GiB | Usage: %.1f%%%n",
            memTotal / 1024.0 / 1024.0,
            usedKB / 1024.0 / 1024.0,
            percentUsed);
}
```

---

## Part 3: Cross-Cutting Concepts

These concepts apply to both the processor and memory parts (and the rest of the project too).

### 3.1 Project Structure — Organising Your Classes

A clean structure for this project:

```
src/
├── Main.java            ← Entry point, calls the other classes
├── ProcessorInfo.java   ← Reads /proc/cpuinfo and /proc/stat
├── MemoryInfo.java      ← Reads /proc/meminfo
├── PciInfo.java         ← (later)
└── UsbInfo.java         ← (later)
```

Each class is responsible for one thing (**separation of concerns**). Your `Main.java` ties them together:

```java
public class Main {
    public static void main(String[] args) {
        System.out.println("=== Hardware Inspector ===\n");
        
        ProcessorInfo.report();
        MemoryInfo.report();
        // PciInfo.report();
        // UsbInfo.report();
    }
}
```

### 3.2 Methods — Breaking Your Code Into Pieces

Don't write everything in `main()`. Break the work into methods:

```java
public class ProcessorInfo {

    public static String getModelName() {
        // reads /proc/cpuinfo and returns the model name
    }

    public static int getCoreCount() {
        // reads /proc/cpuinfo and counts processor lines
    }

    public static double[] getCoreLoads() {
        // reads /proc/stat twice, calculates per-core load
    }

    public static void report() {
        String model = getModelName();
        int cores = getCoreCount();
        double[] loads = getCoreLoads();

        System.out.println("Processor: " + model);
        System.out.println("Cores: " + cores);
        // ... print loads
    }
}
```

**Why this matters for the interview:** The marking scheme explicitly asks "can you discuss design choices?" Having clean, separated methods is a design choice you can explain.

### 3.3 Exception Handling — `try/catch`

You've seen this already, but here's a summary of what you need to know:

```java
try {
    // Code that might fail (e.g., file not found)
    BufferedReader reader = new BufferedReader(new FileReader("/proc/cpuinfo"));
} catch (FileNotFoundException e) {
    // Handle the specific error
    System.out.println("File not found: " + e.getMessage());
} catch (IOException e) {
    // Handle broader I/O errors
    System.out.println("I/O error: " + e.getMessage());
}
```

- **Checked exceptions** (like `IOException`) — Java *forces* you to handle them. File I/O always involves checked exceptions.
- **Unchecked exceptions** (like `NumberFormatException`, `ArrayIndexOutOfBoundsException`) — Java doesn't force you to catch them, but they'll crash your program if they happen.

> [!TIP]
> For this project, the brief explicitly says "your program must cope with a missing file and not crash." Always wrap file reading in `try/catch`.

### 3.4 Compiling and Running

Since this needs to run from a terminal:

```bash
# Compile all .java files
javac *.java

# Run the main class
java Main
```

If your files are in a package:
```bash
javac src/*.java
java -cp src Main
```

---

## Part 4: Quick Reference — Checklist for Each Requirement

### ✅ Processor Checklist

| Task | Data Source | Java Skills Needed |
|------|------------|--------------------|
| Get model name | `/proc/cpuinfo` → `model name` line | `BufferedReader`, `startsWith()`, `split()` or `indexOf()` |
| Count cores | `/proc/cpuinfo` → count `processor` lines | `BufferedReader`, counter variable |
| Handle ARM (no model name) | `/proc/cpuinfo` → `CPU implementer` / `CPU part` | `startsWith()`, fallback logic |
| Read CPU times (first) | `/proc/stat` → `cpuN` lines | `split("\\s+")`, `Long.parseLong()` |
| Wait 1 second | — | `Thread.sleep(1000)` |
| Read CPU times (second) | `/proc/stat` → same lines | Same as above |
| Calculate per-core load | Difference of readings | Arithmetic, `double` division |
| Calculate average load | Sum of core loads / count | Loop, accumulator |
| Print results | — | `System.out.printf()`, `String.format()` |

### ✅ Memory Checklist

| Task | Data Source | Java Skills Needed |
|------|------------|--------------------|
| Get total memory | `/proc/meminfo` → `MemTotal` | `BufferedReader`, `split()`, `Long.parseLong()` |
| Get available memory | `/proc/meminfo` → `MemAvailable` | Same |
| Calculate used memory | `total - available` | Arithmetic |
| Calculate percentage | `(used / total) × 100` | `double` division (avoid integer division!) |
| Convert kB to GiB | `÷ 1024 ÷ 1024` | Floating-point arithmetic |
| Print results | — | `printf()` with `%.1f` or `%.2f` |

---

## Part 5: Interview Preparation — Concepts You Should Be Able to Explain

The brief says your marks come from the interview. Here are the kinds of questions you should prepare for:

### Processor Questions
1. **"How did you calculate CPU load from two readings?"**
   - Read `/proc/stat` twice, one second apart. Each line gives cumulative tick counts. Subtract the first from the second to get the *delta*. Load = busy_delta / total_delta × 100.

2. **"Why can't you just read `/proc/stat` once?"**
   - The values are cumulative since boot. A single reading gives the average since the machine started, not the current load. Two readings with a known gap give you the load during that gap.

3. **"What's the difference between logical and physical cores?"**
   - Physical cores are actual hardware execution units. Logical cores include hyperthreads — Intel's technology that lets one physical core appear as two to the OS. The `processor` count in `/proc/cpuinfo` gives logical cores.

4. **"What does each field in `/proc/stat` mean?"**
   - user, nice, system, idle, iowait, irq, softirq, steal, guest, guest_nice. Idle = idle + iowait. Everything else is busy time.

### Memory Questions
1. **"Why use MemAvailable instead of MemFree?"**
   - Linux uses free memory for caches/buffers. MemFree is misleadingly low. MemAvailable accounts for reclaimable cache/buffer memory.

2. **"What's the difference between kB and KiB?"**
   - kB = 1000 bytes (SI), KiB = 1024 bytes (binary). `/proc/meminfo` says "kB" but means KiB (1024).

3. **"What is the memory being used for if MemFree is low but MemAvailable is high?"**
   - Buffers (pending disk writes) and cached file data. The OS keeps recently read files in RAM so reading them again is fast.

### Java Questions
1. **"Why did you use `long` instead of `int`?"**
   - `/proc/stat` values are cumulative ticks since boot. On a server running for months, they exceed `int`'s max value of ~2.1 billion.

2. **"What does `try-with-resources` do?"**
   - Automatically closes the resource (like `BufferedReader`) when the block exits, even if an exception is thrown. Prevents resource leaks.

3. **"What happens if the file doesn't exist?"**
   - A `FileNotFoundException` is thrown. Our `try/catch` block catches it and prints an error message instead of crashing.

---

> [!TIP]
> **Study strategy**: Before writing any code, open a terminal and run `cat /proc/cpuinfo`, `cat /proc/stat`, and `cat /proc/meminfo` yourself. Understand what the raw data looks like. Then your Java code is just automating what you did by hand.
