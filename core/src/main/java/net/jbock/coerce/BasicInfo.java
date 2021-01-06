package net.jbock.coerce;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import net.jbock.compiler.ParamName;
import net.jbock.compiler.TypeTool;
import net.jbock.compiler.ValidationException;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import java.util.Optional;

/**
 * Coercion input: Information about a single parameter (option or param).
 */
public class BasicInfo {

  private final ParamName paramName;

  private final ExecutableElement sourceMethod;

  private final TypeTool tool;

  private final ClassName optionType;

  // nullable
  private final ExecutableElement mapper;

  // nullable
  private final ExecutableElement collector;

  BasicInfo(
      ExecutableElement mapper,
      ExecutableElement collector,
      ParamName paramName,
      ClassName optionType,
      ExecutableElement sourceMethod,
      TypeTool tool) {
    this.mapper = mapper;
    this.collector = collector;
    this.paramName = paramName;
    this.optionType = optionType;
    this.sourceMethod = sourceMethod;
    this.tool = tool;
  }

  public Optional<CodeBlock> findAutoMapper(TypeMirror testType) {
    Optional<CodeBlock> mapExpr = AutoMapper.findAutoMapper(tool(), testType);
    if (mapExpr.isPresent()) {
      return mapExpr;
    }
    if (isEnumType(testType)) {
      return Optional.of(CodeBlock.of("$T::valueOf", testType));
    }
    return Optional.empty();
  }

  public ParamName parameterName() {
    return paramName;
  }

  public ParameterSpec constructorParam(TypeMirror type) {
    return ParameterSpec.builder(TypeName.get(type), paramName.camel()).build();

  }

  public TypeMirror returnType() {
    return sourceMethod.getReturnType();
  }

  public ValidationException failure(String message) {
    return ValidationException.create(sourceMethod, message);
  }

  public TypeTool tool() {
    return tool;
  }

  Optional<ExecutableElement> mapperClass() {
    return Optional.ofNullable(mapper);
  }

  Optional<ExecutableElement> collectorClass() {
    return Optional.ofNullable(collector);
  }

  public ClassName optionType() {
    return optionType;
  }

  private boolean isEnumType(TypeMirror mirror) {
    Types types = tool.types();
    return types.directSupertypes(mirror).stream()
        .anyMatch(t -> tool.isSameErasure(t, Enum.class.getCanonicalName()));
  }
}
