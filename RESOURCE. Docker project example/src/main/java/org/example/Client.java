package org.example;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;

import static spark.Spark.get;
import static spark.Spark.port;

public class Client {
	public static void main(String[] args) {
		while (true) {
			try {
				System.out.println(Jsoup.connect("http:localhost:8080/hello").get().body() + "\n------");
				Thread.sleep(1000);
			} catch (Throwable e) {
				throw new RuntimeException(e);
			}
		}
	}
}