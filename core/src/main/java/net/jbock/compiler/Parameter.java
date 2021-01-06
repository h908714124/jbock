package net.jbock.compiler;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import net.jbock.Option;
import net.jbock.coerce.Coercion;
import net.jbock.coerce.CoercionProvider;
import net.jbock.coerce.FlagCoercion;
import net.jbock.coerce.Skew;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.Character.isWhitespace;
import static net.jbock.compiler.Constants.ALLOWED_MODIFIERS;

/**
 * This class represents either an {@link Option} or a {@link net.jbock.Param}.
 */
public final class Parameter {

  // null iff this is a param
  private final String optionName;

  // ' ' if this is a param
  private final char mnemonic;

  private final ExecutableElement sourceMethod;

  private final String bundleKey;

  private final String sample;

  private final List<String> names;

  private final Coercion coercion;

  private final List<String> description;

  private final Integer positionalIndex;

  private static ParamName findParamName(List<Parameter> alreadyCreated, ExecutableElement sourceMethod) {
    String methodName = sourceMethod.getSimpleName().toString();
    ParamName result = ParamName.create(methodName);
    for (Parameter param : alreadyCreated) {
      if (param.paramName().enumConstant().equals(result.enumConstant())) {
        return result.append(Integer.toString(alreadyCreated.size()));
      }
    }
    return result;
  }

  private static void checkBundleKey(String bundleKey, List<Parameter> alreadyCreated, ExecutableElement sourceMethod) {
    if (bundleKey.isEmpty()) {
      return;
    }
    if (bundleKey.matches(".*\\s+.*")) {
      throw ValidationException.create(sourceMethod, "The bundle key may not contain whitespace characters.");
    }
    for (Parameter param : alreadyCreated) {
      if (bundleKey.equals(param.bundleKey)) {
        throw ValidationException.create(sourceMethod, "Duplicate bundle key.");
      }
    }
  }

  private Parameter(char mnemonic, String optionName, ExecutableElement sourceMethod, String bundleKey, String sample,
                    List<String> names, Coercion coercion, List<String> description, Integer positionalIndex) {
    this.mnemonic = mnemonic;
    this.optionName = optionName;
    this.sourceMethod = sourceMethod;
    this.bundleKey = bundleKey;
    this.sample = sample;
    this.names = names;
    this.coercion = coercion;
    this.description = description;
    this.positionalIndex = positionalIndex;
  }

  public Coercion coercion() {
    return coercion;
  }

  static Parameter createPositionalParam(TypeTool tool, List<Parameter> alreadyCreated, ExecutableElement sourceMethod,
                                         int positionalIndex, ExecutableElement mapper, ExecutableElement collector,
                                         String[] description, ClassName optionType) {
    net.jbock.Param parameter = sourceMethod.getAnnotation(net.jbock.Param.class);
    ParamName name = findParamName(alreadyCreated, sourceMethod);
    Coercion coercion = CoercionProvider.nonFlagCoercion(sourceMethod, name, mapper, collector, optionType, tool);
    checkBundleKey(parameter.bundleKey(), alreadyCreated, sourceMethod);
    return new Parameter(' ', null, sourceMethod, parameter.bundleKey(), name.snake().toLowerCase(Locale.US),
        Collections.emptyList(), coercion, Arrays.asList(description), positionalIndex);
  }

  static Parameter createNamedOption(boolean anyMnemonics, TypeTool tool, List<Parameter> alreadyCreated,
                                     ExecutableElement sourceMethod, ExecutableElement mapper,
                                     ExecutableElement collector, String[] description, ClassName optionType) {
    String optionName = optionName(alreadyCreated, sourceMethod);
    char mnemonic = mnemonic(alreadyCreated, sourceMethod);
    Option option = sourceMethod.getAnnotation(Option.class);
    ParamName name = findParamName(alreadyCreated, sourceMethod);
    boolean flag = isInferredFlag(mapper, collector, sourceMethod.getReturnType(), tool);
    Coercion coercion = flag ?
        new FlagCoercion(name, sourceMethod) :
        CoercionProvider.nonFlagCoercion(sourceMethod, name, mapper, collector, optionType, tool);
    String bundleKey = option.bundleKey().isEmpty() ? option.value() : option.bundleKey();
    checkBundleKey(bundleKey, alreadyCreated, sourceMethod);
    List<String> names = names(optionName, mnemonic);
    return new Parameter(mnemonic, optionName, sourceMethod, bundleKey, sample(flag, name, names, anyMnemonics),
        names, coercion, Arrays.asList(description), null);
  }

  private static boolean isInferredFlag(ExecutableElement mapperClass, ExecutableElement collectorClass, TypeMirror mirror, TypeTool tool) {
    if (mapperClass != null || collectorClass != null) {
      // not a flag
      return false;
    }
    return mirror.getKind() == TypeKind.BOOLEAN || tool.isSameType(mirror, Boolean.class.getCanonicalName());
  }

