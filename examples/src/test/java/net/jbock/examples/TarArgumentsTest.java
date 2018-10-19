package net.jbock.examples;

import net.jbock.examples.fixture.ParserTestFixture;
import org.junit.jupiter.api.Test;

class TarArgumentsTest {

  private ParserTestFixture<TarArguments> f =
      ParserTestFixture.create(TarArguments_Parser.create());

  @Test
  void testExtract() {
    f.assertThat("-x", "-f", "foo.tar").succeeds(
        "extract", true,
        "create", false,
        "verbose", false,
        "compress", false,
        "file", "foo.tar");
    f.assertThat("-v", "-x", "-f", "foo.tar").succeeds(
        "extract", true,
        "create", false,
        "verbose", true,
        "compress", false,
        "file", "foo.tar");
  }

  @Test
  void noGrouping() {
    f.assertThat("-v", "xf", "foo.tar").failsWithLine4("Invalid option: xf");
    f.assertThat("-v", "-xf", "foo.tar").failsWithLine4("Invalid option: -xf");
  }

  @Test
  void testPrint() {
    f.assertPrintsHelp(
        "NAME",
        "  TarArguments",
        "",
        "SYNOPSIS",
        "  TarArguments [<options>] -f <FILE>",
        "",
        "DESCRIPTION",
        "",
        "OPTIONS",
        "  -x",
        "",
        "  -c",
        "",
        "  -v",
        "",
        "  -z",
        "",
        "  -f <FILE>",
        "",
        "  --help",
        "    Print this help page.",
        "    The help flag may only be passed as the first argument.",
        "    Any further arguments will be ignored.",
        "",
        "");
  }
}
