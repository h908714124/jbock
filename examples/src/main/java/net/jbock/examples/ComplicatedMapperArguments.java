package net.jbock.examples;

import net.jbock.Command;
import net.jbock.MapperFor;
import net.jbock.Option;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

@Command
abstract class ComplicatedMapperArguments {

  @Option("number")
  abstract Integer number();

  @Option("numbers")
  abstract List<LazyNumber> numbers();

  static class LazyNumberMapper implements Supplier<Function<String, LazyNumber>> {
    @Override
    public Function<String, LazyNumber> get() {
      return s -> () -> Integer.valueOf(s);
    }
  }

  @MapperFor("numbers")
  static LazyNumber mapLazy(String s) {
    return () -> Integer.valueOf(s);
  }

  @MapperFor("numbers")
  static Integer map(String s) {
    return 1;
  }

  interface LazyNumber extends Supplier<Integer> {
  }
}
