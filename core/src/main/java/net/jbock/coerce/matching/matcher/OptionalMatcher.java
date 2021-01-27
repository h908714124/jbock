package net.jbock.coerce.matching.matcher;

import com.squareup.javapoet.CodeBlock;
import net.jbock.coerce.Skew;
import net.jbock.coerce.matching.UnwrapSuccess;
import net.jbock.compiler.ParameterContext;
import net.jbock.either.Either;

import javax.inject.Inject;

public class OptionalMatcher extends Matcher {

  private final Optionalish optionalish;

  @Inject
  OptionalMatcher(ParameterContext parameterContext, Optionalish optionalish) {
    super(parameterContext);
    this.optionalish = optionalish;
  }

  @Override
  public Either<String, UnwrapSuccess> tryUnwrapReturnType() {
    return optionalish.unwrap(returnType());
  }

  @Override
  public Skew skew() {
    return Skew.OPTIONAL;
  }

  @Override
  public CodeBlock tailExpr() {
    return CodeBlock.of(".findAny()");
  }
}