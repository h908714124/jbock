package net.jbock;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * Marker annotation for named options.
 * The annotated method must be abstract
 * and have an empty argument list.
 */
@Target(METHOD)
@Retention(SOURCE)
public @interface Option {

  /**
   * A unique &quot;gnu style&quot; long name.
   * It must not begin with a dash character.
   *
   * @return a nonempty string
   */
  String value();

  /**
   * An optional mnemonic.
   * The space character is reserved for "none".
   *
   * @return a mnemonic
   */
  char mnemonic() default ' ';

  /**
   * The key that is used to find the parameter
   * description in the i18 resource bundle for the online help.
   * If no bundleKey is defined, the method name is used as bundle key instead.
   * If no bundle is supplied at runtime,
   * or a bundle is supplied but doesn't contain the bundle key,
   * then the {@code abstract} method's javadoc is used as description.
   *
   * @return an optional bundle key
   */
  String bundleKey() default "";
}
