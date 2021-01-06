package net.jbock.coerce;

import net.jbock.compiler.TypeTool;
import net.jbock.compiler.ValidationException;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeMirror;
import java.util.List;
import java.util.function.Function;

class CollectorClassValidator {

  private final Function<String, ValidationException> errorHandler;
  private final TypeTool tool;
  private final ExecutableElement collector;
  private final TypeMirror returnType;

  CollectorClassValidator(
      Function<String, ValidationException> errorHandler,
      TypeTool tool,
      ExecutableElement collector,
      TypeMirror returnType) {
    this.errorHandler = errorHandler;
    this.tool = tool;
    this.collector = collector;
    this.returnType = returnType;
  }

  CollectorInfo getCollectorInfo() {
    TypeMirror collectorType = collector.getReturnType();
    List<? extends TypeMirror> typeArguments = TypeTool.asDeclared(collectorType).getTypeArguments();
    if (typeArguments.isEmpty()) {
      throw boom("raw collector type");
    }
    TypeMirror inputType = typeArguments.get(0);
    TypeMirror outputType = typeArguments.get(2);
    tool.unify(this.returnType, outputType, this::boom)
        .orElseThrow(this::boom);
    return CollectorInfo.create(inputType, collector);
  }

  private ValidationException boom(String message) {
    return errorHandler.apply(String.format("There is a problem with the collector class: %s.", message));
  }
}
