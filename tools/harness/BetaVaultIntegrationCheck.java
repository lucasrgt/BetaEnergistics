import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Compiles and executes the BetaVault adapter against one pinned source checkout. */
public final class BetaVaultIntegrationCheck {
    private final Path root = Paths.get("").toAbsolutePath().normalize();
    private final Path build = root.resolve(".betaenergistics/build/integration");
    private final Properties harness = new Properties();
    private final Properties lock = new Properties();

    public static void main(String[] arguments) {
        if (arguments.length != 0) {
            System.err.println("usage: java tools/harness/BetaVaultIntegrationCheck.java");
            System.exit(2);
        }
        try { new BetaVaultIntegrationCheck().execute(); }
        catch (Exception error) {
            System.err.println("BetaVault integration failed: " + error.getMessage());
            System.exit(1);
        }
    }

    private void execute() throws Exception {
        load(harness, root.resolve("harness.properties"));
        load(lock, root.resolve("dependency-lock.properties"));
        Path vault = dependencyRoot();
        requireRevision(vault, value(lock, "betavault.revision"));
        List<Path> outputs = compileBetaVault(vault);
        Path adapter = compileDirectory(vault.resolve("adapters/minecraft/src/main/java"),
                build.resolve("betavault-adapter"), outputs);
        outputs.add(adapter);
        Path product = compileIntegration(outputs);
        Path tests = compileTests(outputs, product);
        String classpath = join(append(outputs, product, tests));
        for (String suite : values(harness, "integration.suites")) run(command("java", "-cp", classpath, suite));
        System.out.println("  pinned BetaVault " + value(lock, "betavault.version") + ": integration PASS");
    }

    private Path dependencyRoot() {
        String configured = System.getenv("BETAVAULT_ROOT");
        Path candidate = configured == null || configured.trim().isEmpty()
                ? root.resolve("../betavault") : root.resolve(configured);
        Path result = candidate.toAbsolutePath().normalize();
        if (!Files.isRegularFile(result.resolve("release/betavault.properties"))) {
            fail("BetaVault checkout absent at " + result + "; set BETAVAULT_ROOT");
        }
        return result;
    }

    private void requireRevision(Path vault, String expected) throws Exception {
        String actual = capture(command("git", "-C", vault.toString(), "rev-parse", "HEAD")).trim();
        if (!expected.equals(actual)) fail("BetaVault revision is " + actual + "; expected " + expected);
    }

    private List<Path> compileBetaVault(Path vault) throws Exception {
        List<Path> outputs = new ArrayList<Path>();
        for (String module : Arrays.asList("core", "codec", "journal", "store")) {
            Path output = compileDirectory(vault.resolve("modules/" + module + "/src/main/java"),
                    build.resolve("betavault-" + module), outputs);
            outputs.add(output);
        }
        return outputs;
    }

    private Path compileIntegration(List<Path> dependencies) throws Exception {
        Path output = build.resolve("product");
        List<Path> sources = javaFiles(root.resolve(value(harness, "integration.sources")));
        sources.add(root.resolve("src/betaenergistics/storage/BE_ItemKey.java"));
        compile(sources, output, dependencies);
        return output;
    }

    private Path compileTests(List<Path> dependencies, Path product) throws Exception {
        Path output = build.resolve("tests");
        List<Path> classpath = new ArrayList<Path>(dependencies);
        classpath.add(product);
        compile(javaFiles(root.resolve(value(harness, "integration.tests"))), output, classpath);
        return output;
    }

    private Path compileDirectory(Path sources, Path output, List<Path> dependencies) throws Exception {
        compile(javaFiles(sources), output, dependencies);
        return output;
    }

    private void compile(List<Path> sources, Path output, List<Path> dependencies) throws Exception {
        if (sources.isEmpty()) fail("missing sources for " + output.getFileName());
        Files.createDirectories(output);
        List<String> command = command("javac", "--release", value(harness, "java.release"),
                "-Xlint:all,-options", "-Werror");
        if (!dependencies.isEmpty()) { command.add("-cp"); command.add(join(dependencies)); }
        command.add("-d"); command.add(output.toString());
        for (Path source : sources) command.add(source.toString());
        run(command);
    }

    private List<Path> javaFiles(Path directory) throws IOException {
        if (!Files.isDirectory(directory)) return new ArrayList<Path>();
        try (Stream<Path> paths = Files.walk(directory)) {
            return paths.filter(Files::isRegularFile).filter(path -> path.toString().endsWith(".java"))
                    .sorted(Comparator.naturalOrder()).collect(Collectors.toCollection(ArrayList::new));
        }
    }

    private List<Path> append(List<Path> values, Path... additions) {
        List<Path> result = new ArrayList<Path>(values);
        result.addAll(Arrays.asList(additions));
        return result;
    }

    private String join(List<Path> paths) {
        return paths.stream().map(Path::toString).collect(Collectors.joining(File.pathSeparator));
    }

    private void run(List<String> command) throws Exception {
        Process process = new ProcessBuilder(command).directory(root.toFile()).inheritIO().start();
        if (process.waitFor() != 0) fail(command.get(0) + " failed");
    }

    private String capture(List<String> command) throws Exception {
        Process process = new ProcessBuilder(command).directory(root.toFile()).redirectErrorStream(true).start();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (InputStream input = process.getInputStream()) {
            byte[] buffer = new byte[1024];
            for (int read; (read = input.read(buffer)) >= 0;) bytes.write(buffer, 0, read);
        }
        if (process.waitFor() != 0) fail(command.get(0) + " failed");
        return new String(bytes.toByteArray(), StandardCharsets.UTF_8);
    }

    private void load(Properties properties, Path path) throws IOException {
        try (java.io.Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) { properties.load(reader); }
    }

    private List<String> values(Properties properties, String key) {
        return Arrays.stream(value(properties, key).split(",")).map(String::trim)
                .filter(text -> !text.isEmpty()).collect(Collectors.toList());
    }

    private String value(Properties properties, String key) {
        String result = properties.getProperty(key);
        if (result == null || result.trim().isEmpty()) fail("missing property " + key);
        return result.trim();
    }

    private static List<String> command(String... arguments) {
        return new ArrayList<String>(Arrays.asList(arguments));
    }

    private static void fail(String message) { throw new IllegalStateException(message); }
}
