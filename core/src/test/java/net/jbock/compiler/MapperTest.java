package net.jbock.compiler;

import org.junit.jupiter.api.Test;

import javax.tools.JavaFileObject;

import static com.google.common.truth.Truth.assertAbout;
import static com.google.testing.compile.JavaSourcesSubjectFactory.javaSources;
import static java.util.Collections.singletonList;
import static net.jbock.compiler.ProcessorTest.fromSource;

class MapperTest {

  @Test
  void validArrayMapperSupplier() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Option(\"x\")",
        "  abstract Optional<int[]> foo();",
        "",
        "  @MapperFor(\"foo\")",
        "  static int[] doMap(String s) {",
        "    return null;",
        "  }",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .compilesWithoutError();
  }

  @Test
  void validArrayMapper() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Option(value = \"x\")",
        "  abstract Optional<int[]> foo();",
        "",
        "  @MapperFor(\"foo\")",
        "  static int[] map(String s) {",
        "    return null;",
        "  }",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .compilesWithoutError();
  }

  @Test
  void invalidFlagMapper() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Option(\"x\")",
        "  abstract Boolean flag();",
        "",
        "  @MapperFor(\"flag\")",
        "  static Boolean doMap(String s) {",
        "    return null;",
        "  }",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .compilesWithoutError();
  }

  @Test
  void validBooleanList() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Param(0)",
        "  abstract List<Boolean> booleanList();",
        "",
        "  @MapperFor(\"booleanList\")",
        "  static Boolean map(String s) {",
        "    return null;",
        "  }",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .compilesWithoutError();
  }

  @Test
  void mapperInvalidNotStringFunction() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Option(value = \"x\", mappedBy = Mapper.class)",
        "  abstract Integer number();",
        "",
        "  @MapperFor(\"number\")",
        "  static Integer doMap(Integer s) {",
        "    return null;",
        "  }",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .failsToCompile()
        .withErrorContaining("The mapper method must have a single parameter of type String.");
  }

  @Test
  void mapperInvalidReturnsString() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Option(\"x\")",
        "  abstract Integer number();",
        "",
        "  @MapperFor(\"number\")",
        "  static String map(String s) {",
        "    return null;",
        "  }",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .failsToCompile()
        .withErrorContaining("There is a problem with the mapper class: Unification failed: java.lang.String and java.lang.Integer have different erasure.");
  }

  @Test
  void rawType() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Option(\"x\")",
        "  abstract List things();",
        "",
        "  @MapperFor(\"things\")",
        "  static List doMap(String s) {",
        "    return null;",
        "  }",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .compilesWithoutError();
  }

  @Test
  void mapperValidTypevars() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Option(\"x\")",
        "  abstract Supplier<String> string();",
        "",
        "  @MapperFor(\"string\")",
        "  static Supplier<String> map(String s) {",
        "    return null;",
        "  }",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .compilesWithoutError();
  }

  @Test
  void mapperValidNestedTypevars() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Option(\"x\")",
        "  abstract Supplier<Optional<String>> string();",
        "",
        "  @MapperFor(\"string\")",
        "  static Supplier<Optional<String>> doMap(String s) {",
        "    return null;",
        "  }",
        "}");

    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .compilesWithoutError();
  }

  @Test
  void testSudokuHard() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Option(\"x\")",
        "  abstract List<List<List<List<List<List<List<Set<Set<Set<Set<Set<Set<java.util.Collection<Integer>>>>>>>>>>>>>> numbers();",
        "",
        "  @MapperFor(\"numbers\")",
        "  static List<List<List<List<List<List<List<Set<Set<Set<Set<Set<Set<java.util.Collection<Integer>>>>>>>>>>>>>> map(String x) {",
        "    return null;",
        "  }",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .compilesWithoutError();
  }

  @Test
  void mapperValid() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Option(\"x\")",
        "  abstract List<java.util.OptionalInt> numbers();",
        "",
        "  @MapperFor(\"numbers\")",
        "  static java.util.OptionalInt doMap(String s) {",
        "    return null;",
        "  }",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .compilesWithoutError();
  }

  @Test
  void mapperValidByte() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Option(\"x\")",
        "  abstract Byte number();",
        "",
        "  @MapperFor(\"number\")",
        "  static Byte doMap(String s) {",
        "    return null;",
        "  }",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .compilesWithoutError();
  }

  @Test
  void mapperValidBytePrimitive() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Option(\"x\")",
        "  abstract byte number();",
        "",
        "  @MapperFor(\"number\")",
        "  static Byte map(String s) {",
        "    return null;",
        "  }",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .compilesWithoutError();
  }

  @Test
  void mapperValidOptionalInteger() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Option(\"x\")",
        "  abstract Optional<Integer> number();",
        "",
        "  @MapperFor(\"number\")",
        "  static Integer doMap(String s) {",
        "    return null;",
        "  }",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .compilesWithoutError();
  }

  @Test
  void charSequence() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Option(\"x\")",
        "  abstract List<List<java.lang.CharSequence>> number();",
        "",
        "  @MapperFor(\"number\")",
        "  static List<List<String>> doMap(String s) {",
        "    return null;",
        "  }",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .failsToCompile()
        .withErrorContaining("There is a problem with the mapper class: Unification failed: java.lang.String and java.lang.CharSequence have different erasure.");
  }

  @Test
  void implicitMapperOptionalInt() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Option(value = \"x\")",
        "  abstract java.util.OptionalInt b();",
        "",
        "  @MapperFor(\"b\")",
        "  static Integer doMap(String s) {",
        "    return null;",
        "  }",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .compilesWithoutError();
  }

  @Test
  void mapperOptionalInt() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Option(\"x\")",
        "  abstract java.util.OptionalInt b();",
        "",
        "  @MapperFor(\"b\")",
        "  static java.util.OptionalInt map(String s) {",
        "    return null;",
        "  }",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .compilesWithoutError();
  }

  @Test
  void oneOptionalInt() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Option(value = \"x\")",
        "  abstract java.util.OptionalInt b();",
        "",
        "  static class Mapper implements Supplier<Function<String, Integer>> {",
        "    public Function<String, Integer> get() { return null; }",
        "  }",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .compilesWithoutError();
  }

  @Test
  void mapperValidListOfSet() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Option(\"x\")",
        "  abstract List<Set<Integer>> sets();",
        "",
        "  @MapperFor(\"sets\")",
        "  static Set<Integer> map(String s) {",
        "    return null;",
        "  }",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .compilesWithoutError();
  }
}
