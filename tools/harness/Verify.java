import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Portable zero-dependency host gate. Run with: java tools/harness/Verify.java */
public final class Verify {
    private final Path root = Paths.get("").toAbsolutePath().normalize();
    private final Path build = root.resolve(".betaenergistics/build");
    private final Properties config = new Properties();
    private final Properties legacy = new Properties();
    private final boolean integration;

    private Verify(boolean integration) { this.integration = integration; }

    public static void main(String[] arguments) {
        boolean integration = Arrays.equals(arguments, new String[] {"--integration"});
        if (arguments.length > 0 && !integration) {
            System.err.println("usage: java tools/harness/Verify.java [--integration]");
            System.exit(2);
        }
        try { new Verify(integration).execute(); }
        catch (Exception error) {
            System.err.println("verify failed: " + error.getMessage());
            System.exit(1);
        }
    }

    private void execute() throws Exception {
        System.out.println("BetaEnergistics repository verification");
        load(config, "harness.properties");
        load(legacy, "tools/harness/legacy-lines.properties");
        verifyRelease();
        enforceProduct();
        enforceSimple("harness", root.resolve("tools/harness"), integer("harness.max.file"));
        enforceSimple("test", root.resolve("tests"), integer("test.max.file"));
        enforceSimple("integration", root.resolve(value("integration.sources")), integer("product.max.file"));
        enforceSimple("integration test", root.resolve(value("integration.tests")), integer("test.max.file"));
        verifyPackages();
        verifyPublicTree();
        run(command("java", "tools/harness/OptimizationRecordsCheck.java"));
        recreateBuild();
        compileAndTest();
        if (integration) run(command("java", "tools/harness/BetaVaultIntegrationCheck.java"));
        System.out.println("verify passed");
    }

    private void verifyRelease() throws IOException {
        Properties release = new Properties();
        load(release, "release/betaenergistics.properties");
        match(release, "id", "betaenergistics");
        match(release, "name", "BetaEnergistics");
        match(release, "version", value("release.version"));
        match(release, "status", value("release.status"));
        match(release, "canonical.command", "java tools/harness/Verify.java");
        for (String required : Arrays.asList("README.md", "AGENTS.md", "dependency-lock.properties",
                "docs/STANDALONE_COMPARISON.md", "docs/BETAVAULT_INTEGRATION.md",
                "docs/HYPERPERFORMANCE.md", "docs/WORLDLINE_EXTENSION.md")) {
            if (!Files.isRegularFile(root.resolve(required))) fail("missing " + required);
        }
        System.out.println("  release: " + release.getProperty("version") + " " + release.getProperty("status"));
    }

    private void enforceProduct() throws IOException {
        int standard = integer("product.max.file");
        int files = 0;
        long total = 0;
        for (Path file : javaFiles(root.resolve("src"))) {
            String relative = relative(file);
            int lines = codeLines(file);
            String allowance = legacy.getProperty(relative);
            int ceiling = allowance == null ? standard : Integer.parseInt(allowance);
            if (lines > ceiling) fail("product file ceiling exceeded: " + relative + " has " + lines + "/" + ceiling);
            if (allowance != null && lines != ceiling) {
                fail("stale legacy allowance: " + relative + " has " + lines + ", recorded " + ceiling);
            }
            files++;
            total += lines;
        }
        for (String relative : legacy.stringPropertyNames()) {
            if (!Files.isRegularFile(root.resolve(relative))) fail("legacy allowance names missing file: " + relative);
            if (Integer.parseInt(legacy.getProperty(relative)) <= standard) fail("unnecessary legacy allowance: " + relative);
        }
        System.out.println("  product sources: " + files + " files, " + total
                + " code lines, new-file max " + standard + ", legacy ratchets " + legacy.size());
    }

    private void enforceSimple(String label, Path directory, int ceiling) throws IOException {
        int files = 0;
        long total = 0;
        for (Path file : javaFiles(directory)) {
            int lines = codeLines(file);
            if (lines > ceiling) fail(label + " file ceiling exceeded: " + relative(file) + " has " + lines + "/" + ceiling);
            files++;
            total += lines;
        }
        System.out.println("  " + label + " sources: " + files + " files, " + total + " code lines, max " + ceiling);
    }

    private void verifyPackages() throws IOException {
        for (Path file : javaFiles(root.resolve("src"))) {
            String relative = relative(file);
            int slash = relative.lastIndexOf('/');
            String expected = relative.substring("src/".length(), slash).replace('/', '.');
            String text = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
            if (!text.contains("package " + expected + ";")) fail("package/path mismatch: " + relative);
        }
        System.out.println("  package layout: organized source tree PASS");
    }

