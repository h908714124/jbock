package net.jbock.compiler.view;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeSpec;
import net.jbock.compiler.Constants;
import net.jbock.compiler.Context;

import static java.util.Arrays.asList;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.STATIC;
import static net.jbock.compiler.Constants.STRING;
import static net.jbock.compiler.view.ParserState.throwRepetitionErrorStatement;

/**
 * Generates the RegularOptionParser class, which handles non-repeatable options.
 */
final class RegularOptionParser {

  static TypeSpec define(Context context) {
    return TypeSpec.classBuilder(context.regularOptionParserType())
        .superclass(context.optionParserType())
        .addMethod(readMethod(context))
        .addMethod(constructor(context))
        .addModifiers(PRIVATE, STATIC).build();
  }

  static MethodSpec constructor(Context context) {
    ParameterSpec optionParam = ParameterSpec.builder(context.optionType(), "option").build();
    return MethodSpec.constructorBuilder()
        .addStatement("super($N)", optionParam)
        .addParameter(optionParam)
        .build();
  }

  private static MethodSpec readMethod(Context context) {
    FieldSpec option = FieldSpec.builder(context.optionType(), "option").build();
    ParameterSpec token = ParameterSpec.builder(STRING, "token").build();
    ParameterSpec it = ParameterSpec.builder(Constants.STRING_ITERATOR, "it").build();
    MethodSpec.Builder spec = MethodSpec.methodBuilder("read")
        .addParameters(asList(token, it));

    spec.beginControlFlow("if (!values.isEmpty())")
        .addStatement(throwRepetitionErrorStatement(option))
        .endControlFlow();

    spec.addStatement("super.read($N, $N)", token, it);

    return spec.addAnnotation(Override.class).build();
  }
}
