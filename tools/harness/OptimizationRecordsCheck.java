import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/** Portable fail-closed validation for project-owned optimization records. */
public final class OptimizationRecordsCheck {
    private static final Pattern ID = Pattern.compile("[a-z][a-z0-9]*(?:[.-][a-z0-9]+)+");
    private static final Set<String> STATUS = new HashSet<String>(
            Arrays.asList("active", "candidate", "rejected", "retired", "unknown"));
    private static final String[] REQUIRED = {"schema", "id", "summary", "subsystem", "status",
        "default.enabled", "behavior.delta", "risks", "rollback", "tracking", "source.symbols", "evidence"};

    private OptimizationRecordsCheck() {}
    public static void main(String[] arguments) {
        if (arguments.length != 0) throw new IllegalArgumentException("usage: java OptimizationRecordsCheck.java");
        try { execute(); }
        catch (Exception error) { System.err.println("optimization records failed: " + error.getMessage()); System.exit(1); }
    }
    private static void execute() throws Exception {
        Path directory = Paths.get("optimizations/catalog");
        require(Files.isDirectory(directory), "missing catalog"); int count = 0;
        try (Stream<Path> paths = Files.list(directory)) {
            for (Path path : (Iterable<Path>) paths.filter(Files::isRegularFile)
                    .filter(file -> file.toString().endsWith(".properties"))::iterator) {
                Properties record = new Properties();
                try (java.io.Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                    record.load(reader);
                }
                for (String key : REQUIRED) require(value(record, key) != null, "missing " + key + " in " + path);
                String id = value(record, "id"), status = value(record, "status");
                require(ID.matcher(id).matches(), "invalid id " + id);
                require(path.getFileName().toString().equals(id + ".properties"), "filename/id mismatch " + id);
                require("worldline.optimization.v1".equals(value(record, "schema")), "invalid schema " + id);
                require(STATUS.contains(status), "invalid status " + id);
                String enabled = value(record, "default.enabled");
                require(enabled.equals("true") || enabled.equals("false"), "invalid default " + id);
                require(status.equals("active") || enabled.equals("false"), "non-active optimization enabled " + id);
                require(value(record, "tracking").equals("symbol"), "external records must use symbol tracking " + id);
                count++;
            }
        }
        require(count > 0, "empty optimization catalog");
        System.out.println("  optimization records: " + count + " candidate records PASS");
    }
    private static String value(Properties record, String key) {
        String value = record.getProperty(key); return value == null || value.trim().isEmpty() ? null : value.trim();
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
