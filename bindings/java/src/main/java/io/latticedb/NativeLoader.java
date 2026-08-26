package io.latticedb;

/**
 * Loads the native LatticeDB JNI library.
 *
 * Resolution order:
 * <ol>
 *   <li>{@code latticedb.native.dir} system property (directory containing
 *       {@code liblattice_jni} and {@code liblattice})</li>
 *   <li>{@code LATTICE_NATIVE_DIR} environment variable</li>
 *   <li>{@code java.library.path} via {@link System#loadLibrary(String)}</li>
 * </ol>
 */
final class NativeLoader {
    private static volatile boolean loaded = false;

    private NativeLoader() {
    }

    static synchronized void load() {
        if (loaded) {
            return;
        }
        String dir = System.getProperty("latticedb.native.dir");
        if (dir == null || dir.isEmpty()) {
            dir = System.getenv("LATTICE_NATIVE_DIR");
        }
        String os = System.getProperty("os.name", "").toLowerCase();
        String ext = os.contains("win") ? "dll" : os.contains("mac") || os.contains("darwin") ? "dylib" : "so";
        String prefix = os.contains("win") ? "" : "lib";
        if (dir != null && !dir.isEmpty()) {
            java.nio.file.Path abs = java.nio.file.Path.of(dir).toAbsolutePath().normalize();
            String sep = abs.getFileSystem().getSeparator();
            System.load(abs + sep + prefix + "lattice_jni." + ext);
            loaded = true;
            return;
        }
        System.loadLibrary("lattice_jni");
        loaded = true;
    }
}