    private void verifyPublicTree() throws IOException {
        if (Files.exists(root.resolve(".gitmodules"))) fail("standalone repository must not own dependency submodules");
        try (Stream<Path> paths = Files.walk(root)) {
            for (Path path : paths.filter(Files::isRegularFile).collect(Collectors.toList())) {
                String relative = relative(path).toLowerCase();
                if (relative.startsWith(".git/") || relative.startsWith(".betaenergistics/")) continue;
                if (relative.endsWith(".class") || relative.endsWith(".jar") || relative.startsWith("mcp/")) {
                    fail("prohibited generated or binary artifact: " + relative(path));
                }
            }
        }
        System.out.println("  public tree: no submodules, binaries, or generated Minecraft tree");
    }

    private void compileAndTest() throws Exception {
        Path product = build.resolve("product");
        Path tests = build.resolve("tests");
        Files.createDirectories(product);
        Files.createDirectories(tests);
        List<String> compile = command("javac", "--release", value("java.release"), "-Xlint:all,-options", "-Werror",
                "-d", product.toString());
        for (String source : values("compile.sources")) compile.add(root.resolve(source).toString());
        run(compile);
        List<String> testCompile = command("javac", "--release", value("java.release"), "-Xlint:all,-options", "-Werror",
                "-cp", product.toString(), "-d", tests.toString());
        for (Path source : javaFiles(root.resolve("tests/src"))) testCompile.add(source.toString());
        run(testCompile);
        String classpath = tests + System.getProperty("path.separator") + product;
        for (String suite : values("test.suites")) run(command("java", "-cp", classpath, suite));
        System.out.println("  Java " + value("java.release") + " core slice: compile and tests PASS");
    }

    private void recreateBuild() throws IOException {
        if (Files.exists(build)) {
            List<Path> paths;
            try (Stream<Path> walk = Files.walk(build)) {
                paths = walk.sorted(Collections.reverseOrder()).collect(Collectors.toList());
            }
            for (Path path : paths) Files.delete(path);
        }
        Files.createDirectories(build);
    }

    private List<Path> javaFiles(Path directory) throws IOException {
        if (!Files.isDirectory(directory)) return Collections.emptyList();
        try (Stream<Path> paths = Files.walk(directory)) {
            return paths.filter(Files::isRegularFile).filter(path -> path.toString().endsWith(".java"))
                    .sorted().collect(Collectors.toList());
        }
    }

    private int codeLines(Path path) throws IOException {
        boolean block = false;
        int count = 0;
        for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            boolean code = false;
            boolean string = false;
            boolean character = false;
            boolean escaped = false;
            for (int index = 0; index < line.length(); index++) {
                char current = line.charAt(index);
                char next = index + 1 < line.length() ? line.charAt(index + 1) : '\0';
                if (block) {
                    if (current == '*' && next == '/') { block = false; index++; }
                    continue;
                }
                if (!string && !character && current == '/' && next == '*') { block = true; index++; continue; }
                if (!string && !character && current == '/' && next == '/') break;
                if (current == '"' && !character && !escaped) string = !string;
                if (current == '\'' && !string && !escaped) character = !character;
                if (!Character.isWhitespace(current)) code = true;
                escaped = (string || character) && current == '\\' && !escaped;
                if (current != '\\') escaped = false;
            }
            if (code) count++;
        }
        return count;
    }

    private void run(List<String> command) throws Exception {
        Process process = new ProcessBuilder(command).directory(root.toFile()).inheritIO().start();
        int status = process.waitFor();
        if (status != 0) fail(command.get(0) + " failed with exit " + status);
    }

    private void load(Properties target, String relative) throws IOException {
        try (java.io.Reader reader = Files.newBufferedReader(root.resolve(relative), StandardCharsets.UTF_8)) {
            target.load(reader);
        }
    }

    private List<String> values(String key) {
        String raw = value(key);
        if (raw.isEmpty()) return Collections.emptyList();
        return Arrays.stream(raw.split(",")).map(String::trim).filter(value -> !value.isEmpty())
                .collect(Collectors.toList());
    }

    private String value(String key) {
        String result = config.getProperty(key);
        if (result == null) fail("missing harness property " + key);
        return result.trim();
    }

    private int integer(String key) { return Integer.parseInt(value(key)); }

    private void match(Properties properties, String key, String expected) {
        if (!expected.equals(properties.getProperty(key))) fail(key + " is not " + expected);
    }

    private String relative(Path path) { return root.relativize(path.toAbsolutePath().normalize()).toString().replace('\\', '/'); }

    private static List<String> command(String... values) { return new ArrayList<String>(Arrays.asList(values)); }

    private static void fail(String message) { throw new IllegalStateException(message); }
}
