package net.jbock.examples;

import net.jbock.examples.fixture.ParserTestFixture;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;
import java.util.OptionalInt;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomMapperArgumentsTest {

  private ParserTestFixture<CustomMapperArguments> f =
      ParserTestFixture.create(new CustomMapperArguments_Parser());

  @Test
  void success() {
    CustomMapperArguments parsed = f.parse(
        "--date", "1500000000000",
        "--optDate", "1500000000000",
        "--dateList", "1500000000000",
        "--verbosity", "0x10",
        "--notFlag", "true",
        "--integerList", "1,2,3,4",
        "--optionalInts", "1",
        "--optionalInts", "",
        "--optionalInts", "3",
        "--listWrapper", "foo",
        "--optionalInts", "4",
        "--enumSet", "FOO",
        "true", "false", "true",
        "--stringArray", "A",
        "--aRequiredInt", "51");
    assertEquals(1500000000000L, parsed.date().getTime());
    assertEquals(Optional.of(1500000000000L), parsed.optDate().map(Date::getTime));
    assertEquals(1500000000000L, parsed.dateList().get(0).getTime());
    assertEquals(Optional.of(16), parsed.verbosity().map(BigInteger::intValue));
    assertEquals(Arrays.asList(true, false, true), parsed.booleanList());
    assertEquals(51, parsed.aRequiredInt());
    assertEquals(Arrays.asList(1, 2, 3, 4), parsed.integerList().orElseThrow(AssertionFailedError::new));
    assertEquals(Arrays.asList(OptionalInt.of(1), OptionalInt.empty(), OptionalInt.of(3), OptionalInt.of(4)),
        parsed.optionalInts());
    assertEquals(singleton(MyEnum.FOO), parsed.enumSet().orElseThrow(AssertionFailedError::new));
    assertEquals(Optional.of(singletonList("foo")), parsed.listWrapper());
    assertArrayEquals(new String[]{"A"}, parsed.stringArray().orElseThrow(AssertionFailedError::new));
    assertTrue(parsed.notFlag());
  }

  @Test
  void invalidOptions() {
    f.assertThat("--date", "FooBar").failsWithMessage("For input string: \"FooBar\"");
    f.assertThat().failsWithMessage("Missing required: DATE (--date)");
  }
}
