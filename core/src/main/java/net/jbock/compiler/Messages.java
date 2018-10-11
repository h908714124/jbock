package net.jbock.compiler;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeSpec;

import java.util.Arrays;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

import static java.util.Arrays.asList;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.STATIC;
import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.classBuilder;

/**
 * Defines the inner class IndentPrinter.
 */
final class Messages {

  private final FieldSpec resourceBundle = FieldSpec.builder(ResourceBundle.class, "resourceBundle")
      .addModifiers(FINAL).build();

  private final Context context;

  private final FieldSpec br = FieldSpec.builder(Pattern.class, "br")
      .initializer("$T.compile($S)", Pattern.class, "\\r?\\n")
      .addModifiers(FINAL)
      .build();

  private Messages(Context context) {
    this.context = context;
  }

  static Messages create(Context context) {
    return new Messages(context);
  }

  TypeSpec define() {
    return classBuilder(context.messagesType())
        .addFields(asList(br, resourceBundle))
        .addMethod(privateConstructor())
        .addMethod(getMessageMethod())
        .addModifiers(PRIVATE, STATIC).build();
  }

  private MethodSpec getMessageMethod() {
    ParameterSpec defaultValue = ParameterSpec.builder(Constants.LIST_OF_STRING, "defaultValue").build();
    ParameterSpec key = ParameterSpec.builder(String.class, "key").build();
    return methodBuilder("getMessage")
        .addParameter(key)
        .addParameter(defaultValue)
        .returns(Constants.LIST_OF_STRING)
        .beginControlFlow("if ($N == null || !$N.containsKey($N))", resourceBundle, resourceBundle, key)
        .addStatement("return $N", defaultValue)
        .endControlFlow()
        .addStatement("return $T.asList($N.split($N.getString($N), -1))", Arrays.class, br, resourceBundle, key)
        .build();
  }

  private MethodSpec privateConstructor() {
    ParameterSpec param = ParameterSpec.builder(resourceBundle.type, resourceBundle.name).build();
    return MethodSpec.constructorBuilder()
        .addParameter(param)
        .addStatement("this.$N = $N", resourceBundle, param)
        .build();
  }
}