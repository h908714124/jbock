package net.jbock;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>This annotation is used by the jbock annotation processor.</p>
 *
 * <ul>
 * <li>The annotated type must be an abstract class.</li>
 * <li>There must be at least one abstract method.</li>
 * <li>Each abstract method must have an empty argument list.</li>
 * <li>None of the abstract methods may declare an exception.</li>
 * <li>Each abstract method must either be annotated with {@link Parameter}
 * or {@link PositionalParameter}, but not both.</li>
 * <li>The annotated class may not implement anything and may not extend anything other than
 * {@link java.lang.Object Object}.</li>
 * </ul>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface CommandLineArguments {

  /**
   * <p>The name of the final executable program.
   * If the java program is usually invoked from a wrapper script,
   * then this should be the file name of that script.</p>
   *
   * <p>The program name is printed when the user passes the
   * {@code --help} parameter, in the {@code NAME} section of the usage information.
   * By default, the short name of the annotated java class is used as the program name.
   * If that class is an inner class,
   * then the short name of its enclosing class is the default program name</p>
   */
  String programName() default "";

  /**
   * A short, single-sentence summary of the program. It is printed when the user passes the
   * {@code --help} parameter.
   */
  String missionStatement() default "";

  /**
   * <p>If {@code true}, a special parameter {@code --help} will be understood by the parser,
   * but only if it is the very first argument.
   * In this case, it is an error to assign the long name "help" to any other parameter.</p>
   */
  boolean addHelp() default true;

  /**
   * <p>True if an isolated double dash "--" should stop option parsing.
   * The remaining tokens will then be treated as positional, regardless of their shape.</p>
   */
  boolean allowEscape() default true;

  /**
   * <p>True if unknown tokens that start with a dash should be permissible.
   * These tokens will then be treated as positional.
   * Otherwise these tokens are treated as bad input, and parsing fails.</p>
   *
   * <p>Note that <em>any</em> unknown token is considered bad input,
   * if no positional parameters are defined. See {@link PositionalParameter}.</p>
   *
   * <p>Note that setting {@link #allowEscape()} (and defining a positional list) makes the double dash "--" a known token.</p>
   */
  boolean strict() default true;
}