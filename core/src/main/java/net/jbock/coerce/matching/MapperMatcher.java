package net.jbock.coerce.matching;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.ParameterSpec;
import net.jbock.coerce.BasicInfo;
import net.jbock.coerce.Coercion;
import net.jbock.coerce.NonFlagSkew;
import net.jbock.coerce.either.Either;
import net.jbock.coerce.either.Left;
import net.jbock.coerce.either.Right;
import net.jbock.compiler.TypeTool;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static net.jbock.coerce.NonFlagSkew.OPTIONAL;
import static net.jbock.coerce.NonFlagSkew.REPEATABLE;
import static net.jbock.coerce.NonFlagSkew.REQUIRED;
import static net.jbock.coerce.either.Either.left;
import static net.jbock.coerce.matching.AutoMatcher.boxedType;

public class MapperMatcher {

  private final ExecutableElement mapper;
  private final BasicInfo basicInfo;

  public MapperMatcher(BasicInfo basicInfo, ExecutableElement mapper) {
    this.mapper = mapper;
    this.basicInfo = basicInfo;
  }

  private List<MatchingAttempt> getAttempts() {
    TypeMirror returnType = basicInfo.returnType();
    Optional<Optionalish> opt = Optionalish.unwrap(returnType, tool());
    Optional<TypeMirror> listWrapped = tool().getSingleTypeArgument(returnType, List.class.getCanonicalName());
    List<MatchingAttempt> attempts = new ArrayList<>();
    opt.ifPresent(optional -> {
      ParameterSpec param = basicInfo.constructorParam(optional.liftedType());
      // optional match
      attempts.add(attempt(optional.wrappedType(), optional.extractExpr(param), param, OPTIONAL));
      // exact match (-> required)
      attempts.add(attempt(optional.liftedType(), optional.extractExpr(param), param, REQUIRED));
    });
    listWrapped.ifPresent(wrapped -> {
      ParameterSpec param = basicInfo.constructorParam(returnType);
      // list match
      attempts.add(attempt(wrapped, param, REPEATABLE));
    });
    ParameterSpec param = basicInfo.constructorParam(returnType);
    // exact match (-> required)
    attempts.add(attempt(boxedType(returnType, tool().types()), param, REQUIRED));
    return attempts;
  }

  public Coercion findCoercion() {
    List<MatchingAttempt> attempts = getAttempts();
    Either<String, Coercion> either = left("");
    for (MatchingAttempt attempt : attempts) {
      either = attempt.findCoercion(basicInfo);
      if (either instanceof Right) {
        return ((Right<String, Coercion>) either).value();
      }
    }
    throw basicInfo.failure(((Left<String, Coercion>) either).value());
  }

  private TypeTool tool() {
    return basicInfo.tool();
  }

  private MatchingAttempt attempt(TypeMirror expectedReturnType, CodeBlock extractExpr, ParameterSpec constructorParam, NonFlagSkew skew) {
    return new MatchingAttempt(expectedReturnType, extractExpr, constructorParam, skew, mapper);
  }

  private MatchingAttempt attempt(TypeMirror expectedReturnType, ParameterSpec constructorParam, NonFlagSkew skew) {
    return new MatchingAttempt(expectedReturnType, CodeBlock.of("$N", constructorParam), constructorParam, skew, mapper);
  }
}
