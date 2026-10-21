package org.ulpgc.bigdata.stage2;

import java.nio.file.Path;
import java.sql.*;

final class MetadataRepository {
    private MetadataRepository() {}

    static void upsert(Path database, Book book) throws SQLException {
        String url = "jdbc:sqlite:" + database.toAbsolutePath();
        try (Connection connection = DriverManager.getConnection(url);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS books (book_id INTEGER PRIMARY KEY, title TEXT NOT NULL, author TEXT NOT NULL, language TEXT NOT NULL, year INTEGER)");
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO books(book_id,title,author,language,year) VALUES(?,?,?,?,?) " +
                    "ON CONFLICT(book_id) DO UPDATE SET title=excluded.title, author=excluded.author, language=excluded.language, year=excluded.year")) {
                insert.setInt(1, book.book_id());
                insert.setString(2, book.title());
                insert.setString(3, book.author());
                insert.setString(4, book.language());
                if (book.year() == null) insert.setNull(5, Types.INTEGER); else insert.setInt(5, book.year());
                insert.executeUpdate();
            }
        }
    }
}
