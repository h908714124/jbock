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
        "  @Option(value = \"x\", mappedBy = ArrayMapper.class)",
        "  abstract Optional<int[]> foo();",
        "",
        "  static class ArrayMapper implements Supplier<Function<String, int[]>> {",
        "    public Function<String, int[]> get() { return null; }",
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
  void validMapperWithTypeParameter() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Option(value = \"x\", mappedBy = IdentityMapper.class)",
        "  abstract String string();",
        "",
        "  static class IdentityMapper<E> implements Function<E, E> {",
        "    public E apply(E e) { return null; }",
        "  }",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .compilesWithoutError();
  }

  @Test
  void validMapperWithTypeParameters() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Option(value = \"x\", mappedBy = IdentityMapper.class)",
        "  abstract String string();",
        "",
        "  static class IdentityMapper<E, F> implements Function<E, F> {",
        "    public F apply(E e) { return null; }",
        "  }",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .compilesWithoutError();
  }

  @Test
  void validMapperTypeParameterWithBounds() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Option(value = \"x\", mappedBy = IdentityMapper.class)",
        "  abstract String string();",
        "",
        "  static class IdentityMapper<E extends java.lang.CharSequence> implements Function<E, E> {",
        "    public E apply(E e) { return null; }",
        "  }",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .compilesWithoutError();
  }

  @Test
  void validMapperTypeParameterSupplierWithBounds() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Option(value = \"x\", mappedBy = IdentityMapper.class)",
        "  abstract String string();",
        "",
        "  static class IdentityMapper<E extends java.lang.CharSequence> implements Supplier<Function<E, E>> {",
        "    public Function<E, E> get() { return null; }",
        "  }",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .compilesWithoutError();
  }

  @Test
  void invalidMapperTypeParameterSupplierWithBounds() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Option(value = \"x\", mappedBy = IdentityMapper.class)",
        "  abstract String string();",
        "",
        "  static class IdentityMapper<E extends Integer> implements Supplier<Function<E, E>> {",
        "    public Function<E, E> get() { return null; }",
        "  }",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .failsToCompile()
        .withErrorContaining("There is a problem with the mapper class: Unification failed: can't choose E to be java.lang.String.");
  }


  @Test
  void invalidFlagMapper() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Option(value = \"x\", mappedBy = FlagMapper.class)",
        "  abstract Boolean flag();",
        "",
        "  static class FlagMapper implements Supplier<Function<String, Boolean>> {",
        "    public Function<String, Boolean> get() { return null; }",
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
        "  @Param(value = 0, mappedBy = BooleanMapper.class)",
        "  abstract List<Boolean> booleanList();",
        "",
        "  static class BooleanMapper implements Supplier<Function<String, Boolean>> {",
        "    public Function<String, Boolean> get() { return null; }",
        "  }",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .compilesWithoutError();
  }

  @Test
  void validBounds() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Param(value = 1, mappedBy = BoundMapper.class)",
        "  abstract String a();",
        "",
        "  static class BoundMapper implements Katz<String> {",
        "    public Function<String, String> get() { return null; }",
        "  }",
        "",
        "  interface Katz<OR> extends Supplier<Function<OR, OR>> { }",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .failsToCompile()
        .withErrorContaining("not a java.util.function.Function or java.util.function.Supplier<java.util.function.Function>");
  }

  @Test
  void mapperInvalidPrivateConstructor() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Option(value = \"x\", mappedBy = Mapper.class)",
        "  abstract Integer number();",
        "",
        "  static class Mapper implements Supplier<Function<String, Integer>> {",
        "",
        "    private Mapper() {}",
        "",
        "    public Function<String, Integer> get() { return null; }",
        "  }",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .failsToCompile()
        .withErrorContaining("constructor");
  }

  @Test
  void mapperInvalidNoDefaultConstructor() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Option(value = \"x\", mappedBy = Mapper.class)",
        "  abstract Integer number();",
        "",
        "  static class Mapper implements Supplier<Function<String, Integer>> {",
        "",
        "    Mapper(int i) {}",
        "",
        "    public Function<String, Integer> get() { return null; }",
        "  }",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .failsToCompile()
        .withErrorContaining("must have a default constructor");
  }

  @Test
  void mapperInvalidConstructorException() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Option(value = \"x\", mappedBy = Mapper.class)",
        "  abstract Integer number();",
        "",
        "  static class Mapper implements Supplier<Function<String, Integer>> {",
        "",
        "    Mapper() throws IllegalStateException {}",
        "",
        "    public Function<String, Integer> get() { return null; }",
        "  }",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .failsToCompile()
        .withErrorContaining("The class must have a default constructor");
  }

  @Test
  void mapperInvalidNonstaticInnerClass() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Option(value = \"x\", mappedBy = Mapper.class)",
        "  abstract Integer number();",
        "",
        "  class Mapper implements Supplier<Function<String, Integer>> {",
        "    public Function<String, Integer> get() { return null; }",
        "  }",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .failsToCompile()
        .withErrorContaining("class must be static");
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
        "  static class Mapper implements Supplier<Function<Integer, Integer>> {",
        "    public Function<Integer, Integer> get() { return null; }",
        "  }",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .failsToCompile()
        .withErrorContaining("There is a problem with the mapper class");
  }

  @Test
  void mapperInvalidReturnsString() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Option(value = \"x\", mappedBy = Mapper.class)",
        "  abstract Integer number();",
        "",
        "  static class Mapper implements Supplier<Function<String, String>> {",
        "    public Function<String, String> get() { return null; }",
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
        "  @Option(value = \"x\", mappedBy = Mapper.class)",
        "  abstract List things();",
        "",
        "  static class Mapper implements Supplier<Function<String, List>> {",
        "    public Function<String, List> get() { return null; }",
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
        "  @Option(value = \"x\", mappedBy = Mapper.class)",
        "  abstract Supplier<String> string();",
        "",
        "  static class Mapper implements Supplier<Function<String, Supplier<String>>> {",
        "    public Function<String, Supplier<String>> get() { return null; }",
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
        "  @Option(value = \"x\", mappedBy = Mapper.class)",
        "  abstract Supplier<Optional<String>> string();",
        "",
        "  static class Mapper implements Supplier<Function<String, Supplier<Optional<String>>>> {",
        "    public Function<String, Supplier<Optional<String>>> get() { return null; }",
        "  }",
        "}");

    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .compilesWithoutError();
  }

  @Test
  void mapperValidExtendsFunction() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Option(value = \"x\", mappedBy = Mapper.class)",
        "  abstract Integer number();",
        "",
        "  static class Mapper<E> implements Supplier<StringFunction<E, Integer>> {",
        "    public StringFunction<E, Integer> get() { return null; }",
        "  }",
        "",
        "  interface StringFunction<V, X> extends Function<V, X> {}",
        "",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .failsToCompile()
        .withErrorContaining("expected java.util.function.Function but found test.Arguments.StringFunction<E,java.lang.Integer>");
  }

  @Test
  void mapperInvalidStringFunction() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Option(value = \"x\", mappedBy = Mapper.class)",
        "  abstract Integer number();",
        "",
        "  static class Mapper implements Supplier<StringFunction<Integer>> {",
        "    public StringFunction<Integer> get() { return null; }",
        "  }",
        "",
        "  interface StringFunction<R> extends Function<Long, R> {}",
        "",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .failsToCompile()
        .withErrorContaining("expected java.util.function.Function but found test.Arguments.StringFunction<java.lang.Integer>");
  }

  @Test
  void testMapperTypeSudokuInvalid() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Option(value = \"x\", mappedBy = Mapper.class)",
        "  abstract List<List<Integer>> number();",
        "",
        "  static class Mapper<E extends List<List<Integer>>> implements FooSupplier<E> { public Foo<E> get() { return null; } }",
        "  interface FooSupplier<K> extends Supplier<Foo<K>> { }",
        "  interface Foo<X> extends Function<String, List<List<X>>> { }",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .failsToCompile()
        .withErrorContaining("There is a problem with the mapper class");
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
  void mapperInvalidRawSupplier() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Option(value = \"x\", mappedBy = Mapper.class)",
        "  abstract Integer number();",
        "",
        "  static class Mapper implements Supplier {",
        "    public Object get() { return null; }",
        "  }",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .failsToCompile()
        .withErrorContaining("There is a problem with the mapper class: raw type: java.util.function.Supplier.");
  }

  @Test
  void mapperInvalidRawFunction() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Option(value = \"x\", mappedBy = Mapper.class)",
        "  abstract Integer number();",
        "",
        "  static class Mapper implements Function {",
        "    public Object apply(Object o) { return null; }",
        "  }",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .failsToCompile()
        .withErrorContaining("There is a problem with the mapper class: raw type: java.util.function.Function.");
  }

  @Test
  void mapperInvalidSupplyingTypevar() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Option(value = \"x\", mappedBy = Mapper.class)",
        "  abstract Integer number();",
        "",
        "  static class Mapper<E> implements Supplier<E> {",
        "    public E get() { return null; }",
        "  }",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .failsToCompile()
        .withErrorContaining("There is a problem with the mapper class: not a java.util.function.Function or java.util.function.Supplier<java.util.function.Function>");
  }

  @Test
  void mapperValid() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Option(value = \"x\", mappedBy = Mapper.class)",
        "  abstract List<java.util.OptionalInt> numbers();",
        "",
        "  static class Mapper implements Supplier<Function<String, java.util.OptionalInt>> {",
        "    public Function<String, java.util.OptionalInt> get() { return null; }",
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
        "  @Option(value = \"x\", mappedBy = Mapper.class)",
        "  abstract Byte number();",
        "",
        "  static class Mapper implements Supplier<Function<String, Byte>> {",
        "    public Function<String, Byte> get() { return null; }",
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
        "  @Option(value = \"x\", mappedBy = Mapper.class)",
        "  abstract Optional<Integer> number();",
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
  void mapperValidStringOptionalStringTypevar() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Option(value = \"x\", mappedBy = Mapper.class)",
        "  abstract Optional<String> number();",
        "",
        "  static class Mapper<E> implements Supplier<Function<E, Optional<E>>> {",
        "    public Function<E, Optional<E>> get() { return null; }",
        "  }",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .compilesWithoutError();
  }

  @Test
  void mapperValidStringListTypevar() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Option(value = \"x\", mappedBy = Mapper.class)",
        "  abstract List<String> number();",
        "",
        "  static class Mapper<E> implements Supplier<Function<E, List<E>>> {",
        "    public Function<E, List<E>> get() { return null; }",
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
        "  @Option(value = \"x\", mappedBy = Mapper.class)",
        "  abstract List<List<java.lang.CharSequence>> number();",
        "",
        "  static class Mapper implements Supplier<Function<String, List<List<String>>>> {",
        "    public Function<String, List<List<String>>> get() { return null; }",
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
        "  @Option(value = \"x\", mappedBy = Mapper.class)",
        "  abstract java.util.OptionalInt b();",
        "",
        "  static class Mapper implements Supplier<Function<String, java.util.OptionalInt>> {",
        "    public Function<String, java.util.OptionalInt> get() { return null; }",
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
