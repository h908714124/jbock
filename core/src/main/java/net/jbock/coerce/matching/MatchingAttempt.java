package net.jbock.coerce.matching;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.ParameterSpec;
import net.jbock.coerce.BasicInfo;
import net.jbock.coerce.Coercion;
import net.jbock.coerce.MapperClassValidator;
import net.jbock.coerce.NonFlagCoercion;
import net.jbock.coerce.NonFlagSkew;
import net.jbock.coerce.either.Either;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeMirror;
import java.util.function.Function;
import java.util.stream.Collectors;

class MatchingAttempt {

  private final CodeBlock extractExpr;
  private final ParameterSpec constructorParam;
  private final NonFlagSkew skew;
  private final TypeMirror testType;
  private final ExecutableElement mapper;

  MatchingAttempt(TypeMirror testType, CodeBlock extractExpr, ParameterSpec constructorParam, NonFlagSkew skew,
                  ExecutableElement mapper) {
    this.testType = testType;
    this.extractExpr = extractExpr;
    this.constructorParam = constructorParam;
    this.skew = skew;
    this.mapper = mapper;
  }

  static CodeBlock autoCollectExpr(BasicInfo basicInfo, NonFlagSkew skew) {
    switch (skew) {
      case OPTIONAL:
        return CodeBlock.of(".findAny()");
      case REQUIRED:
        return CodeBlock.of(".findAny().orElseThrow($T.$L::missingRequired)", basicInfo.optionType(),
            basicInfo.parameterName().enumConstant());
      case REPEATABLE:
        return CodeBlock.of(".collect($T.toList())", Collectors.class);
      default:
        throw new AssertionError("unknown skew: " + skew);
    }
  }

  Either<String, Coercion> findCoercion(BasicInfo basicInfo) {
    MapperClassValidator validator = new MapperClassValidator(basicInfo::failure, basicInfo.tool(), testType, mapper);
    return validator.getMapExpr().map(Function.identity(), mapExpr -> {
      CodeBlock expr = autoCollectExpr(basicInfo, skew);
      return new NonFlagCoercion(basicInfo, mapExpr, expr, extractExpr, skew, constructorParam);
    });
  }
}
