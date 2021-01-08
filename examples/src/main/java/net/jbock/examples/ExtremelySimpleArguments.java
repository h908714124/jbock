package net.jbock.examples;

import net.jbock.Command;
import net.jbock.Param;

@Command
abstract class ExtremelySimpleArguments {

  @Param(1)
  abstract int hello();
}
