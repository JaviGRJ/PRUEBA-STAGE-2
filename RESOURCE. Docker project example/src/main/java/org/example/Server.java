package org.example;

import static spark.Spark.get;
import static spark.Spark.port;

public class Server {
	public static void main(String[] args) {
		port(8080);
		get("/hello", (req, res) -> "Hello World");
	}
}