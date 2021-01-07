package net.jbock.compiler;

import org.junit.jupiter.api.Test;

import javax.tools.JavaFileObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.google.common.truth.Truth.assertAbout;
import static com.google.testing.compile.JavaFileObjects.forSourceLines;
import static com.google.testing.compile.JavaSourcesSubjectFactory.javaSources;
import static java.util.Collections.singletonList;

class ProcessorTest {

  @Test
  void emptyLongName() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "  @Option(\"\") abstract String a();",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .failsToCompile()
        .withErrorContaining("The name may not be empty");
  }

  @Test
  void duplicateOptionName() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "  @Option(\"x\") abstract String a();",
        "  @Option(\"x\") abstract String b();",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .failsToCompile()
        .withErrorContaining("Duplicate option name");
  }

  @Test
  void duplicateMnemonic() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "  @Option(value = \"x\", mnemonic = 'x') abstract String a();",
        "  @Option(value = \"y\", mnemonic = 'x') abstract String b();",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .failsToCompile()
        .withErrorContaining("Duplicate mnemonic");
  }

  @Test
  void unknownReturnType() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Option(\"x\")",
        "  abstract StringBuilder a();",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .failsToCompile()
        .withErrorContaining("Unknown parameter type: java.lang.StringBuilder. Try defining a custom mapper or collector.");
  }

  @Test
  void declaredException() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Option(\"x\")",
        "  abstract String a() throws IllegalArgumentException;",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .failsToCompile()
        .withErrorContaining("may not declare any exceptions");
  }

  @Test
  void methodNotAbstract() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "class Arguments {",
        "",
        "  @Option(\"x\")",
        "  String a() { return null; }",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .failsToCompile()
        .withErrorContaining("The method must be abstract.");
  }

  @Test
  void rawList() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Option(\"x\")",
        "  abstract List a();",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .failsToCompile()
        .withErrorContaining("Unknown parameter type: java.util.List. Try defining a custom mapper or collector.");
  }

  @Test
  void rawOptional() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Option(\"x\")",
        "  abstract Optional a();",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .failsToCompile()
        .withErrorContaining("Unknown parameter type: java.util.Optional. Try defining a custom mapper or collector.");
  }

  @Test
  void parameterizedSet() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Option(\"x\")",
        "  abstract java.util.Set<String> a();",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .failsToCompile()
        .withErrorContaining("Unknown parameter type: java.util.Set<java.lang.String>. Try defining a custom mapper or collector.");
  }

  @Test
  void integerArray() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Option(\"x\")",
        "  abstract int[] a();",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .failsToCompile()
        .withErrorContaining("Unknown parameter type: int[]. Try defining a custom mapper or collector.");
  }

  @Test
  void utilDate() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Option(\"x\")",
        "  abstract java.util.Date a();",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .failsToCompile()
        .withErrorContaining("Unknown parameter type: java.util.Date. Try defining a custom mapper or collector.");
  }

  @Test
  void interfaceNotClass() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "interface Arguments {",
        "  abstract String a();",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .failsToCompile()
        .withErrorContaining("Use a class, not an interface");
  }

  @Test
  void whitespaceInName() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "  @Option(\"a \")",
        "  abstract String a();",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .failsToCompile()
        .withErrorContaining("may not contain whitespace");
  }

  @Test
  void noMethods() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .failsToCompile()
        .withErrorContaining("Define at least one abstract method");
  }

  @Test
  void oneOptionalIntNotOptional() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Option(\"x\")",
        "  abstract java.util.OptionalInt b();",
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
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .compilesWithoutError();
  }

  @Test
  void simpleFlag() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Option(value = \"x\")",
        "  abstract boolean x();",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .compilesWithoutError();
  }

  @Test
  void simpleInt() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Option(value = \"x\")",
        "  abstract int aRequiredInt();",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .compilesWithoutError();
  }

  @Test
  void extendsNotAllowed() {
    JavaFileObject javaFile = fromSource(
        "abstract class Arguments {",
        "",
        "  @Command",
        "  static abstract class Foo extends Arguments {",
        "    abstract String a();",
        "  }",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .failsToCompile()
        .withErrorContaining("The model class may not implement or extend anything");
  }

  @Test
  void implementsNotAllowed() {
    JavaFileObject javaFile = fromSource(
        "interface Arguments {",
        "",
        "  @Command",
        "  abstract class Foo implements Arguments {",
        "    abstract String a();",
        "  }",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .failsToCompile()
        .withErrorContaining("implement");
  }

  @Test
  void missingCommandAnnotation() {
    JavaFileObject javaFile = fromSource(
        "abstract class Arguments {",
        "  @Option(value = \"a\") abstract String a();",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .failsToCompile()
        .withErrorContaining("missing @Command annotation");
  }

  @Test
  void abstractMethodHasParameter() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "  @Option(value = \"x\") abstract String a(int b, int c);",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .failsToCompile()
        .withErrorContaining("The method may not have any parameters.");
  }

  @Test
  void typeParameter() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "  @Option(value = \"x\") abstract <E> String a();",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .failsToCompile()
        .withErrorContaining("The method may not have any type parameters.");
  }

  @Test
  void missingAnnotation() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  abstract List<String> a();",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .failsToCompile()
        .withErrorContaining("Annotate this method with either @Option or @Param");
  }

  @Test
  void positionalFlag() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Param(value = 1)",
        "  abstract boolean hello();",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .failsToCompile()
        .withErrorContaining("Unknown parameter type: boolean. Try defining a custom mapper or collector.");
  }

  @Test
  void doubleAnnotation() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Option(\"x\")",
        "  @Param(1)",
        "  abstract List<String> a();",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .failsToCompile()
        .withErrorContaining("Use either @Option or @Param annotation, but not both");
  }

  @Test
  void twoLists() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Option(\"x\")",
        "  abstract List<String> a();",
        "",
        "  @Param(1)",
        "  abstract List<String> b();",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .compilesWithoutError();
  }

  @Test
  void innerEnum() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Option(\"x\")",
        "  abstract Foo foo();",
        "",
        "  enum Foo {",
        "    BAR",
        "   }",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .compilesWithoutError();
  }

  @Test
  void privateParameterType() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Option(\"x\")",
        "  abstract Foo foo();",
        "",
        "  private enum Foo {",
        "    BAR",
        "   }",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .failsToCompile()
        .withErrorContaining("Unreachable parameter type.");
  }

  @Test
  void unreachableParameterType() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Option(\"x\")",
        "  abstract List<Foo> foo();",
        "",
        "  private enum Foo {",
        "    BAR",
        "   }",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .failsToCompile()
        .withErrorContaining("Unreachable parameter type.");
  }


  @Test
  void invalidNesting() {
    JavaFileObject javaFile = fromSource(
        "class Arguments {",
        "  private static class Foo {",
        "    @Command",
        "    abstract static class Bar {",
        "      @Option(\"x\") abstract String a();",
        "    }",
        "  }",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .failsToCompile()
        .withErrorContaining("The class may not not be private");
  }

  static JavaFileObject fromSource(String... lines) {
    List<String> sourceLines = withImports(lines);
    return forSourceLines("test.Arguments", sourceLines);
  }

  static List<String> withImports(String... lines) {
    List<String> header = Arrays.asList(
        "package test;",
        "",
        "import java.util.List;",
        "import java.util.Set;",
        "import java.util.Optional;",
        "import java.util.function.Function;",
        "import java.util.function.Supplier;",
        "import java.util.stream.Collector;",
        "",
        "import net.jbock.Command;",
        "import net.jbock.Param;",
        "import net.jbock.Option;",
        "import net.jbock.MapperFor;",
        "import net.jbock.CollectorFor;",
        "");
    List<String> moreLines = new ArrayList<>(lines.length + header.size());
    moreLines.addAll(header);
    Collections.addAll(moreLines, lines);
    return moreLines;
  }
}
