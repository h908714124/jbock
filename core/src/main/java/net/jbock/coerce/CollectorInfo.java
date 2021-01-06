package net.jbock.coerce;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

class CollectorInfo {

  // For custom collector this is the T in Collector<T, A, R>.
  // For default collector it is the E in List<E>.
  private final TypeMirror inputType;

  private final ExecutableElement collector;

  private CollectorInfo(TypeMirror inputType, ExecutableElement collector) {
    this.inputType = inputType;
    this.collector = collector;
  }

  static CollectorInfo create(TypeMirror inputType, ExecutableElement collectorClass) {
    return new CollectorInfo(inputType, collectorClass);
  }

  TypeMirror inputType() {
    return inputType;
  }

  CodeBlock collectExpr(TypeElement sourceElement) {
    return CodeBlock.of(".collect($T.$L())",
        TypeName.get(sourceElement.asType()),
        collector.getSimpleName().toString());
  }
}
