import java.io.IOException;
import java.io.PrintStream;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.TextField;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;
import se.krka.kahlua.luaj.compiler.LuaCompiler;
import se.krka.kahlua.vm.LuaClosure;
import se.krka.kahlua.vm.LuaState;


public class KahluaInterpreter extends MIDlet implements CommandListener, Runnable {
	private TextField input;
	private Command run;
	private Command exit;
	private Form form;
	private LuaState state;
	private Command clear;
	private boolean alive = true;

	private Object notifyObject = new Object();
	private String source;

	public KahluaInterpreter() {
		run = new Command("Run", Command.OK, 1);
		clear = new Command("Clear", Command.OK, 90);
		exit = new Command("Exit", Command.EXIT, 100);

		form = new Form("Kahlua Interpreter");
		input = new TextField("Input", "", 200, TextField.ANY);

		PrintStream printStream = new PrintStream(System.out) {
			public void println(String s) {
				form.append(s + "\n");
			}
		};
		state = new LuaState(printStream);
		LuaCompiler.register(state);

		form.setCommandListener(this);
		form.addCommand(exit);
		form.addCommand(run);
		form.addCommand(clear);

		form.append(input);

		new Thread(this).start();

		Display.getDisplay(this).setCurrent(form);
	}

	protected void destroyApp(boolean arg0) throws MIDletStateChangeException {
		alive = false;
		synchronized (notifyObject) {
			notifyObject.notifyAll();
		}
	}

	protected void pauseApp() {
	}
	protected void startApp() throws MIDletStateChangeException {
	}

	public void commandAction(Command command, Displayable display) {
		if (command == exit) {
			notifyDestroyed();
			return;
		}
		if (command == run) {
			form.removeCommand(run);
			clearOutput();
			source = input.getString();
			if (source.startsWith("=")) {
				source = "return " + source.substring(1);
			}
			synchronized (notifyObject) {
				notifyObject.notifyAll();
			}
			return;
		}

		if (command == clear) {
			input.setString("");
			return;
		}
	}

	private void clearOutput() {
		for (int i = form.size() - 1; i >= 1; i--) {
			form.delete(i);
		}
	}

	public void run() {
		while (alive) {
			synchronized (notifyObject) {
				try {
					notifyObject.wait();
				} catch (InterruptedException e) {
					
				}
			}
			if (source != null) {
				try {
					state.out.println("Compiling...");
					LuaClosure closure = LuaCompiler.loadstring(source, "stdin", state.getEnvironment());
					source = null;
					clearOutput();
					state.out.println("Running...");
					Object[] result = state.pcall(closure);

					if (result != null) {
						if (result[0] == Boolean.TRUE) {
							StringBuffer line = new StringBuffer();
							for (int i = 1; i < result.length; i++) {
								if (i > 1) {
									line.append(", ");
								}
								line.append(result[i].toString());
							}
							state.out.println(line.toString());
						} else {
							if (result[1] instanceof String) {
								state.out.println((String) result[1]);
							}
							if (result[2] instanceof String) {
								state.out.println((String) result[2]);
							}
						}
					}
				} catch (RuntimeException e) {
					state.out.println(e.getMessage());
				} catch (IOException e) {
					state.out.println(e.getMessage());
				}
				// delete the "running..." text
				form.delete(1);
				form.addCommand(run);
			}
		}
	}
}