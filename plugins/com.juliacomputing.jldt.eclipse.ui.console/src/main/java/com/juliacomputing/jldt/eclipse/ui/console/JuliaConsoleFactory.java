package com.juliacomputing.jldt.eclipse.ui.console;

import org.eclipse.dltk.console.ScriptConsolePrompt;
import org.eclipse.dltk.console.ui.ScriptConsoleFactoryBase;
import org.eclipse.dltk.core.environment.IFileHandle;
import org.eclipse.dltk.launching.IInterpreterInstall;
import org.eclipse.dltk.launching.IInterpreterInstallType;
import org.eclipse.dltk.launching.ScriptRuntime;

public class JuliaConsoleFactory extends ScriptConsoleFactoryBase {

  private static final String CONSOLE_NAME = "Julia REPL";
  private static final String JULIA_INSTALLATION_TYPE = "org.eclipse.dltk.internal.debug.ui.launcher.JuliaInterpreterInstallationType";
  private static final String PROMPT = "julia>";
  private static final String APPEND_COMMAND = "[";
  private final String workingDirectory;

  public JuliaConsoleFactory() {
    this(null);
  }

  public JuliaConsoleFactory(String workingDirectory) {
    this.workingDirectory = workingDirectory;
  }

  public JuliaScriptConsole createConsoleInstance() {
    final IInterpreterInstallType installType = ScriptRuntime
        .getInterpreterInstallType(JULIA_INSTALLATION_TYPE);
    final IInterpreterInstall[] interpreterInstalls = installType.getInterpreterInstalls();
    final IFileHandle location = interpreterInstalls[0].getInstallLocation();
    final JuliaScriptConsole console = new JuliaScriptConsole(CONSOLE_NAME, "interactive");
    console.setInterpreter(new JuliaConsoleInterpreter(location.getCanonicalPath(),
        workingDirectory));
    console.setPrompt(new ScriptConsolePrompt(PROMPT, APPEND_COMMAND));
    return console;
  }

  public JuliaScriptConsole newConsole() {
    final JuliaScriptConsole console = createConsoleInstance();
    registerAndOpenConsole(console);
    return console;
  }

}
