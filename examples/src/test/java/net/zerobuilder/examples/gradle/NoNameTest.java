package net.zerobuilder.examples.gradle;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class NoNameTest {

  @Test
  public void test() throws Exception {
    NoNameParser.Binder binder = NoNameParser.parse(new String[]{"--message=m", "--file=f", "--file=o",
        "--file=o", "--cmos"});
    NoName noName = binder.bind();
    assertThat(noName.cmos, is(true));
    assertThat(noName.message, is("m"));
    assertThat(noName.file.size(), is(3));
    assertThat(noName.file.get(0), is("f"));
    assertThat(noName.file.get(1), is("o"));
    assertThat(noName.file.get(2), is("o"));
  }
}