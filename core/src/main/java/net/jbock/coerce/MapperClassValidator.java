package net.jbock.coerce;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import net.jbock.coerce.either.Either;
import net.jbock.compiler.TypeTool;
import net.jbock.compiler.ValidationException;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.util.function.Function;

public final class MapperClassValidator {

  private final TypeElement sourceElement;
  private final Function<String, ValidationException> errorHandler;
  private final TypeTool tool;
  private final TypeMirror expectedReturnType;
  private final ExecutableElement mapper;

  public MapperClassValidator(
      TypeElement sourceElement, Function<String, ValidationException> errorHandler, TypeTool tool,
      TypeMirror expectedReturnType, ExecutableElement mapper) {
    this.sourceElement = sourceElement;
    this.errorHandler = errorHandler;
    this.tool = tool;
    this.expectedReturnType = expectedReturnType;
    this.mapper = mapper;
  }

  public Either<String, CodeBlock> getMapExpr() {
    TypeMirror outputType = mapper.getReturnType();
    return tool.unify(expectedReturnType, outputType, this::boom)
        .map(this::enrichMessage, this::handle);
  }

  private CodeBlock handle(TypevarMapping outputSolution) {
    return CodeBlock.of("$T::$L",
        TypeName.get(sourceElement.asType()),
        mapper.getSimpleName().toString());
  }

  private ValidationException boom(String message) {
    return errorHandler.apply(enrichMessage(message));
  }

  private String enrichMessage(String message) {
    return String.format("There is a problem with the mapper class: %s.", message);
  }
}
