package net.jbock.coerce;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import net.jbock.coerce.collector.AbstractCollector;
import net.jbock.coerce.collector.DefaultCollector;
import net.jbock.coerce.mapper.MapperType;
import net.jbock.coerce.mapper.ReferenceMapperType;
import net.jbock.compiler.ParamName;
import net.jbock.compiler.TypeTool;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static javax.lang.model.element.Modifier.FINAL;

public class CoercionProvider {

  private final BasicInfo basicInfo;

  private CoercionProvider(BasicInfo basicInfo) {
    this.basicInfo = basicInfo;
  }

  public static Coercion flagCoercion(ExecutableElement sourceMethod, ParamName paramName) {
    ParameterSpec name = ParameterSpec.builder(TypeName.get(sourceMethod.getReturnType()), paramName.snake()).build();
    return new Coercion(
        Optional.empty(),
        CodeBlock.of(""),
        name,
        FieldSpec.builder(TypeName.get(sourceMethod.getReturnType()), paramName.snake(), FINAL).build(),
        CodeBlock.of("$N", name),
        true);
  }

  public static Coercion findCoercion(
      ExecutableElement sourceMethod,
      ParamName paramName,
      TypeElement mapperClass,
      TypeElement collectorClass,
      InferredAttributes attributes,
      TypeTool tool) {
    BasicInfo basicInfo = BasicInfo.create(
        mapperClass, collectorClass,
        attributes, paramName, sourceMethod, tool);
    CoercionProvider coercionProvider = new CoercionProvider(basicInfo);
    return coercionProvider.run();
  }

  private Coercion run() {
    if (basicInfo.collectorClass().isPresent()) {
      return handleRepeatable();
    } else {
      return handleNotRepeatable();
    }
  }

  private Coercion handleNotRepeatable() {
    if (basicInfo.mapperClass().isPresent()) {
      return new ExplicitMapperNotRepeatableHandler(basicInfo.mapperClass().get(), basicInfo).handleExplicitMapperNotRepeatable();
    } else {
      return handleAutoMapperNotRepeatable();
    }
  }

  private Coercion handleRepeatable() {
    if (basicInfo.mapperClass().isPresent()) {
      return handleRepeatableExplicitMapper(basicInfo.mapperClass().get());
    } else {
      return handleRepeatableAutoMapper();
    }
  }

  // TODO refactoring
  private Coercion handleAutoMapperNotRepeatable() {
    Optional<CodeBlock> mapExpr = findAutoMapper(tool().box(basicInfo.originalReturnType()));
    Function<ParameterSpec, CodeBlock> extractExpr;
    Optional<TypeMirror> listInfo = tool().unwrap(List.class, basicInfo.originalReturnType());
    Optional<AbstractCollector> collector;
    Optional<TypeMirror> optionalInfo = tool().liftingUnwrap(basicInfo.originalReturnType());
    boolean optional = false;
    MapperType mapperType = null;
    if (optionalInfo.isPresent()) {
      mapExpr = findAutoMapper(optionalInfo.get());
      extractExpr = LiftedType.lift(basicInfo.originalReturnType(), tool()).extractExpr();
      if (mapExpr.isPresent()) {
        mapperType = MapperType.create(optionalInfo.get(), mapExpr.get());
        optional = true;
      }
      collector = Optional.empty();
    } else if (listInfo.isPresent()) {
      mapExpr = findAutoMapper(listInfo.get());
      extractExpr = p -> CodeBlock.of("$N", p);
      if (mapExpr.isPresent()) {
        mapperType = MapperType.create(listInfo.get(), mapExpr.get());
      }
      collector = Optional.of(new DefaultCollector(listInfo.get()));
    } else {
      collector = Optional.empty();
      if (mapExpr.isPresent()) {
        mapperType = MapperType.create(tool().box(basicInfo.originalReturnType()), mapExpr.get());
      }
      extractExpr = p -> CodeBlock.of("$N", p);
    }
    if (mapperType == null) {
      throw basicInfo.asValidationException("Unknown parameter type. Try defining a custom mapper or collector.");
    }
    TypeMirror constructorParamType = LiftedType.lift(basicInfo.originalReturnType(), tool()).liftedType();
    return Coercion.getCoercion(basicInfo, collector, mapperType, extractExpr, constructorParamType, optional);
  }


  private Coercion handleRepeatableAutoMapper() {
    AbstractCollector collectorInfo = collectorInfo();
    CodeBlock mapExpr = findAutoMapper(collectorInfo.inputType())
        .orElseThrow(() -> basicInfo.asValidationException("Unknown parameter type. Define a custom mapper."));
    MapperType mapperType = MapperType.create(collectorInfo.inputType(), mapExpr);
    Function<ParameterSpec, CodeBlock> extractExpr = p -> CodeBlock.of("$N", p);
    TypeMirror constructorParamType = basicInfo.originalReturnType();
    return Coercion.getCoercion(basicInfo, Optional.of(collectorInfo), mapperType, extractExpr, constructorParamType, false);
  }

  private Coercion handleRepeatableExplicitMapper(TypeElement mapperClass) {
    AbstractCollector collectorInfo = collectorInfo();
    ReferenceMapperType mapperType = new MapperClassValidator(basicInfo, collectorInfo.inputType(), mapperClass).checkReturnType();
    Function<ParameterSpec, CodeBlock> extractExpr = p -> CodeBlock.of("$N", p);
    TypeMirror constructorParamType = basicInfo.originalReturnType();
    return Coercion.getCoercion(basicInfo, Optional.of(collectorInfo), mapperType, extractExpr, constructorParamType, false);
  }

  private Optional<CodeBlock> findAutoMapper(TypeMirror innerType) {
    Optional<CodeBlock> mapExpr = AutoMapper.findAutoMapper(tool(), tool().box(innerType));
    if (mapExpr.isPresent()) {
      return mapExpr;
    }
    if (isEnumType(innerType)) {
      return Optional.of(CodeBlock.of("$T::valueOf", innerType));
    }
    return Optional.empty();
  }

  private boolean isEnumType(TypeMirror mirror) {
    List<? extends TypeMirror> supertypes = tool().getDirectSupertypes(mirror);
    if (supertypes.isEmpty()) {
      // not an enum
      return false;
    }
    TypeMirror superclass = supertypes.get(0);
    if (!tool().isSameErasure(superclass, tool().asType(Enum.class))) {
      // not an enum
      return false;
    }
    if (tool().isPrivateType(mirror)) {
      throw basicInfo.asValidationException("The enum may not be private.");
    }
    return true;
  }

  private AbstractCollector collectorInfo() {
    if (basicInfo.collectorClass().isPresent()) {
      return new CollectorClassValidator(basicInfo, basicInfo.collectorClass().get()).getCollectorInfo();
    }
    if (!tool().isSameErasure(basicInfo.returnType(), List.class)) {
      throw basicInfo.asValidationException("Either define a custom collector, or return List.");
    }
    List<? extends TypeMirror> typeParameters = tool().typeargs(basicInfo.returnType());
    if (typeParameters.isEmpty()) {
      throw basicInfo.asValidationException("Add a type parameter.");
    }
    return new DefaultCollector(typeParameters.get(0));
  }

  private TypeTool tool() {
    return basicInfo.tool();
  }
}
