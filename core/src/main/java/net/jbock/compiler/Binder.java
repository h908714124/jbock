package net.jbock.compiler;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static net.jbock.compiler.Analyser.STRING;
import static net.jbock.compiler.Analyser.STRING_LIST;

final class Binder {

  private final ClassName binderClass;
  private final Option option;
  private final FieldSpec optMap;
  private final FieldSpec otherTokens;
  private final FieldSpec rest = FieldSpec.builder(STRING_LIST, "rest")
      .addModifiers(PRIVATE, FINAL)
      .build();
  private final Processor.Constructor constructor;

  private Binder(ClassName binderClass,
                 Option option,
                 FieldSpec optMap,
                 FieldSpec otherTokens,
                 Processor.Constructor constructor) {
    this.binderClass = binderClass;
    this.option = option;
    this.optMap = optMap;
    this.otherTokens = otherTokens;
    this.constructor = constructor;
  }

  static Binder create(ClassName binderClass,
                       Option optionClass,
                       FieldSpec optMap,
                       FieldSpec otherTokens,
                       Processor.Constructor constructor) {
    return new Binder(binderClass, optionClass,
        optMap, otherTokens, constructor);
  }

  TypeSpec define() {
    TypeName originalClass = constructor.enclosingType;
    return TypeSpec.classBuilder(binderClass)
        .addFields(Arrays.asList(optMap, otherTokens, rest))
        .addModifiers(PUBLIC, STATIC, FINAL)
        .addMethod(privateConstructor())
        .addMethod(bindMethod())
        .addMethod(otherTokensMethod())
        .addJavadoc("Parsed arguments, ready to be passed to the constructor.\n\n" +
                "@see $T#$T($L)\n", originalClass, originalClass,
            Option.constructorArgumentsForJavadoc(constructor))
        .build();
  }

  private MethodSpec bindMethod() {
    CodeBlock.Builder builder = CodeBlock.builder();
    ParameterSpec a = ParameterSpec.builder(STRING, "a").build();
    builder.add("return new $T(\n    ", constructor.enclosingType);
    for (int j = 0; j < constructor.parameters.size(); j++) {
      if (j > 0) {
        builder.add(",\n    ");
      }
      OptionType optionType = constructor.parameters.get(j).optionType;
      if (optionType == OptionType.FLAG) {
        builder.add("$N.containsKey($T.$N)", optMap, option.optionClass, option.enumConstant(j));
      } else if (optionType == OptionType.STRING) {
        builder.add("$N.getOrDefault($T.$L, $T.emptyList()).stream()\n",
            optMap, option.optionClass, option.enumConstant(j), Collections.class)
            .add("        .findFirst().orElse(null)");
      } else if (optionType == OptionType.OTHER_TOKENS) {
        builder.add("$N", otherTokens);
      } else if (optionType == OptionType.EVERYTHING_AFTER) {
        builder.add("$N", rest);
      } else {
        builder.add("$N.getOrDefault($T.$L, $T.emptyList()).stream()\n",
            optMap, option.optionClass, option.enumConstant(j), Collections.class)
            .add("        .collect($T.toList())", Collectors.class);
      }
    }
    builder.add(");\n");
    TypeName originalClass = constructor.enclosingType;
    StringBuilder javadoc = new StringBuilder();
    javadoc.append("Invokes the constructor.\n")
        .append("\n");
    for (TypeName thrownType : constructor.thrownTypes) {
      javadoc.append("@throws ").append(thrownType.toString()).append("\n");
    }
    javadoc.append("@return an instance of {@link $T}\n");
    return MethodSpec.methodBuilder("bind")
        .addCode(builder.build())
        .addExceptions(constructor.thrownTypes)
        .addModifiers(PUBLIC)
        .addJavadoc(javadoc.toString(), originalClass)
        .returns(constructor.enclosingType)
        .build();
  }

  private MethodSpec otherTokensMethod() {
    return MethodSpec.methodBuilder("otherTokens")
        .addStatement("return $N", otherTokens)
        .addJavadoc("Collection of all unbound tokens.\n" +
            "Unless @OtherTokens is used in the constructor,\n" +
            "a good practice is to verify that this list is empty\n" +
            "before invoking {@link #bind()}.\n" +
            "\n" +
            "@return tokens that the parser ignored, an unmodifiable list\n")
        .returns(otherTokens.type)
        .addModifiers(PUBLIC)
        .build();
  }

  private MethodSpec privateConstructor() {
    CodeBlock.Builder builder = CodeBlock.builder();
    ParameterSpec optMap = ParameterSpec.builder(this.optMap.type, this.optMap.name)
        .build();
    ParameterSpec otherTokens = ParameterSpec.builder(this.otherTokens.type, this.otherTokens.name)
        .build();
    ParameterSpec esc = ParameterSpec.builder(this.rest.type, this.rest.name).build();
    builder.addStatement("this.$N = $T.unmodifiableMap($N)", this.optMap, Collections.class, optMap);
    builder.addStatement("this.$N = $T.unmodifiableList($N)", this.otherTokens, Collections.class, otherTokens);
    builder.addStatement("this.$N = $T.unmodifiableList($N)", this.rest, Collections.class, esc);
    return MethodSpec.constructorBuilder()
        .addParameters(Arrays.asList(optMap, otherTokens, esc))
        .addCode(builder.build())
        .addModifiers(PRIVATE)
        .build();
  }
}