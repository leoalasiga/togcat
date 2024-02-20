package com.als.togcat;

import com.als.togcat.classloader.Resource;
import com.als.togcat.classloader.WebAppClassLoader;
import com.als.togcat.connector.HttpConnector;
import com.als.togcat.utils.ClassPathUtils;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.annotation.WebServlet;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.spi.FileSystemProvider;
import java.security.cert.PKIXRevocationChecker;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.jar.JarFile;

/**
 * TODO
 *
 * @author 刘嘉杰
 * @version 1.0.0
 * @date 2024/1/29 下午4:09
 */
public class Start {
    static Logger logger = LoggerFactory.getLogger(Start.class);

    public static void main(String[] args) throws Exception {
        String warFile = null;
        String customConfigPath = null;
        Options options = new Options();
        options.addOption(Option.builder("w").longOpt("war").argName("file").hasArg().desc("specify war file.").required().build());
        options.addOption(Option.builder("c").longOpt("config").argName("file").hasArg().desc("specify external configuration file.").build());
        try {
            DefaultParser parser = new DefaultParser();
            CommandLine cmd = parser.parse(options, args);
            warFile = cmd.getOptionValue("war");
            customConfigPath = cmd.getOptionValue("config");
        } catch (ParseException e) {
            System.err.println(e.getMessage());
            HelpFormatter help = new HelpFormatter();
            String jarname = pathOf(Start.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getFileName().toString();
            help.printHelp("java -jar " + jarname + " [options]", options);
            System.exit(1);
            return;
        }
        new Start().start(warFile, customConfigPath);
    }

    public static Path pathOf(URI uri) {

        String scheme = uri.getScheme();
        if (scheme == null) {
            throw new IllegalArgumentException("Missing scheme");
        }

        // check for default provider to avoid loading of installed providers
        if (scheme.equalsIgnoreCase("file")) {
            return FileSystems.getDefault().provider().getPath(uri);
        }

        // try to find provider
        for (FileSystemProvider provider : FileSystemProvider.installedProviders()) {
            if (provider.getScheme().equalsIgnoreCase(scheme)) {
                return provider.getPath(uri);
            }
        }

        throw new FileSystemNotFoundException("Provider \"" + scheme + "\" not installed");
    }

    public static Path pathOf(String first, String... more) {
        return FileSystems.getDefault().getPath(first, more);
    }

    public void start(String warFile, String customConfigPath) throws IOException {
        Path warPath = parseWarFile(warFile);

        // extract war if necessary:
        Path[] ps = extractWarIfNecessary(warPath);

        String webRoot = ps[0].getParent().getParent().toString();
        logger.info("set web root: {}", webRoot);

        // load configs:
        String defaultConfigYaml = ClassPathUtils.readString("/server.yml");
        String customConfigYaml = null;
        if (customConfigPath != null) {
            logger.info("load external config {}...", customConfigPath);
            try {
//                customConfigYaml = Files.readString(Paths.get(customConfigPath), StandardCharsets.UTF_8);
                customConfigYaml = readString(Paths.get(customConfigPath), StandardCharsets.UTF_8);

            } catch (IOException e) {
                logger.error("Could not read config: " + customConfigPath, e);
                System.exit(1);
                return;
            }
        }
        Config config;
        Config customConfig;
        try {
            config = loadConfig(defaultConfigYaml);
        } catch (JacksonException e) {
            logger.error("Parse default config failed.", e);
            throw new RuntimeException(e);
        }
        if (customConfigYaml != null) {
            try {
                customConfig = loadConfig(customConfigYaml);
            } catch (JacksonException e) {
                logger.error("Parse custom config failed: " + customConfigPath, e);
                throw new RuntimeException(e);
            }
            // copy custom-config to default-config:
            try {
                merge(config, customConfig);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }

        // set classloader:
        WebAppClassLoader classLoader = new WebAppClassLoader(ps[0], ps[1]);
        // scan class:
        Set<Class<?>> classSet = new HashSet<>();
        Consumer<Resource> handler = (r) -> {
            if (r.name().endsWith(".class")) {
                String className = r.name().substring(0, r.name().length() - 6).replace('/', '.');
                if (className.endsWith("module-info") || className.endsWith("package-info")) {
                    return;
                }
                Class<?> clazz;
                try {
                    clazz = classLoader.loadClass(className);
                } catch (ClassNotFoundException e) {
                    logger.warn("load class '{}' failed: {}: {}", className, e.getClass().getSimpleName(), e.getMessage());
                    return;
                } catch (NoClassDefFoundError err) {
                    logger.error("load class '{}' failed: {}: {}", className, err.getClass().getSimpleName(), err.getMessage());
                    return;
                }
                if (clazz.isAnnotationPresent(WebServlet.class)) {
                    logger.info("Found @WebServlet: {}", clazz.getName());
                    classSet.add(clazz);
                }
                if (clazz.isAnnotationPresent(WebFilter.class)) {
                    logger.info("Found @WebFilter: {}", clazz.getName());
                    classSet.add(clazz);
                }
                if (clazz.isAnnotationPresent(WebListener.class)) {
                    logger.info("Found @WebListener: {}", clazz.getName());
                    classSet.add(clazz);
                }
            }
        };
        classLoader.scanClassPath(handler);
        classLoader.scanJar(handler);
        List<Class<?>> autoScannedClasses = new ArrayList<>(classSet);

        // executor:
        if (config.server.enableVirtualThread) {
            logger.info("Virtual thread is enabled.");
        }
//        ExecutorService executor = config.server.enableVirtualThread ? Executors.newVirtualThreadPerTaskExecutor()
//                : new ThreadPoolExecutor(0, config.server.threadPoolSize, 0L, TimeUnit.MILLISECONDS,
//                new LinkedBlockingQueue<>());
        ExecutorService executor = new ThreadPoolExecutor(0, config.server.threadPoolSize, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>());

        try (HttpConnector connector = new HttpConnector(config, webRoot, executor, classLoader, autoScannedClasses)) {
            for (; ; ) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        logger.info("jerrymouse http server was shutdown.");
    }

    public static String readString(Path path, Charset cs) throws IOException {
        Objects.requireNonNull(path);
        Objects.requireNonNull(cs);

        byte[] ba = Files.readAllBytes(path);
        return new String(ba, cs);
    }


    // return classes and lib path:
    Path[] extractWarIfNecessary(Path warPath) throws IOException {
        if (Files.isDirectory(warPath)) {
            logger.info("war is directy: {}", warPath);
            Path classesPath = warPath.resolve("WEB-INF/classes");
            Path libPath = warPath.resolve("WEB-INF/lib");
            Files.createDirectories(classesPath);
            Files.createDirectories(libPath);
            return new Path[]{classesPath, libPath};
        }
        Path extractPath = createExtractTo();
        logger.info("extract '{}' to '{}'", warPath, extractPath);
        JarFile war = new JarFile(warPath.toFile());
        war.stream().sorted((e1, e2) -> e1.getName().compareTo(e2.getName())).forEach(entry -> {
            if (!entry.isDirectory()) {
                Path file = extractPath.resolve(entry.getName());
                Path dir = file.getParent();
                if (!Files.isDirectory(dir)) {
                    try {
                        Files.createDirectories(dir);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
                try (InputStream in = war.getInputStream(entry)) {
                    Files.copy(in, file);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        });
        // check WEB-INF/classes and WEB-INF/lib:
        Path classesPath = extractPath.resolve("WEB-INF/classes");
        Path libPath = extractPath.resolve("WEB-INF/lib");
        Files.createDirectories(classesPath);
        Files.createDirectories(libPath);
        return new Path[]{classesPath, libPath};
    }

    Path parseWarFile(String warFile) {
//        Path warPath = Path.of(warFile).toAbsolutePath().normalize();
        Path warPath = pathOf(warFile).toAbsolutePath().normalize();
        if (!Files.isRegularFile(warPath) && !Files.isDirectory(warPath)) {
            System.err.printf("war file '%s' was not found.\n", warFile);
            System.exit(1);
        }
        return warPath;
    }

    Path createExtractTo() throws IOException {
        Path tmp = Files.createTempDirectory("_jm_");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                deleteDir(tmp);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }));
        return tmp;
    }

    void deleteDir(Path p) throws IOException {
        Files.list(p).forEach(c -> {
            try {
                if (Files.isDirectory(c)) {
                    deleteDir(c);
                } else {
                    Files.delete(c);
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        Files.delete(p);
    }

    Config loadConfig(String config) throws JacksonException {
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory()).setPropertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return objectMapper.readValue(config, Config.class);
    }

    static void merge(Object source, Object override) throws ReflectiveOperationException {
        for (Field field : source.getClass().getFields()) {
            Object overrideFieldValue = field.get(override);
            if (overrideFieldValue != null) {
                Class<?> type = field.getType();
                if (type == String.class || type.isPrimitive() || Number.class.isAssignableFrom(type)) {
                    // source.xyz = override.xyz:
                    field.set(source, overrideFieldValue);
                } else if (Map.class.isAssignableFrom(type)) {
                    // source.map.putAll(override.map):
                    @SuppressWarnings("unchecked")
                    Map<String, String> sourceMap = (Map<String, String>) field.get(source);
                    @SuppressWarnings("unchecked")
                    Map<String, String> overrideMap = (Map<String, String>) overrideFieldValue;
                    sourceMap.putAll(overrideMap);
                } else {
                    // merge(source.xyz, override.xyz):
                    merge(field.get(source), overrideFieldValue);
                }
            }
        }
    }

    //version 1////////////////////////////////////////////////////////////////
//    public static void main(String[] args) throws Exception {
//        try (HttpConnector connector = new HttpConnector()) {
//            for (;;) {
//                try {
//                    Thread.sleep(1000);
//                } catch (InterruptedException e) {
//                    break;
//                }
//            }
//        } catch (Exception e) {
//            logger.error(e.getMessage(), e);
//        }
//        logger.info("togcat http server was shutdown.");
//    }

}
