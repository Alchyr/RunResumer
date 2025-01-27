package runresumer;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * This utility compresses a list of files to standard ZIP format file.
 * It is able to compress all sub files and sub directories, recursively.
 * @author www.codejava.net
 *
 */
public class ZipUtility {
    /**
     * A constants for buffer size used to read/write data
     */
    private static final int BUFFER_SIZE = 4096;
    /**
     * Compresses a list of files to a destination zip file
     * @param destZipFile The path of the destination zip file
     * @param paths A collection of files and directories
     */
    public void zip(Path destZipFile, Collection<Path> paths) throws IOException {
        ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(destZipFile));
        for (Path path : paths) {
            if (Files.isDirectory(path)) {
                zipDirectory(path, path.getFileName().toString(), zos);
            } else {
                zipFile(path, zos);
            }
        }
        zos.flush();
        zos.close();
    }
    /**
     * Compresses files represented in an array of paths
     * @param destZipFile The path of the destination zip file
     * @param paths a String array containing file paths
     */
    public void zip(Path destZipFile, Path... paths) throws IOException {
        List<Path> pathsList = new ArrayList<>();
        Collections.addAll(pathsList, paths);

        zip(destZipFile, pathsList);
    }
    /**
     * Compresses files represented in an array of paths
     * @param destZipFile The path of the destination zip file
     * @param files a String array containing file paths
     */
    public void zip(Path destZipFile, String... files) throws IOException {
        List<Path> paths = new ArrayList<>();
        for (String file : files) {
            paths.add(new File(file).toPath());
        }
        zip(destZipFile, paths);
    }

    /**
     * Adds a directory to the current zip output stream
     * @param folder the directory to be  added
     * @param parentFolder the path of parent directory
     * @param zos the current zip output stream
     */
    private void zipDirectory(Path folder, String parentFolder, ZipOutputStream zos) throws IOException {
        if (!Files.isDirectory(folder)) {
            throw new IllegalArgumentException("Path received was not a directory");
        }

        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(folder)) {
            for (Path path : directoryStream) {
                if (Files.isDirectory(path)) {
                    zipDirectory(path, parentFolder + "/" + path.getFileName(), zos);
                    continue;
                }
                zos.putNextEntry(new ZipEntry(parentFolder + "/" + path.getFileName()));
                BufferedInputStream bis = new BufferedInputStream(
                        Files.newInputStream(path));
                byte[] bytesIn = new byte[BUFFER_SIZE];
                int read = 0;
                while ((read = bis.read(bytesIn)) != -1) {
                    zos.write(bytesIn, 0, read);
                }
                zos.closeEntry();
            }
        }
    }

    /**
     * Adds a file to the current zip output stream
     * @param path the file to be added
     * @param zos the current zip output stream
     */
    private void zipFile(Path path, ZipOutputStream zos) throws IOException {
        zos.putNextEntry(new ZipEntry(path.getFileName().toString()));
        BufferedInputStream bis = new BufferedInputStream(Files.newInputStream(path));
        byte[] bytesIn = new byte[BUFFER_SIZE];
        int read = 0;
        while ((read = bis.read(bytesIn)) != -1) {
            zos.write(bytesIn, 0, read);
        }
        zos.closeEntry();
    }
}
