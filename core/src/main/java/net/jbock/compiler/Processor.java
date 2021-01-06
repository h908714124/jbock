package net.jbock.compiler;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import net.jbock.CollectorFor;
import net.jbock.Command;
import net.jbock.MapperFor;
import net.jbock.Option;
import net.jbock.Param;
import net.jbock.coerce.SuppliedClassValidator;
import net.jbock.compiler.view.GeneratedClass;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toSet;
import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.util.ElementFilter.methodsIn;
import static net.jbock.compiler.TypeTool.AS_DECLARED;

public final class Processor extends AbstractProcessor {

  private final boolean debug;

  public Processor() {
    this(false);
  }

  // visible for testing
  Processor(boolean debug) {
    this.debug = debug;
  }

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return Stream.of(Command.class, Option.class, Param.class)
        .map(Class::getCanonicalName)
        .collect(toSet());
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {
    TypeTool tool = new TypeTool(processingEnv.getElementUtils(), processingEnv.getTypeUtils());
    try {
      getAnnotatedMethods(env, annotations).forEach(method -> {
        checkEnclosingElementIsAnnotated(method);
        validateParameterMethod(method);
      });
    } catch (ValidationException e) {
      processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage(), e.about);
      return false;
    }
    if (annotations.stream().map(TypeElement::getQualifiedName)
        .noneMatch(name -> name.contentEquals(Command.class.getCanonicalName()))) {
      return false;
    }
    ElementFilter.typesIn(env.getElementsAnnotatedWith(Command.class)).forEach(sourceElement ->
        processSourceElement(sourceElement, tool));
    return false;
  }

  private void processSourceElement(TypeElement sourceElement, TypeTool tool) {
    ClassName generatedClass = generatedClass(sourceElement);
    try {
      validateSourceElement(tool, sourceElement);
      ClassName optionType = generatedClass.nestedClass("Option");
      List<Parameter> parameters = getParams(tool, sourceElement, optionType);
      if (parameters.isEmpty()) { // javapoet #739
        throw ValidationException.create(sourceElement, "Define at least one abstract method");
      }

      checkOnlyOnePositionalList(parameters);
      checkRankConsistentWithPosition(parameters);

      Context context = new Context(sourceElement, generatedClass, optionType, parameters);
      TypeSpec typeSpec = GeneratedClass.create(context).define();
      write(sourceElement, context.generatedClass(), typeSpec);
    } catch (ValidationException e) {
      processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage(), e.about);
    } catch (AssertionError error) {
      handleUnknownError(sourceElement, error);
    }
  }

  private static void checkOnlyOnePositionalList(List<Parameter> allParams) {
    allParams.stream().filter(p -> p.isRepeatable() && p.isPositional())
        .skip(1).findAny().ifPresent(p -> {
      throw p.validationError("There can only be one repeatable param.");
    });
  }

  private static void checkRankConsistentWithPosition(List<Parameter> allParams) {
    int currentOrdinal = -1;
    for (Parameter param : allParams) {
      OptionalInt order = param.positionalOrder();
      if (!order.isPresent()) {
        continue;
      }
      if (order.getAsInt() < currentOrdinal) {
        throw param.validationError("Bad position, expecting Optional < Required < Repeatable");
      }
      currentOrdinal = order.getAsInt();
    }
  }

  private void write(TypeElement sourceElement, ClassName generatedType, TypeSpec definedType) {
    JavaFile.Builder builder = JavaFile.builder(generatedType.packageName(), definedType);
    JavaFile javaFile = builder.build();
    try {
      JavaFileObject sourceFile = processingEnv.getFiler()
          .createSourceFile(generatedType.toString(),
              new TypeElement[]{sourceElement});
      try (Writer writer = sourceFile.openWriter()) {
        String sourceCode = javaFile.toString();
        writer.write(sourceCode);
        if (debug) {
          System.err.println("##############\n# Debug info #\n##############");
          System.err.println(sourceCode);
        }
      } catch (IOException e) {
        handleUnknownError(sourceElement, e);
      }
    } catch (IOException e) {
      handleUnknownError(sourceElement, e);
    }
  }

  private Map<String, ExecutableElement> getMappers(TypeTool tool, TypeElement sourceElement) {
    List<ExecutableElement> methods = methodsIn(sourceElement.getEnclosedElements()).stream()
        .filter(m -> validateMapperMethod(tool, m))
        .collect(Collectors.toList());
    Map<String, ExecutableElement> result = new HashMap<>();
    for (ExecutableElement method : methods) {
      String key = method.getAnnotation(MapperFor.class).value();
      ExecutableElement exist = result.get(key);
      if (exist != null) {
        throw ValidationException.create(method, "Duplicate mapper key: '" + key + "'");
      }
      result.put(key, method);
    }
    return result;
  }

  private Map<String, ExecutableElement> getCollectors(TypeTool tool, TypeElement sourceElement) {
    List<ExecutableElement> methods = methodsIn(sourceElement.getEnclosedElements()).stream()
        .filter(m -> validateCollectorMethod(tool, m))
        .collect(Collectors.toList());
    Map<String, ExecutableElement> result = new HashMap<>();
    for (ExecutableElement method : methods) {
      String key = method.getAnnotation(CollectorFor.class).value();
      ExecutableElement exist = result.get(key);
      if (exist != null) {
        throw ValidationException.create(method, "Duplicate collector key: '" + key + "'");
      }
      result.put(key, method);
    }
    return result;
  }

  private List<Parameter> getParams(TypeTool tool, TypeElement sourceElement, ClassName optionType) {
    Map<String, ExecutableElement> mappers = getMappers(tool, sourceElement);
    Map<String, ExecutableElement> collectors = getCollectors(tool, sourceElement);
    Methods methods = Methods.create(methodsIn(sourceElement.getEnclosedElements()).stream()
        .filter(Processor::validateParameterMethod)
        .collect(Collectors.toList()));
    List<Parameter> params = new ArrayList<>();
    for (int i = 0; i < methods.params().size(); i++) {
      params.add(Parameter.createPositionalParam(tool, params, methods.params().get(i), i,
          mappers.get(methods.params().get(i).getSimpleName().toString()),
          collectors.get(methods.params().get(i).getSimpleName().toString()),
          getDescription(methods.params().get(i)), optionType));
    }
    boolean anyMnemonics = methods.options().stream().anyMatch(method -> method.getAnnotation(Option.class).mnemonic() != ' ');
    for (ExecutableElement option : methods.options()) {
      params.add(Parameter.createNamedOption(anyMnemonics, tool, params, option,
          mappers.get(option.getSimpleName().toString()),
          collectors.get(option.getSimpleName().toString()),
          getDescription(option), optionType));
    }
    if (!sourceElement.getAnnotation(Command.class).helpDisabled()) {
      methods.options().forEach(this::checkHelp);
    }
    return params;
  }

  private void validateSourceElement(TypeTool tool, TypeElement sourceElement) {
    SuppliedClassValidator.commonChecks(sourceElement);
    if (!tool.isSameType(sourceElement.getSuperclass(), Object.class.getCanonicalName()) || !sourceElement.getInterfaces().isEmpty()) {
      throw ValidationException.create(sourceElement, "The model class may not implement or extend anything.");
    }
    if (!sourceElement.getTypeParameters().isEmpty()) {
      throw ValidationException.create(sourceElement, "The class cannot have type parameters.");
    }
  }

  private String[] getDescription(ExecutableElement method) {
    String docComment = processingEnv.getElementUtils().getDocComment(method);
    return docComment == null ? new String[0] : tokenizeJavadoc(docComment);
  }

  private static String[] tokenizeJavadoc(String docComment) {
    String[] tokens = docComment.trim().split("\\R", -1);
    List<String> result = new ArrayList<>(tokens.length);
    for (String t : tokens) {
      String token = t.trim();
      if (token.startsWith("@")) {
        return result.toArray(new String[0]);
      }
      if (!token.isEmpty()) {
        result.add(token);
      }
    }
    return result.toArray(new String[0]);
  }

  private void checkHelp(ExecutableElement option) {
    if ("help".equals(option.getAnnotation(Option.class).value())) {
      throw ValidationException.create(option, "'help' is reserved. Either disable the help feature or change the option name to something else.");
    }
  }

  private static boolean validateParameterMethod(ExecutableElement method) {
    if (!method.getModifiers().contains(ABSTRACT)) {
      if (method.getAnnotation(Param.class) != null || method.getAnnotation(Option.class) != null) {
        throw ValidationException.create(method, "The method must be abstract.");
      }
      return false;
    }
    if (!method.getParameters().isEmpty()) {
      throw ValidationException.create(method, "The method may not have any parameters.");
    }
    if (!method.getTypeParameters().isEmpty()) {
      throw ValidationException.create(method, "The method may not have any type parameters.");
    }
    if (!method.getThrownTypes().isEmpty()) {
      throw ValidationException.create(method, "The method may not declare any exceptions.");
    }
    if (method.getAnnotation(Param.class) == null && method.getAnnotation(Option.class) == null) {
      throw ValidationException.create(method, String.format("Annotate this method with either @%s or @%s",
          Option.class.getSimpleName(), Param.class.getSimpleName()));
    }
    if (method.getAnnotation(Param.class) != null && method.getAnnotation(Option.class) != null) {
      throw ValidationException.create(method, String.format("Use either @%s or @%s annotation, but not both",
          Option.class.getSimpleName(), Param.class.getSimpleName()));
    }
    if (isUnreachable(method.getReturnType())) {
      throw ValidationException.create(method, "Unreachable parameter type.");
    }
    return true;
  }

  private static boolean validateMapperMethod(TypeTool tool, ExecutableElement method) {
    if (method.getAnnotation(MapperFor.class) == null) {
      return false;
    }
    if (method.getAnnotation(CollectorFor.class) != null) {
      throw ValidationException.create(method, "The method can be either a mapper or collector, but not both.");
    }
    if (method.getModifiers().contains(ABSTRACT)) {
      throw ValidationException.create(method, "The mapper method must not be abstract.");
    }
    if (method.getModifiers().contains(PRIVATE)) {
      throw ValidationException.create(method, "The mapper method must not be private.");
    }
    if (method.getParameters().size() != 1 || !tool.isSameType(method.getParameters().get(0).asType(), String.class.getCanonicalName())) {
      throw ValidationException.create(method, "The mapper method must have a single parameter of type String.");
    }
    return true;
  }

  private static boolean validateCollectorMethod(TypeTool tool, ExecutableElement method) {
    if (method.getAnnotation(CollectorFor.class) == null) {
      return false;
    }
    if (method.getAnnotation(MapperFor.class) != null) {
      throw ValidationException.create(method, "The method can be either a mapper or collector, but not both.");
    }
    if (method.getModifiers().contains(ABSTRACT)) {
      throw ValidationException.create(method, "The collector method must not be abstract.");
    }
    if (method.getModifiers().contains(PRIVATE)) {
      throw ValidationException.create(method, "The collector method must not be private.");
    }
    if (!method.getParameters().isEmpty()) {
      throw ValidationException.create(method, "The collector method must be parameterless.");
    }
    if (!method.getParameters().isEmpty() || !tool.isSameErasure(method.getReturnType(), Collector.class.getCanonicalName())) {
      throw ValidationException.create(method, "The collector method must return an instance of java.util.stream.Collector.");
    }
    return true;
  }

  private static boolean isUnreachable(TypeMirror mirror) {
    TypeKind kind = mirror.getKind();
    if (kind != TypeKind.DECLARED) {
      return false;
    }
    DeclaredType declared = mirror.accept(AS_DECLARED, null);
    if (declared.asElement().getModifiers().contains(Modifier.PRIVATE)) {
      return true;
    }
    List<? extends TypeMirror> typeArguments = declared.getTypeArguments();
    for (TypeMirror typeArgument : typeArguments) {
      if (isUnreachable(typeArgument)) {
        return true;
      }
    }
    return false;
  }

  private List<ExecutableElement> getAnnotatedMethods(RoundEnvironment env, Set<? extends TypeElement> annotations) {
    List<ExecutableElement> methods = new ArrayList<>();
    for (Class<? extends Annotation> annotation : Arrays.asList(Option.class, Param.class)) {
      if (annotations.stream().map(TypeElement::getQualifiedName)
          .anyMatch(name -> name.contentEquals(annotation.getCanonicalName()))) {
        methods.addAll(methodsIn(env.getElementsAnnotatedWith(annotation)));
      }
    }
    return methods;
  }

  private static ClassName generatedClass(TypeElement sourceElement) {
    String name = String.join("_", ClassName.get(sourceElement).simpleNames()) + "_Parser";
    return ClassName.get(sourceElement).topLevelClassName().peerClass(name);
  }

  private void handleUnknownError(TypeElement sourceType, Throwable e) {
    String message = String.format("JBOCK: Unexpected error while processing %s: %s", sourceType, e.getMessage());
    e.printStackTrace(System.err);
    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, message, sourceType);
  }

  private void checkEnclosingElementIsAnnotated(ExecutableElement method) {
    Element p = method.getEnclosingElement();
    if (p.getAnnotation(Command.class) == null) {
      throw ValidationException.create(p, "missing @" + Command.class.getSimpleName() + " annotation");
    }
  }

  private boolean isSeparateClassFile(TypeElement sourceElement, TypeElement originatingElement) {
    if (sourceElement == originatingElement) {
      return false;
    }
    if (originatingElement.getNestingKind() == NestingKind.TOP_LEVEL) {
      return true;
    }
    Element current = originatingElement;
    while (true) {
      Element enclosingElement = current.getEnclosingElement();
      if (enclosingElement == null || enclosingElement.getKind() == ElementKind.PACKAGE) {
        return true;
      }
      if (enclosingElement == sourceElement) {
        return false;
      }
      current = enclosingElement;
    }
  }
}
