package in.abhishekmishra.picoturtle;

import java.util.ArrayList;
import java.util.List;

public class TurtleCommand {

	public String cmd;
	public List<Object> args;
	
	public TurtleCommand(String commandName) {
		this.cmd = commandName;
		args = new ArrayList<Object>();
	}
}
