import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/** Compiles and runs the external BetaEnergistics Worldline specs. */
public final class Run {
    private Run() {}
    public static void main(String[] arguments) throws Exception {
        if (arguments.length != 0) throw new IllegalArgumentException("usage: java tools/testkit/Run.java");
        Path root = Paths.get("").toAbsolutePath().normalize(), home = home();
        Path api = home.resolve("worldline-test-api-0.1.0.jar");
        Path runner = home.resolve("worldline-test-runner-0.1.0.jar");
        require(Files.isRegularFile(api) && Files.isRegularFile(runner), "incomplete TestKit distribution");
        run(root, java(), "tools/harness/Verify.java");
        Path product = root.resolve(".betaenergistics/build/product");
        Path output = root.resolve(".betaenergistics/worldline-test/classes");
        Files.createDirectories(output);
        List<String> compile = new ArrayList<String>();
        compile.add(javac()); compile.add("--release"); compile.add("8");
        compile.add("-Xlint:all,-options"); compile.add("-Werror"); compile.add("-classpath");
        compile.add(api + File.pathSeparator + product); compile.add("-d"); compile.add(output.toString());
        for (Path source : sources(root.resolve("worldline-tests/src/test/java"))) compile.add(source.toString());
        run(root, compile);
        run(root, java(), "-jar", runner.toString(), "test", "run", output.toString(),
                "--classpath=" + product, "--no-runtime", "--reporter=default,agent",
                "--artifacts=" + root.resolve(".betaenergistics/worldline-test/results"));
    }
    private static Path home() {
        String value = System.getenv("WORLDLINE_TESTKIT_HOME");
        require(value != null && !value.trim().isEmpty(), "WORLDLINE_TESTKIT_HOME is required");
        return Paths.get(value).toAbsolutePath().normalize();
    }
    private static List<Path> sources(Path root) throws Exception {
        List<Path> values = new ArrayList<Path>();
        try (Stream<Path> stream = Files.walk(root)) {
            stream.filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".java"))
                    .sorted(Comparator.naturalOrder()).limit(101).forEach(values::add);
        }
        require(!values.isEmpty() && values.size() <= 100, "invalid spec source count"); return values;
    }
    private static String java() { return executable("java"); }
    private static String javac() { return executable("javac"); }
    private static String executable(String name) {
        Path path = Paths.get(System.getProperty("java.home"), "bin",
                name + (File.separatorChar == '\\' ? ".exe" : ""));
        require(Files.isRegularFile(path), "missing JDK executable " + path); return path.toString();
    }
    private static void run(Path root, String... command) throws Exception {
        run(root, java.util.Arrays.asList(command));
    }
    private static void run(Path root, List<String> command) throws Exception {
        Process process = new ProcessBuilder(command).directory(root.toFile()).inheritIO().start();
        if (process.waitFor() != 0) throw new IllegalStateException("command failed: " + command.get(0));
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
