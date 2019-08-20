package net.jbock.coerce;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import net.jbock.compiler.TypeTool;
import net.jbock.compiler.ValidationException;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.util.Optional;
import java.util.function.Function;

import static javax.lang.model.element.Modifier.FINAL;
import static net.jbock.compiler.Util.snakeToCamel;

public class BasicInfo {

  private final InferredAttributes attributes;

  private final String paramName;

  private final ExecutableElement sourceMethod;

  private final TypeTool tool;

  private final Optional<TypeElement> mapperClass;

  private final Optional<TypeElement> collectorClass;

  private BasicInfo(
      InferredAttributes attributes,
      String paramName,
      ExecutableElement sourceMethod,
      TypeTool tool,
      Optional<TypeElement> mapperClass,
      Optional<TypeElement> collectorClass) {
    this.attributes = attributes;
    this.paramName = paramName;
    this.sourceMethod = sourceMethod;
    this.tool = tool;
    this.mapperClass = mapperClass;
    this.collectorClass = collectorClass;
  }

  static BasicInfo create(
      TypeElement mapperClass,
      TypeElement collectorClass,
      InferredAttributes attributes,
      String paramName,
      ExecutableElement sourceMethod,
      TypeTool tool) {
    return new BasicInfo(attributes, snakeToCamel(paramName), sourceMethod, tool, Optional.ofNullable(mapperClass), Optional.ofNullable(collectorClass));
  }

  public String paramName() {
    return paramName;
  }

  public TypeMirror returnType() {
    return attributes.liftedType().liftedType();
  }

  public TypeMirror originalReturnType() {
    return attributes.liftedType().liftedType();
  }

  Function<ParameterSpec, CodeBlock> extractExpr() {
    return attributes.liftedType().extractExpr();
  }

  FieldSpec fieldSpec() {
    return FieldSpec.builder(TypeName.get(attributes.liftedType().originalType()), paramName, FINAL).build();
  }

  ValidationException asValidationException(String message) {
    return ValidationException.create(sourceMethod, message);
  }

  TypeTool tool() {
    return tool;
  }

  public Optional<TypeMirror> optionalInfo() {
    return attributes.optionalInfo();
  }

  ExecutableElement sourceMethod() {
    return sourceMethod;
  }

  public boolean isRepeatable() {
    return attributes.repeatable();
  }

  public boolean isOptional() {
    return optionalInfo().isPresent();
  }

  public Optional<TypeElement> mapperClass() {
    return mapperClass;
  }

  public Optional<TypeElement> collectorClass() {
    return collectorClass;
  }
}
