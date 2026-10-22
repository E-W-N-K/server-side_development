package by.bsuir.stsr;

import java.sql.*;

/** Лабораторная №2: операции Statement и PreparedStatement над учебной БД. */
public final class App {
    private static final String URL = "jdbc:h2:mem:stsr;DB_CLOSE_DELAY=-1";

    public static void main(String[] args) throws SQLException {
        try (Connection db = DriverManager.getConnection(URL)) {
            createSchema(db);
            seed(db);
            statementTasks(db);
            preparedTasks(db);
        }
    }

    private static void createSchema(Connection db) throws SQLException {
        try (Statement s = db.createStatement()) {
            s.execute("CREATE TABLE authors(id BIGINT PRIMARY KEY, first_name VARCHAR(100), last_name VARCHAR(100))");
            s.execute("CREATE TABLE books(id BIGINT PRIMARY KEY, title VARCHAR(200), author_id BIGINT REFERENCES authors(id), year_of_publication INT)");
            s.execute("CREATE TABLE shops(id BIGINT PRIMARY KEY, name VARCHAR(100), address VARCHAR(200))");
            s.execute("CREATE TABLE shops_books(shop_id BIGINT REFERENCES shops(id), book_id BIGINT REFERENCES books(id), PRIMARY KEY(shop_id, book_id))");
            s.execute("CREATE TABLE students(id BIGINT PRIMARY KEY, first_name VARCHAR(100), last_name VARCHAR(100), age INT, city VARCHAR(100), email VARCHAR(200))");
            s.execute("CREATE TABLE courses(id BIGINT PRIMARY KEY, title VARCHAR(100), teacher VARCHAR(100), duration_hours INT)");
            s.execute("CREATE TABLE suppliers(id BIGINT PRIMARY KEY, name VARCHAR(100), city VARCHAR(100), email VARCHAR(200))");
        }
    }

    private static void seed(Connection db) throws SQLException {
        try (Statement s = db.createStatement()) {
            s.executeUpdate("INSERT INTO authors VALUES (1,'Лев','Толстой'),(2,'Фёдор','Достоевский'),(3,'Антон','Чехов')");
            s.executeUpdate("INSERT INTO books VALUES (1,'Война и мир',1,1869),(2,'Анна Каренина',1,1877),(3,'Идиот',2,1869),(4,'Рассказы',3,1888)");
            s.executeUpdate("INSERT INTO shops VALUES (1,'Книжный мир','Минск'),(2,'Буква','Гродно')");
            s.executeUpdate("INSERT INTO shops_books VALUES (1,1),(1,2),(1,3),(2,4)");
            s.executeUpdate("INSERT INTO students VALUES (1,'Евгений','Сакун',20,'Минск','student@example.org')");
        }
    }

    private static void statementTasks(Connection db) throws SQLException {
        System.out.println("=== Statement: задание из методички ===");
        try (Statement s = db.createStatement()) {
            for (String table : new String[]{"authors", "books", "shops", "shops_books", "students"}) {
                print(s, "Все записи: " + table, "SELECT * FROM " + table);
            }
            print(s, "Авторы по имени", "SELECT * FROM authors ORDER BY first_name, last_name");
            print(s, "Авторы по фамилии", "SELECT * FROM authors ORDER BY last_name, first_name");
            print(s, "Книги после 1870 года", "SELECT * FROM books WHERE year_of_publication > 1870 ORDER BY year_of_publication");
            // Для воспроизводимости вместо случайной выбирается первая книга указанного магазина.
            int changed = s.executeUpdate("UPDATE books SET title = title || ' (новое издание)' WHERE id = " +
                    "(SELECT MIN(b.id) FROM books b JOIN shops_books sb ON sb.book_id=b.id WHERE sb.shop_id=1)");
            System.out.println("Обновлено названий книг магазина №1: " + changed);
            print(s, "Книги авторов на Т", "SELECT b.title, a.last_name FROM books b JOIN authors a ON a.id=b.author_id WHERE a.last_name LIKE 'Т%'");
            changed = s.executeUpdate("UPDATE authors SET first_name='Лев Николаевич' WHERE id IN " +
                    "(SELECT b.author_id FROM books b JOIN shops_books sb ON sb.book_id=b.id WHERE sb.shop_id=1) AND id=1");
            System.out.println("Обновлено имён авторов магазина №1: " + changed);
            print(s, "Книг у каждого автора", "SELECT a.last_name, (SELECT COUNT(*) FROM books b WHERE b.author_id=a.id) AS book_count FROM authors a ORDER BY a.id");
        }
    }

    private static void print(Statement s, String label, String sql) throws SQLException {
        System.out.println("-- " + label);
        try (ResultSet rs = s.executeQuery(sql)) {
            ResultSetMetaData md = rs.getMetaData();
            while (rs.next()) {
                StringBuilder row = new StringBuilder();
                for (int c = 1; c <= md.getColumnCount(); c++) {
                    if (c > 1) row.append(" | ");
                    row.append(md.getColumnLabel(c)).append('=').append(rs.getString(c));
                }
                System.out.println(row);
            }
        }
    }

    private static void preparedTasks(Connection db) throws SQLException {
        System.out.println("=== PreparedStatement: дополнительные сущности ===");
        db.setAutoCommit(false);
        try {
            insert(db, "INSERT INTO courses VALUES (?,?,?,?)", 1L, "Java и JDBC", "Иванов", 32);
            insert(db, "INSERT INTO suppliers VALUES (?,?,?,?)", 1L, "Учебные книги", "Минск", "books@example.org");
            update(db, "UPDATE courses SET duration_hours=? WHERE id=?", 40, 1L);
            update(db, "UPDATE suppliers SET city=? WHERE id=?", "Брест", 1L);
            select(db, "SELECT * FROM courses WHERE id=?", 1L);
            select(db, "SELECT * FROM suppliers WHERE id=?", 1L);
            db.commit();
        } catch (SQLException e) {
            db.rollback();
            throw e;
        } finally {
            db.setAutoCommit(true);
        }
    }

    private static void insert(Connection db, String sql, Object... values) throws SQLException {
        update(db, sql, values);
    }

    private static void update(Connection db, String sql, Object... values) throws SQLException {
        try (PreparedStatement ps = db.prepareStatement(sql)) {
            bind(ps, values);
            System.out.println("Изменено строк: " + ps.executeUpdate());
        }
    }

    private static void select(Connection db, String sql, Object... values) throws SQLException {
        try (PreparedStatement ps = db.prepareStatement(sql)) {
            bind(ps, values);
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData md = rs.getMetaData();
                while (rs.next()) {
                    for (int i=1; i<=md.getColumnCount(); i++) {
                        System.out.print(md.getColumnLabel(i) + "=" + rs.getString(i) + " ");
                    }
                    System.out.println();
                }
            }
        }
    }

    private static void bind(PreparedStatement ps, Object[] values) throws SQLException {
        for (int i=0; i<values.length; i++) ps.setObject(i+1, values[i]);
    }
}
