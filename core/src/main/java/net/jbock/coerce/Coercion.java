package net.jbock.coerce;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.ParameterSpec;
import net.jbock.compiler.ParamName;

public abstract class Coercion {

  private final ParameterSpec constructorParam;

  private final ParamName paramName;

  Coercion(ParameterSpec constructorParam, ParamName paramName) {
    this.constructorParam = constructorParam;
    this.paramName = paramName;
  }

  public final ParameterSpec constructorParam() {
    return constructorParam;
  }

  public final ParamName paramName() {
    return paramName;
  }

  public abstract CodeBlock mapExpr();

  public abstract CodeBlock extractExpr();

  public abstract CodeBlock collectExpr();

  public abstract Skew getSkew();
}
