package org.raisercostin.jedi;

import static org.junit.Assert.*;

import io.reactivex.Flowable;
import org.junit.Test;
import org.raisercostin.jedi.Locations;

public class JavaFileIteratorTest {
    @Test
    public void test() {
        //Locations.file("d:\\home\\raiser\\work\\export-all\\revomatico\\all\\design\\").visit().take(3).forEach(System.out::println);
        Flowable<?> res = Locations.current("src/test/resources").visit("glob:**/*.{ZIP,jpg}", false);
        res.forEach(System.out::println);
        assertEquals(3, res.toList().blockingGet().size());
        //Locations.file("z:\\media\\media-ebooks2\\").visit().forEach(System.out::println);
        //Locations.file("z:\\media\\media-ebooks2\\").visit("regex:.*Joyce.*").forEach(System.out::println);
    }

    @Test
    public void test2() {
        Flowable<?> res = Locations.current("src/test/resources")
                .visitFull("glob:**/*.{zip,jpg}", "glob:**/{folder,.git,.svn}", false);
        res.forEach(System.out::println);
        assertEquals(2, res.toList().blockingGet().size());
    }
}
