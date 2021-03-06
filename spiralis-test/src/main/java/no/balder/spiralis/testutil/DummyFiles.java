package no.balder.spiralis.testutil;

import no.balder.spiralis.payload.WellKnownFileTypeSuffix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.file.FileVisitResult.CONTINUE;

/**
 * @author steinar
 *         Date: 06.02.2017
 *         Time: 15.46
 */
public class DummyFiles {

    public static final String SAMPLE_TRANSMISSION_ID = "519962243.2.1488017263621.JavaMail.steinar_macsteinar3.local";

    public static final Logger LOGGER = LoggerFactory.getLogger(DummyFiles.class);

    /**
     * Copies the sample files from the resources/ directory into the new
     * temporary test directory.
     *
     * @return
     * @throws IOException
     */
    public static Path createInboundDummyFilesInRootWithOptionalSubdirs(String... subPaths) throws IOException {

        // Creates the root directory
        Path root = Files.createTempDirectory("test");

        Path resultPath = root;
        for (String subPath : subPaths) {
            resultPath = resultPath.resolve(subPath);
            Files.createDirectories(resultPath);
        }

        for (String resourceName : sampleResourceNames()) {

            final InputStream resourceAsStream = DummyFiles.class.getClassLoader().getResourceAsStream(resourceName);
            if (resourceAsStream == null) {
                throw new IllegalStateException("Unable to find resource " +resourceName + " in class path");
            }
            Path file = resultPath.resolve(resourceName);

            Files.copy(resourceAsStream, file);
            LOGGER.debug("Created " + file);

        }
        LOGGER.debug("Root dir returned as " + root);
        return root;
    }


    /**
     * Creates a list of resource names for each Well known suffix
     * @return
     */
    public static List<String> sampleResourceNames() {

        return Stream.of(WellKnownFileTypeSuffix.values())
                // Skips the UNKNOWN suffix
                .filter(s -> s != WellKnownFileTypeSuffix.UNKNOWN)
                .map(e -> SAMPLE_TRANSMISSION_ID + e.getSuffix())
                .collect(Collectors.toList());
    }

    /**
     * List all payload files in supplied directory, no traversal of subdirectories.
     *
     * @param rootPath
     * @return
     * @throws IOException
     */
    public static List<Path> locatePayloadFilesIn(Path rootPath) throws IOException {
        return locateFiles(rootPath, WellKnownFileTypeSuffix.PAYLOAD);
    }

    public static List<Path> locateJsonMetaData(Path rootPath) throws IOException {
        return locateFiles(rootPath, WellKnownFileTypeSuffix.META_JSON);
    }


    public static List<Path> locateFiles(Path rootPath, WellKnownFileTypeSuffix... suffixes) {
        final List<Path> paths = new ArrayList<>();

        final String fileGlob = createFileGlob(suffixes);
        final PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher(fileGlob);
        try {
            Files.walkFileTree(rootPath, new SimpleFileVisitor<Path>(){
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (pathMatcher.matches(file)) {
                        paths.add(file);
                    }
                    return CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new IllegalStateException("Unable to walk file tree from " + rootPath, e);
        }

        return paths;
    }

    public static String createFileGlob(WellKnownFileTypeSuffix[] suffixes) {
        final StringJoiner stringJoiner = new StringJoiner(",", "glob:**{", "}");
        for (int i = 0; i < suffixes.length; i++) {
            stringJoiner.add(suffixes[i].getSuffix());
        }
        return stringJoiner.toString();
    }

    public static URL samplePayloadUrl() {
        return DummyFiles.class.getClassLoader().getResource(SAMPLE_TRANSMISSION_ID + WellKnownFileTypeSuffix.PAYLOAD.getSuffix());
    }

    public static Void removeAll(Path root) throws IOException {

        Files.walkFileTree(root, new SimpleFileVisitor<Path>(){

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return CONTINUE;
            }
        });

        return null;
    }

    
}
