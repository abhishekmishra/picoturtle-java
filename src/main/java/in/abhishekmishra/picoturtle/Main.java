package in.abhishekmishra.picoturtle;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import com.google.gson.Gson;

public class Main {
	public static void main(String[] args) throws IOException, URISyntaxException {
		Gson gson = new Gson();
		Turtle t = new Turtle(null, null, -1, true, 1);
		System.out.println("Created Turtle with name -> " + t.name);
		t.canvas_size(250, 250);
		t.penup();
		t.setpos(125, 125);
		t.pendown();
		t.pencolour(128, 128, 0);
		for (int i = 0; i < 4; i++) {
			t.forward(100);
			t.right(90);
		}
		t.export_img("java-picoturtle-test.png");
		TurtleState s = t.stop();
		System.out.println(gson.toJson(s, TurtleState.class));
		System.out.println(s.colour.r);
		if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
			Desktop.getDesktop().browse(new URI(t.browserURL()));
		}
	}
}
