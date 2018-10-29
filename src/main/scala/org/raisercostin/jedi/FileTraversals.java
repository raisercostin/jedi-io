package org.raisercostin.jedi;

import static org.apache.commons.io.filefilter.FileFilterUtils.and;
import static org.apache.commons.io.filefilter.FileFilterUtils.directoryFileFilter;
import static org.apache.commons.io.filefilter.FileFilterUtils.nameFileFilter;
import static org.apache.commons.io.filefilter.FileFilterUtils.notFileFilter;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import io.reactivex.Flowable;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.OrFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;

import com.google.common.base.Splitter;
import com.google.common.collect.Streams;
import com.google.common.graph.Traverser;

public class FileTraversals {
    public static FileTraversal traverseUsingWalk() {
        return new WalkTraversal();
    }

    public static FileTraversal traverseUsingGuava() {
        return new GuavaTraversal();
    }

    public static FileTraversal traverseUsingGuavaAndDirectoryStream() {
        return new GuavaAndDirectoryStreamTraversal();
    }

    public static FileTraversal traverseUsingCommonsIo(String gitIgnores) {
        return new CommonsIoTraversal(gitIgnores);
    }

    public static interface FileTraversal {
        default Flowable<Path> traverse(Path start, boolean ignoreCase) {
            return traverse(start, "**/*", ignoreCase);
        }
        default <T> Flowable<T> traverse(Path start, boolean ignoreCase, Function<Path,T> f) {
            return traverse(start, "**/*", ignoreCase).map(x->{System.out.println("map "+x);return f.apply(x);});
        }

        Flowable<Path> traverse(Path start, String regex, boolean ignoreCase);
    }

    public static interface SimpleFileTraversal extends FileTraversal {
        abstract Flowable<Path> traverse(Path start, boolean ignoreCase);

        default Flowable<Path> traverse(Path start, String regex, boolean ignoreCase) {
            PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + regex);
            return traverse(start, ignoreCase).filter(path -> matcher.matches(path));
        }
    }

    public static class WalkTraversal implements SimpleFileTraversal {
        public Flowable<Path> traverse(Path start, boolean ignoreCase) {
            try {
                return toFlowableViaIterator(Files.walk(start).map(x->{System.out.println("walk "+x);return x;}));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
    private static <T> Flowable<T> toFlowableViaList(Stream<T> stream) {
        return Flowable.fromIterable(stream.collect(Collectors.toList()));
    }
    //same performance as toFlowableViaIterator
    private static <T> Flowable<T> toFlowableViaIterator(Stream<T> stream) {
        return Flowable.fromIterable(()->stream.iterator());
    }
    //same performance as toFlowableViaIterator
    private static <T> Flowable<T> toFlowable(Stream<T> stream) {
        return Flowable.generate(
                () -> stream.spliterator(),
                (reader, emitter) -> {
                    boolean read = reader.tryAdvance(x -> {
                        System.out.println("try "+x);
                        emitter.onNext(x);
                    });
                    if (!read) {
                        System.out.println("complete");
                        emitter.onComplete();
                    }
                }
        );
    }

    public static class CommonsIoTraversal implements FileTraversal {
        private final IOFileFilter gitFilterCaseSensible;
        private final IOFileFilter gitFilterCaseInSensible;

        public CommonsIoTraversal(String gitIgnores) {
            gitFilterCaseSensible = notFileFilter(and(directoryFileFilter(), createFilter(gitIgnores, true)));
            gitFilterCaseInSensible = notFileFilter(and(directoryFileFilter(), createFilter(gitIgnores, false)));
        }

        private OrFileFilter createFilter(String gitIgnores, boolean ignoreCase) {
            Stream<IOFileFilter> or = Streams.stream(Splitter.on("\n").omitEmptyStrings().trimResults().split(gitIgnores))
                    .filter(line -> !line.startsWith("#"))
                    .map(folder -> nameFileFilter(folder, ignoreCase ? IOCase.INSENSITIVE : IOCase.SENSITIVE));
            List<IOFileFilter> all = or.collect(Collectors.toList());
            return new OrFileFilter(all);
        }

        public Flowable<Path> traverse(Path start, String regex, boolean ignoreCase) {
            Iterable<File> a = () -> FileUtils.iterateFilesAndDirs(start.toFile(), TrueFileFilter.INSTANCE,
                    getFilter(ignoreCase));
            // lesAndDirs(start.toFile(), null, null);
            return Flowable.fromIterable(a).map(x -> x.toPath());
        }

        private IOFileFilter getFilter(boolean ignoreCase) {
            if (ignoreCase)
                return gitFilterCaseInSensible;
            else
                return gitFilterCaseSensible;
        }
    }

    // public static class WalkFileTreeTraversal implements FileTraversal {
    // public Stream<Path> traverse(Path start) {
    // try {
    // Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
    // });
    // //return stream;
    // } catch (IOException e) {
    // throw new RuntimeException(e);
    // }
    // }
    // }
    //
    public static class GuavaTraversal implements SimpleFileTraversal {
        public Flowable<Path> traverse(Path start, boolean ignoreCase) {
            Iterable<File> iterable = com.google.common.io.Files.fileTraverser().depthFirstPreOrder(start.toFile());
            return Flowable.fromIterable(iterable).map(x -> x.toPath());
        }
    }

    public static class GuavaAndDirectoryStreamTraversal implements FileTraversal {
        public Flowable<Path> traverse(Path start, String regex, boolean ignoreCase) {
            try {
                Iterable<Path> iterable = fileTraverser(createFilter(start, regex)).depthFirstPreOrder(start);
                return Flowable.fromIterable(iterable);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private static Traverser<Path> fileTraverser(Optional<Filter<Path>> filter) {
            return Traverser.forTree(file -> fileTreeChildren(file, filter));
        }

        private static LinkOption[] options = {LinkOption.NOFOLLOW_LINKS};

        private static Iterable<Path> fileTreeChildren(Path file, Optional<Filter<Path>> filter) {
            // check isDirectory() just because it may be faster than listFiles() on a
            // non-directory
            if (Files.isDirectory(file, options)) {
                try {
                    DirectoryStream<Path> files = null;
                    if (filter.isPresent())
                        files = Files.newDirectoryStream(file, filter.get());
                    else
                        files = Files.newDirectoryStream(file);
                    if (files != null) {
                        return files;
                        // return Collections.unmodifiableList(Arrays.asList(files));
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            return Collections.emptyList();
        }
    }

    // copied from newDirectoryStream(file,regex)
    public static Optional<Filter<Path>> createFilter(Path dir, String glob) throws IOException {
        // avoid creating a matcher if all entries are required.
        if (glob.equals("*"))
            return Optional.empty();
        // create a matcher and return a filter that uses it.
        FileSystem fs = dir.getFileSystem();
        final PathMatcher matcher = fs.getPathMatcher("glob:" + glob);
        DirectoryStream.Filter<Path> filter = new DirectoryStream.Filter<Path>() {
            @Override
            public boolean accept(Path path) {
                return matcher.matches(path);
            }
        };
        return Optional.of(filter);
    }
}
