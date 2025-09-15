package implementation;

import interpreter.PrintScriptFormatter;
import interpreter.PrintScriptInterpreter;
import interpreter.PrintScriptLinter;

import java.io.BufferedInputStream;
import java.util.Arrays;

public class CustomImplementationFactory implements PrintScriptFactory {

  @Override
  public PrintScriptInterpreter interpreter() {
    return new RunnerInterpreterAdapter();
  }

  @Override
  public PrintScriptFormatter formatter() {
    return new RunnerFormatterAdapter();
  }

  @Override
  public PrintScriptLinter linter() {
   return new RunnerLinterAdapter();
  }
}