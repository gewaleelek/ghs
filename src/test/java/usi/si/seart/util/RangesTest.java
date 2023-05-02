package usi.si.seart.util;

import com.google.common.collect.Range;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import usi.si.seart.exception.UnsplittableRangeException;

import java.text.Format;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.function.BinaryOperator;

class RangesTest {

    @Test
    void testBuild() {
        Assertions.assertEquals(Range.all(), Ranges.build(null, null));
        Assertions.assertEquals(Range.atLeast(5), Ranges.build(5, null));
        Assertions.assertEquals(Range.atMost(10), Ranges.build(null, 10));
        Assertions.assertEquals(Range.closed(5, 10), Ranges.build(5, 10));
    }

    @Test
    void testBuildException() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Ranges.build(5, 1));
    }

    @Test
    void testSplit() {
        BinaryOperator<Long> average = (a, b) -> (a + b)/2;
        Pair<Range<Long>, Range<Long>> ranges = Ranges.split(Range.closed(2L, 10L), average);
        Assertions.assertEquals(Range.closed(2L, 6L), ranges.getLeft());
        Assertions.assertEquals(Range.closed(6L, 10L), ranges.getRight());
    }

    @Test
    void testSplitException() {
        Range<Long> invalid = Range.closed(2L, 2L);
        BinaryOperator<Long> average = (a, b) -> (a + b)/2;
        Assertions.assertThrows(
                UnsplittableRangeException.class,
                () -> Ranges.split(invalid, average)
        );
    }

    @Test
    void testToString() {
        Assertions.assertEquals("5..10", Ranges.toString(Range.closed(5, 10), NumberFormat.getInstance()));
        Assertions.assertEquals("5..10", Ranges.toString(Range.closed(5L, 10L), NumberFormat.getInstance()));
        Assertions.assertEquals("5..", Ranges.toString(Range.atLeast(5), NumberFormat.getInstance()));
        Assertions.assertEquals("..5", Ranges.toString(Range.atMost(5), NumberFormat.getInstance()));
        Assertions.assertEquals("", Ranges.toString(Range.all(), NumberFormat.getInstance()));

        Calendar calendar = Calendar.getInstance();
        calendar.set(2022, Calendar.JANUARY, 1, 0, 0);
        Date lower = calendar.getTime();
        calendar.set(2022, Calendar.JANUARY, 2, 0, 0);
        Date upper = calendar.getTime();
        Range<Date> dateRange = Range.closed(lower, upper);
        Assertions.assertEquals("2022-01-01..2022-01-02", Ranges.toString(dateRange, new SimpleDateFormat("yyyy-MM-dd")));
        Assertions.assertEquals(
                "2022-01-01T00:00..2022-01-02T00:00",
                Ranges.toString(dateRange, new SimpleDateFormat("yyyy-MM-dd'T'HH:mm"))
        );
    }

    @Test
    void testToStringException() {
        Range<Date> invalid = Range.closed(new Date(), new Date());
        Format format = NumberFormat.getInstance();
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> Ranges.toString(invalid, format)
        );
    }
}