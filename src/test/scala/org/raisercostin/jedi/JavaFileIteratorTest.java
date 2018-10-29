package org.raisercostin.jedi;

import static org.junit.Assert.*;

import io.reactivex.Flowable;
import org.junit.Test;
import org.raisercostin.jedi.Locations;

public class JavaFileIteratorTest {
    @Test
    public void test() {
        Locations.file("d:\\home\\raiser\\work\\export-all\\revomatico\\all\\design\\").visit().take(3).forEach(System.out::println);
        System.out.println("done");
    }
}
