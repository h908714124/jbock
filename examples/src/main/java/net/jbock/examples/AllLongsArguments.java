package net.jbock.examples;

import net.jbock.CommandLineArguments;
import net.jbock.Parameter;
import net.jbock.PositionalParameter;

import java.util.List;
import java.util.Optional;

@CommandLineArguments
abstract class AllLongsArguments {

  @PositionalParameter(repeatable = true)
  abstract List<Long> positional();

  @Parameter(repeatable = true, shortName = 'i')
  abstract List<Long> listOfLongs();

  @Parameter(optional = true, longName = "opt")
  abstract Optional<Long> optionalLong();

  @Parameter(longName = "obj")
  abstract Long longObject();

  @Parameter(longName = "prim")
  abstract long primitiveLong();
}
