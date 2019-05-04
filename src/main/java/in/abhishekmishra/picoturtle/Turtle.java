package in.abhishekmishra.picoturtle;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.gson.Gson;

public class Turtle {
	public String name;
	private String turtleUrl;
	private boolean bulk;
	private int bulkLimit;
	private List<TurtleCommand> commands;
	private Gson gson = new Gson();

	public static Turtle CreateTurtle(String[] args) {
		String turtle_name = null;
		String host = "127.0.0.1";
		int port = 3000;
		boolean bulk = true;
		int bulkLimit = 100;
		boolean show_help = false;

		Options options = new Options();
		options.addOption(new Option("n", "name", true, "the {NAME} of the turtle if already created."));
		options.addOption(new Option("p", "port", true, "the {PORT} for the turtle server to connect to."));
		options.addOption(new Option("h", "help", false, "show this message and exit."));

		CommandLineParser parser = new DefaultParser();
		CommandLine cmd;
		try {
			cmd = parser.parse(options, args);
			if (cmd.hasOption("n")) {
				turtle_name = cmd.getOptionValue("n");
			}
			if (cmd.hasOption("p")) {
				port = Integer.parseInt(cmd.getOptionValue("p"));
			}
			if (cmd.hasOption("h")) {
				show_help = true;
			}

			if (show_help) {
				ShowHelp();
				return null;
			}

			return new Turtle(turtle_name, host, port, bulk, bulkLimit);
		} catch (ParseException e) {
			System.out.println("Error parsing command line -> " + e.getMessage());
			return null;
		}

	}

	static void ShowHelp() {
		System.out.println("Usage: picoturtle-java [OPTIONS]+");
		System.out.println("");
		System.out.println("Options:");
		System.out.println("-p|--port port of the turtle server");
		System.out.println("-n|--name name of the turtle");
	}

	public Turtle(String name, String host, int port, boolean bulk, int bulkLimit) {
		String h;
		int p;
		this.name = name;
		if (host == null) {
			h = "127.0.0.1";
		} else {
			h = host;
		}
		if (port <= 0) {
			p = 3000;
		} else {
			p = port;
		}
		this.turtleUrl = "http://" + h + ":" + p;
		this.bulk = bulk;
		if (bulkLimit > 0) {
			this.bulkLimit = bulkLimit;
		} else {
			this.bulkLimit = 100;
		}
		this.commands = new ArrayList<TurtleCommand>();
		if (this.name == null) {
			this.init(250, 250);
		}
	}

	private TurtleState turtleRequest(String cmd, List<Pair<String, Object>> args, boolean is_obj) {
		if (this.bulk == true && (cmd != "create")) {
			TurtleCommand command = new TurtleCommand(cmd);
			if (args != null && args.size() > 0) {
				if (is_obj) {
					HashMap<String, Object> arg_obj = new HashMap<String, Object>();
					for (int i = 0; i < args.size(); i++) {
						Pair<String, Object> kvpair = args.get(i);
						arg_obj.put(kvpair.first, kvpair.second);
					}
					command.args.add(arg_obj);
				} else {
					for (int i = 0; i < args.size(); i++) {
						Pair<String, Object> kvpair = args.get(i);
						command.args.add(kvpair.second);
					}
				}
			}
			this.commands.add(command);
			if ((this.commands.size() >= this.bulkLimit) || (cmd == "stop") || (cmd == "state")) {
				HttpRequestFactory requestFactory = new NetHttpTransport().createRequestFactory();
				//System.out.println("Draining commands " + this.commands.size());
				String json = gson.toJson(this.commands);
				//System.out.println(json);
				GenericUrl url = new GenericUrl(this.turtleUrl + "/turtle/" + this.name + "/commands");
				//System.out.println(url);
				try {
					HttpRequest request = requestFactory.buildPostRequest(url,
							new ByteArrayContent("application/json", json.getBytes()));
					HttpResponse response = request.execute();
					InputStream is = response.getContent();
					BufferedInputStream bis = new BufferedInputStream(is);
					ByteArrayOutputStream buf = new ByteArrayOutputStream();
					int result = bis.read();
					while (result != -1) {
						buf.write((byte) result);
						result = bis.read();
					}
					// StandardCharsets.UTF_8.name() > JDK 7
					String responseText = buf.toString("UTF-8");
					// System.out.println(responseText);

					TurtleState t = gson.fromJson(responseText, TurtleState.class);
					this.commands.clear();
					return t;
				} catch (Exception e) {
					System.out.println("Error calling Turtle Server, Message : " + e.getMessage());
				}
			}
		} else {
			String request_url = "/turtle/";
			if (this.name != null) {
				request_url += this.name;
				request_url += "/";
			}
			request_url += cmd;
			if (args != null) {
				request_url += "?";
				int i = 0;
				for (int j = 0; j < args.size(); j++) {
					Pair<String, Object> kvpair = args.get(j);
					if (i > 0) {
						request_url += "&";
					}
					request_url += kvpair.first;
					request_url += "=";
					request_url += kvpair.second.toString();
					i++;
				}
			}
			try {
				// System.out.println(request_url);
				HttpRequestFactory requestFactory = new NetHttpTransport().createRequestFactory();
				HttpRequest request = requestFactory.buildGetRequest(new GenericUrl(this.turtleUrl + request_url));
				HttpResponse response = request.execute();
				InputStream is = response.getContent();
				BufferedInputStream bis = new BufferedInputStream(is);
				ByteArrayOutputStream buf = new ByteArrayOutputStream();
				int result = bis.read();
				while (result != -1) {
					buf.write((byte) result);
					result = bis.read();
				}
				// StandardCharsets.UTF_8.name() > JDK 7
				String responseText = buf.toString("UTF-8");
				// System.out.println(responseText);

				TurtleState t = gson.fromJson(responseText, TurtleState.class);
				return t;
			} catch (Exception e) {
				System.out.println("Error calling Turtle Server, Message : " + e.getMessage());
			}
		}
		return null;
	}

