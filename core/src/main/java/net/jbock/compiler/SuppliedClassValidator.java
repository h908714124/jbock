package net.jbock.compiler;

import net.jbock.compiler.TypeTool;
import net.jbock.compiler.ValidationException;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import java.util.ArrayList;
import java.util.List;

class SuppliedClassValidator {

  static void commonChecks(TypeElement classToCheck) {
    if (classToCheck.getNestingKind().isNested() && classToCheck.getNestingKind() != NestingKind.MEMBER) {
      throw ValidationException.create(classToCheck, "Use a top level class or static inner class.");
    }
    if (classToCheck.getNestingKind().isNested() &&
        !classToCheck.getModifiers().contains(Modifier.STATIC)) {
      throw ValidationException.create(classToCheck, "The nested class must be static.");
    }
    if (classToCheck.getModifiers().contains(Modifier.PRIVATE)) {
      throw ValidationException.create(classToCheck, "The class may not be private.");
    }
    if (classToCheck.getKind() == ElementKind.INTERFACE) {
      throw ValidationException.create(classToCheck, "Use a class, not an interface.");
    }
    getEnclosingElements(classToCheck).forEach(element -> {
      if (element.getModifiers().contains(Modifier.PRIVATE)) {
        throw ValidationException.create(element, "The class may not not be private.");
      }
    });
    if (!hasDefaultConstructor(classToCheck)) {
      throw ValidationException.create(classToCheck, "The class must have a default constructor");
    }
  }

  private static List<TypeElement> getEnclosingElements(TypeElement sourceElement) {
    List<TypeElement> result = new ArrayList<>();
    TypeElement current = sourceElement;
    result.add(current);
    while (current.getNestingKind() == NestingKind.MEMBER) {
      Element enclosingElement = current.getEnclosingElement();
      if (enclosingElement.getKind() != ElementKind.CLASS) {
        return result;
      }
      current = TypeTool.asTypeElement(enclosingElement);
      result.add(current);
    }
    return result;
  }

  private static boolean hasDefaultConstructor(TypeElement classToCheck) {
    List<ExecutableElement> constructors = ElementFilter.constructorsIn(classToCheck.getEnclosedElements());
    if (constructors.isEmpty()) {
      return true;
    }
    for (ExecutableElement constructor : constructors) {
      if (!constructor.getParameters().isEmpty()) {
        continue;
      }
      if (constructor.getModifiers().contains(Modifier.PRIVATE)) {
        return false;
      }
      return constructor.getThrownTypes().isEmpty();
    }
    return false;
  }
}