  private static Character mnemonic(List<Parameter> parameters, ExecutableElement sourceMethod) {
    Option option = sourceMethod.getAnnotation(Option.class);
    if (option == null || option.mnemonic() == ' ') {
      return ' ';
    }
    for (Parameter p : parameters) {
      if (option.mnemonic() == p.mnemonic) {
        throw ValidationException.create(sourceMethod, "Duplicate mnemonic");
      }
    }
    return checkMnemonic(sourceMethod, option.mnemonic());
  }

  private static String optionName(List<Parameter> params, ExecutableElement sourceMethod) {
    Option option = sourceMethod.getAnnotation(Option.class);
    if (option == null) {
      return null;
    }
    if (Objects.toString(option.value(), "").isEmpty()) {
      throw ValidationException.create(sourceMethod, "The name may not be empty");
    }
    for (Parameter p : params) {
      if (option.value().equals(p.optionName)) {
        throw ValidationException.create(sourceMethod, "Duplicate option name: " + option.value());
      }
    }
    return checkName(sourceMethod, option.value());
  }

  private static char checkMnemonic(ExecutableElement sourceMethod, char mnemonic) {
    if (mnemonic != ' ') {
      checkName(sourceMethod, Character.toString(mnemonic));
    }
    return mnemonic;
  }

  private static String checkName(ExecutableElement sourceMethod, String name) {
    if (Objects.toString(name, "").isEmpty()) {
      throw ValidationException.create(sourceMethod, "The name may not be empty");
    }
    if (name.charAt(0) == '-') {
      throw ValidationException.create(sourceMethod, "The name may not start with '-'");
    }
    for (int i = 0; i < name.length(); i++) {
      char c = name.charAt(i);
      if (isWhitespace(c)) {
        throw ValidationException.create(sourceMethod, "The name may not contain whitespace characters");
      }
      if (c == '=') {
        throw ValidationException.create(sourceMethod, "The name may not contain '='");
      }
    }
    return name;
  }

  public List<String> description() {
    return description;
  }

  public String methodName() {
    return sourceMethod.getSimpleName().toString();
  }

  public TypeName returnType() {
    return TypeName.get(sourceMethod.getReturnType());
  }

  public String enumConstant() {
    return paramName().enumConstant();
  }

  public boolean isPositional() {
    return positionalIndex != null;
  }

  public OptionalInt positionalIndex() {
    return positionalIndex != null ? OptionalInt.of(positionalIndex) : OptionalInt.empty();
  }

  public boolean isRequired() {
    return coercion.getSkew() == Skew.REQUIRED;
  }

  public boolean isRepeatable() {
    return coercion.getSkew() == Skew.REPEATABLE;
  }

  public boolean isOptional() {
    return coercion.getSkew() == Skew.OPTIONAL;
  }

  public boolean isFlag() {
    return coercion.getSkew() == Skew.FLAG;
  }

  public Optional<String> bundleKey() {
    return bundleKey.isEmpty() ? Optional.empty() : Optional.of(bundleKey);
  }

  OptionalInt positionalOrder() {
    if (positionalIndex == null) {
      return OptionalInt.empty();
    }
    return OptionalInt.of(isRepeatable() ? 2 : isOptional() ? 1 : 0);
  }

  public ParamName paramName() {
    return coercion.paramName();
  }

  ValidationException validationError(String message) {
    return ValidationException.create(sourceMethod, message);
  }

  public Set<Modifier> getAccessModifiers() {
    return sourceMethod.getModifiers().stream()
        .filter(ALLOWED_MODIFIERS::contains)
        .collect(Collectors.toSet());
  }

  public List<String> names() {
    return names;
  }

  static List<String> names(String optionName, char mnemonic) {
    if (optionName != null && mnemonic == ' ') {
      return Collections.singletonList("--" + optionName);
    } else if (optionName == null && mnemonic != ' ') {
      return Collections.singletonList("-" + mnemonic);
    } else if (optionName == null) {
      return Collections.emptyList();
    }
    return Arrays.asList("-" + mnemonic, "--" + optionName);
  }

  public String sample() {
    return sample;
  }

  private static String sample(boolean flag, ParamName name, List<String> names, boolean anyMnemonics) {
    if (names.isEmpty() || names.size() >= 3) {
      throw new AssertionError();
    }
    String argname = flag ? "" : ' ' + name.enumConstant();
    if (names.size() == 1) {
      // Note: The padding has the same length as the string "-f, "
      return (anyMnemonics ? "    " : "") + names.get(0) + argname;
    }
    return names.get(0) + ", " + names.get(1) + argname;
  }
}
