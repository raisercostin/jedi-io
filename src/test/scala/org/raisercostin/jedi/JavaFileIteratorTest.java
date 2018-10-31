package org.raisercostin.jedi;

import static org.junit.Assert.*;

import io.reactivex.Flowable;
import io.reactivex.Single;
import org.junit.Ignore;
import org.junit.Test;
import org.raisercostin.jedi.Locations;

import java.util.List;

public class JavaFileIteratorTest {
    @Test
    public void testNoPruning() {
        //Locations.file("d:\\home\\raiser\\work\\export-all\\revomatico\\all\\design\\").visit().take(3).forEach(System.out::println);
        Flowable<?> res = Locations.current("src/test/resources").traverse(
                Filters.anyFilterNoPruning("glob:**/*.{ZIP,jpg}", false));
        res.forEach(System.out::println);
        assertEquals(3, res.toList().blockingGet().size());
        //Locations.file("z:\\media\\media-ebooks2\\").visit().forEach(System.out::println);
        //Locations.file("z:\\media\\media-ebooks2\\").visit("regex:.*Joyce.*").forEach(System.out::println);
    }

    @Test
    public void testGlobFilters() {
        Flowable<?> res = Locations.current("src/test/resources")
                .traverse(Filters.anyFilter("glob:**/*.{zip,jpg}", "glob:**/{folder,.git,.svn}", false));
        res.forEach(System.out::println);
        assertEquals(2, res.toList().blockingGet().size());
    }

    @Test
    public void testIgnoreFolders() {
        Flowable<?> res = Locations.current("src/test/resources")
                .traverse(Filters.filter(Filters.createGlob("**/*.{zip,jpg}"),Filters.createGitFilter("#test\nfolder",true),false));
        res.forEach(System.out::println);
        assertEquals(2, res.toList().blockingGet().size());
    }

    @Test
    public void testSearchLikeInTotalCommander() {
        Flowable<?> res = Locations.current("")
                .traverse(Filters.createTotalCommanderFilter("*.zip *.jpg|folder"));
        res.forEach(System.out::println);
        assertEquals(33, res.toList().blockingGet().size());
    }

    @Test
    @Ignore
    public void checkBigFile() {
        Flowable<?> res = Locations.file("d:\\home\\raiser\\work\\namek-uniboard\\")
                .traverse(Filters.filter(
                        Filters.createGlob("**/*.{js,jpg}"),Filters.createGitFilter("#test\n.nuxt\nnode_modules",true),true));
        res.forEach(System.out::println);
        assertEquals(15, res.toList().blockingGet().size());
    }

    @Test
    @Ignore
    public void checkSecondBigFile() {
        Flowable<?> res = Locations.file("z:\\media\\media-ebooks2")
                .traverse(Filters.filter(
                        Filters.createGlob("**/*.zip"),Filters.createGitFilter("#test\n.git\nnode_modules",true),true)).cache();
        res.forEach(System.out::println);
        assertEquals(1980, res.toList().blockingGet().size());
    }
}
