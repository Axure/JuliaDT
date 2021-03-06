package com.juliacomputing.jldt.eclipse.ui.console;

import com.juliacomputing.jldt.eclipse.ui.console.Result.Status;

import org.eclipse.dltk.console.*;

import java.io.*;
import java.util.List;

public class JuliaConsoleInterpreter implements IScriptInterpreter {

  private static final String REPL_WRAPPER = "script/repl-wrapper.jl";
  private static final String ENCODING = "UTF8";
  private static final String PLOT_PLACEHOLDER = "<plot>";
  private static final String EOX = "<<<<eox>>>>";

  private final Process process;
  private final BufferedWriter writer;
  private final BufferedReader reader;
  private StringBuilder block = new StringBuilder();
  private int state = IScriptConsoleInterpreter.WAIT_NEW_COMMAND;

  public JuliaConsoleInterpreter(final String path, String workingDirectory) {
    try {
      final ProcessBuilder builder = new ProcessBuilder(path);
      if (workingDirectory != null)
        builder.directory(new File(workingDirectory));
      process = builder.start();
      final OutputStream outputStream = process.getOutputStream();
      writer = new BufferedWriter(new OutputStreamWriter(outputStream, ENCODING));
      final InputStream inputStream = process.getInputStream();
      reader = new BufferedReader(new InputStreamReader(inputStream, ENCODING));
      final String script = Util.read(REPL_WRAPPER);
      writer.write(script);
      writer.newLine();
      writer.flush();
      reader.readLine();
    }
    catch (IOException e) {
      JuliaConsolePlugin.log(e);
      throw new RuntimeException(e);
    }

  }

  @Override
  public void addInitialListenerOperation(Runnable runnable) {

  }

  @Override
  public InputStream getInitialOutputStream() {
    return process.getInputStream();
  }

  @Override
  public boolean isValid() {
    return true;
  }

  @Override
  public void consoleConnected(IScriptConsoleIO iScriptConsoleIO) {
  }

  @Override
  public IScriptExecResult exec(String command) throws IOException {
    block.append(command);
    block.append(System.lineSeparator());
    final String message = String.format("EclipseREPL.execute(\"%s\")",
        Util.escapedString(block.toString(), false));
    final Result result = execute(message);
    if (result == null)
      return new ScriptExecResult("undefined", true);
    return new ScriptExecResult(result.getValue(), result.getStatus() == Status.error);
  }

  private Result execute(String statement) throws IOException {
    writer.write(statement);
    writer.newLine();
    writer.flush();
    StringBuilder response = new StringBuilder();
    String line = reader.readLine();
    while (line != null && !line.contains("<<<<")) {
      response.append(line);
      response.append(System.lineSeparator());
      line = reader.readLine();
    }
    final Status status = Status.valueOf(line.substring(4, line.length() - 4));
    JuliaConsolePlugin.log(status.toString());
    line = reader.readLine();
    final String mimeType = line.substring(4, line.length() - 4);
    JuliaConsolePlugin.log(mimeType);
    if (mimeType.equals("text/html") && status != Status.error) {
      Util.publish(response.toString(), JuliaConsolePlugin.JULIA_PLOT_TOPIC);
      response = new StringBuilder();
      response.append(PLOT_PLACEHOLDER);
      response.append(System.lineSeparator());
    }
    line = reader.readLine().trim();
    while (!line.equals(EOX)) {
      response.append(line);
      response.append(System.lineSeparator());
      line = reader.readLine().trim();
    }
    response.append(System.lineSeparator());
    switch (status) {
      case incomplete:
        state = IScriptConsoleInterpreter.WAIT_USER_INPUT;
        return null;
      default:
        state = IScriptConsoleInterpreter.WAIT_NEW_COMMAND;
        block = new StringBuilder();
        return new Result(status, response.toString());
    }
  }

  @Override
  public int getState() {
    return state;
  }

  @Override
  public List<?> getCompletions(String s, int i) throws IOException {
    return null;
  }

  @Override
  public String getDescription(String s, int i) throws IOException {
    return null;
  }

  @Override
  public String[] getNames(String s) throws IOException {
    return new String[0];
  }

  @Override
  public void close() throws IOException {
    process.destroy();
  }

}