	public String browserURL() {
		return turtleUrl + "/index.html?details=0&list=0&name=" + name;
	}

	public TurtleState init(double x, double y) {
		commands = new ArrayList<TurtleCommand>();
		List<Pair<String, Object>> args = new ArrayList<Pair<String, Object>>();
		args.add(new Pair<String, Object>("x", x));
		args.add(new Pair<String, Object>("y", y));
		TurtleState t = turtleRequest("create", args, false);
		this.name = t.name;
		return t;
	}

	public TurtleState penup() {
		return turtleRequest("penup", null, false);
	}

	public TurtleState pendown() {
		return turtleRequest("pendown", null, false);
	}

	public TurtleState penwidth(double w) {
		List<Pair<String, Object>> args = new ArrayList<Pair<String, Object>>();
		args.add(new Pair<String, Object>("w", w));

		return turtleRequest("penwidth", args, false);
	}

	public TurtleState stop() {
		return turtleRequest("stop", null, false);
	}

	public TurtleState state() {
		return turtleRequest("state", null, false);
	}

	public TurtleState home() {
		return turtleRequest("home", null, false);
	}

	public TurtleState clear() {
		return turtleRequest("clear", null, false);
	}

	public TurtleState forward(double d) {
		List<Pair<String, Object>> args = new ArrayList<Pair<String, Object>>();
		args.add(new Pair<String, Object>("d", d));

		return turtleRequest("forward", args, false);
	}

	public TurtleState back(double d) {
		List<Pair<String, Object>> args = new ArrayList<Pair<String, Object>>();
		args.add(new Pair<String, Object>("d", d));

		return turtleRequest("back", args, false);
	}

	public TurtleState setpos(double x, double y) {
		List<Pair<String, Object>> args = new ArrayList<Pair<String, Object>>();
		args.add(new Pair<String, Object>("x", x));
		args.add(new Pair<String, Object>("y", y));

		return turtleRequest("goto", args, false);
	}

	public TurtleState setx(double x) {
		List<Pair<String, Object>> args = new ArrayList<Pair<String, Object>>();
		args.add(new Pair<String, Object>("x", x));

		return turtleRequest("setx", args, false);
	}

	public TurtleState sety(double y) {
		List<Pair<String, Object>> args = new ArrayList<Pair<String, Object>>();
		args.add(new Pair<String, Object>("y", y));

		return turtleRequest("sety", args, false);
	}

	public TurtleState left(double a) {
		List<Pair<String, Object>> args = new ArrayList<Pair<String, Object>>();
		args.add(new Pair<String, Object>("a", a));

		return turtleRequest("left", args, false);
	}

	public TurtleState right(double a) {
		List<Pair<String, Object>> args = new ArrayList<Pair<String, Object>>();
		args.add(new Pair<String, Object>("a", a));

		return turtleRequest("right", args, false);
	}

	public TurtleState heading(double a) {
		List<Pair<String, Object>> args = new ArrayList<Pair<String, Object>>();
		args.add(new Pair<String, Object>("a", a));

		return turtleRequest("heading", args, false);
	}

	public TurtleState font(String f) {
		List<Pair<String, Object>> args = new ArrayList<Pair<String, Object>>();
		args.add(new Pair<String, Object>("f", f));

		return turtleRequest("font", args, false);
	}

	public TurtleState filltext(String text) {
		List<Pair<String, Object>> args = new ArrayList<Pair<String, Object>>();
		args.add(new Pair<String, Object>("t", text));

		return turtleRequest("filltext", args, false);
	}

	public TurtleState stroketext(String text) {
		List<Pair<String, Object>> args = new ArrayList<Pair<String, Object>>();
		args.add(new Pair<String, Object>("t", text));

		return turtleRequest("stroketext", args, false);
	}

	public TurtleState pencolour(int r, int g, int b) {
		List<Pair<String, Object>> args = new ArrayList<Pair<String, Object>>();
		args.add(new Pair<String, Object>("r", r));
		args.add(new Pair<String, Object>("g", g));
		args.add(new Pair<String, Object>("b", b));

		return turtleRequest("pencolour", args, true);
	}

	public TurtleState canvas_size(int width, int height) {
		List<Pair<String, Object>> args = new ArrayList<Pair<String, Object>>();
		args.add(new Pair<String, Object>("width", width));
		args.add(new Pair<String, Object>("height", height));

		return turtleRequest("canvas_size", args, false);
	}

	public TurtleState export_img(String filename) {
		List<Pair<String, Object>> args = new ArrayList<Pair<String, Object>>();
		args.add(new Pair<String, Object>("filename", filename));

		return turtleRequest("export_img", args, false);
	}
}
