package org.raisercostin.jedi;

import static org.apache.commons.io.filefilter.FileFilterUtils.and;
import static org.apache.commons.io.filefilter.FileFilterUtils.directoryFileFilter;
import static org.apache.commons.io.filefilter.FileFilterUtils.nameFileFilter;
import static org.apache.commons.io.filefilter.FileFilterUtils.notFileFilter;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.DirectoryStream.Filter;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    public static final String GLOB_ALL = "glob:**/*";
    public static final String GLOB_NONE = "glob:";
    public static interface TraversalFilter{
        PathMatcher matcher();
        PathMatcher pruningMatcher();
        @Deprecated//toremove
        boolean ignoreCase();

        default boolean shouldPrune(Path path) {
            return pruningMatcher().matches(path);
        }

        default boolean matches(Path path){
            return matcher().matches(path);
        }
    }
    /**
     * The traversal has two important filters:
     * - performance/pruning filter - that can cut out entire folders from final result (you don't need to test that cuts only folders since this will affect performance.
     * - final filter - to define what is the external result.
     * Files or folders that are matched by both pruning and filter will not be returned.
     */
    public static interface FileTraversal {
        default <T> Flowable<T> traverse(Path start, TraversalFilter filter, Function<Path, T> f) {
            return traverse(start, filter).map(x -> f.apply(x));
        }
        Flowable<Path> traverse(Path start, TraversalFilter filter);
    }



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

    @Deprecated//filter is too late and performance is impacted
    public static interface SimpleFileTraversal extends FileTraversal {
        abstract Flowable<Path> traverse(Path start, boolean ignoreCase);

        @Deprecated
        /**simple but not efficient implementation.*/
        default Flowable<Path> traverse(Path start, TraversalFilter filter) {
            return traverse(start, filter.ignoreCase()).filter(path -> filter.matcher().matches(path));
        }
    }

    /**
     * This is not too performant if filter is needed since it iterates over all files. Use Files.walkFileTree to filter as you go in folders.
     */
    @Deprecated
    public static class WalkTraversal implements SimpleFileTraversal {
        public Flowable<Path> traverse(Path start, boolean ignoreCase) {
            try {
                return toFlowable(Files.walk(start));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    //
//    public static class WalkFileTreeTraversal implements SimpleFileTraversal {
//        public Flowable<Path> traverse(Path start, boolean ignoreCase) {
////            return Flowable.generate(
////                    () -> stream.spliterator(),
////                    (reader, emitter) -> {
////                        boolean read = reader.tryAdvance(x -> {
////                            System.out.println("try " + x);
////                            emitter.onNext(x);
////                        });
////                        if (!read) {
////                            System.out.println("complete");
////                            emitter.onComplete();
////                        }
////                    }
////            );
//            try {
//                Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
//                });
//                //return stream;
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//        }
//    }
    @Deprecated
    private static <T> Flowable<T> toFlowableViaList(Stream<T> stream) {
        return Flowable.fromIterable(stream.collect(Collectors.toList()));
    }

    //same performance as toFlowableWithGenerator
    @Deprecated
    private static <T> Flowable<T> toFlowable(Stream<T> stream) {
        return Flowable.fromIterable(() -> stream.iterator());
    }

    //same performance as toFlowable
    @Deprecated
    private static <T> Flowable<T> toFlowableWithGenerator(Stream<T> stream) {
        return Flowable.generate(
                () -> stream.spliterator(),
                (reader, emitter) -> {
                    boolean read = reader.tryAdvance(x -> {
                        System.out.println("try " + x);
                        emitter.onNext(x);
                    });
                    if (!read) {
                        System.out.println("complete");
                        emitter.onComplete();
                    }
                }
        );
    }

    /**
     * Sample gitIgnores:
     * <pre>
     *     # for now only folders
     *     target
     *     .git
     *     .mvn
     * </pre>
     */
    protected static OrFileFilter createGitFilter(String gitIgnores, boolean ignoreCase) {
        Stream<IOFileFilter> or = Streams.stream(Splitter.on("\n").omitEmptyStrings().trimResults().split(gitIgnores))
                .filter(line -> !line.startsWith("#"))
                .map(folder -> nameFileFilter(folder, ignoreCase ? IOCase.INSENSITIVE : IOCase.SENSITIVE));
        List<IOFileFilter> all = or.collect(Collectors.toList());
        return new OrFileFilter(all);
    }


    public static class CommonsIoTraversal implements FileTraversal {
        private final IOFileFilter gitFilterCaseSensible;
        private final IOFileFilter gitFilterCaseInSensible;

        public CommonsIoTraversal(String gitIgnores) {
            gitFilterCaseSensible = notFileFilter(and(directoryFileFilter(), createGitFilter(gitIgnores, true)));
            gitFilterCaseInSensible = notFileFilter(and(directoryFileFilter(), createGitFilter(gitIgnores, false)));
        }

        public Flowable<Path> traverse(Path start, TraversalFilter filter) {
            IOFileFilter fileFilter = TrueFileFilter.INSTANCE;
            IOFileFilter dirFilter = getFilter(filter.ignoreCase());
            Iterable<File> a = () -> FileUtils.iterateFilesAndDirs(start.toFile(), fileFilter,
                    dirFilter);
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


    /**@deprecated use {@link GuavaAndDirectoryStreamTraversal}*/
    @Deprecated
    public static class GuavaTraversal implements SimpleFileTraversal {
        public Flowable<Path> traverse(Path start, boolean ignoreCase) {
            Iterable<File> iterable = com.google.common.io.Files.fileTraverser().depthFirstPreOrder(start.toFile());
            return Flowable.fromIterable(iterable).map(x -> x.toPath());
        }
    }

    public static class FileFilterPathMatcherAdapter implements PathMatcher {
        private final FileFilter filter;

        public FileFilterPathMatcherAdapter(FileFilter filter) {
            this.filter = filter;
        }

        @Override
        public boolean matches(Path path) {
            return filter.accept(path.toFile());
        }
    }

    /**
     * Reimplemented com.google.common.io.Files.fileTraverser using Files.newDirectoryStream.
     */
    public static class GuavaAndDirectoryStreamTraversal implements FileTraversal {
        public Flowable<Path> traverse(Path start, TraversalFilter filter) {
            try {
                PathMatcher all = new PathMatcher() {
                    @Override
                    public boolean matches(Path path) {
                        if(filter.shouldPrune(path))
                            return false;
                        if(Files.isDirectory(path))
                            return true;
                        return filter.matches(path);
                    }
                };
                Iterable<Path> iterable = fileTraverser(createFilter(all)).depthFirstPreOrder(start);
                //.breadthFirst(start);
                return Flowable.fromIterable(iterable).filter(path -> filter.matches(path));//.map(x->{System.out.println("found "+x);return x;});
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private static Traverser<Path> fileTraverser(Filter<Path> filter) {
            return Traverser.forTree(file -> fileTreeChildren(file, filter));
        }

        private static LinkOption[] options = {LinkOption.NOFOLLOW_LINKS};

        private static Iterable<Path> fileTreeChildren(Path file, Filter<Path> filter) {
            // check isDirectory() just because it may be faster than listFiles() on a
            // non-directory
            if (Files.isDirectory(file, options)) {
                try {
                    DirectoryStream<Path> files = Files.newDirectoryStream(file, filter);
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

        // copied from newDirectoryStream(file,regex)
        public static Filter<Path> createFilter(PathMatcher all) throws IOException {
            return new DirectoryStream.Filter<Path>() {
                @Override
                public boolean accept(Path path) {
                    return all.matches(path);
                }
            };
        }
    }
}
