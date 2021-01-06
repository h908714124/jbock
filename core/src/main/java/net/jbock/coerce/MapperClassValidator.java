package net.jbock.coerce;

import com.squareup.javapoet.CodeBlock;
import net.jbock.coerce.either.Either;
import net.jbock.coerce.reference.ReferenceTool;
import net.jbock.coerce.reference.ReferencedType;
import net.jbock.compiler.TypeTool;
import net.jbock.compiler.ValidationException;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeMirror;
import java.util.function.Function;

import static net.jbock.coerce.SuppliedClassValidator.commonChecks;
import static net.jbock.coerce.Util.checkNotAbstract;
import static net.jbock.coerce.Util.getTypeParameterList;
import static net.jbock.coerce.reference.ExpectedType.MAPPER;

public final class MapperClassValidator {

  private final Function<String, ValidationException> errorHandler;
  private final TypeTool tool;
  private final TypeMirror expectedReturnType;
  private final ExecutableElement mapper;

  public MapperClassValidator(Function<String, ValidationException> errorHandler, TypeTool tool,
                              TypeMirror expectedReturnType, ExecutableElement mapper) {
    this.errorHandler = errorHandler;
    this.tool = tool;
    this.expectedReturnType = expectedReturnType;
    this.mapper = mapper;
  }

  public Either<String, CodeBlock> getMapExpr() {
    commonChecks(mapper);
    checkNotAbstract(mapper);
    ReferencedType<Function<?, ?>> functionType = new ReferenceTool<>(MAPPER, this::boom, tool, mapper)
        .getReferencedType();
    TypeMirror inputType = functionType.typeArguments().get(0);
    TypeMirror outputType = functionType.typeArguments().get(1);
    return tool.unify(tool.asTypeElement(String.class.getCanonicalName()).asType(), inputType, this::boom)
        .flatMap(this::enrichMessage, inputSolution ->
            handle(functionType, outputType, inputSolution));
  }

  private Either<String, CodeBlock> handle(
      ReferencedType<Function<?, ?>> functionType,
      TypeMirror outputType,
      TypevarMapping inputSolution) {
    return tool.unify(expectedReturnType, outputType, this::boom)
        .flatMap(this::enrichMessage, outputSolution ->
            handle(functionType, inputSolution, outputSolution));
  }

  private Either<String, CodeBlock> handle(
      ReferencedType<Function<?, ?>> functionType,
      TypevarMapping inputSolution,
      TypevarMapping outputSolution) {
    return inputSolution.merge(outputSolution)
        .flatMap(Function.identity(), mapping ->
            mapping.getTypeParameters(mapper))
        .map(this::enrichMessage, typeParameters -> CodeBlock.of("new $T$L()$L",
            tool.types().erasure(mapper.asType()),
            getTypeParameterList(typeParameters.getTypeParameters()),
            functionType.isSupplier() ? ".get()" : ""));
  }

  private ValidationException boom(String message) {
    return errorHandler.apply(enrichMessage(message));
  }

  private String enrichMessage(String message) {
    return String.format("There is a problem with the mapper class: %s.", message);
  }
}
