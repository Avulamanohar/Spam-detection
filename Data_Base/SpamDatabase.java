import java.sql.*;
import java.util.*;

public class SpamDatabase {

    //  Local DB connection
   private static final String DB_URL =System.getenv("DB_URL");
    private static final String USER =  System.getenv("DB_USER");
    private static final String PASSWORD =  System.getenv("DB_PASSWORD");



    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, USER, PASSWORD);
    }

    //  Auto-create table only if missing
    public static void initDatabase() {
        String createTableSQL = """
            CREATE TABLE IF NOT EXISTS spam_detection (
                id SERIAL PRIMARY KEY,
                content VARCHAR(300000) UNIQUE NOT NULL,
                verdict VARCHAR(20) NOT NULL
            );
        """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate(createTableSQL);

            // Check if table already existed
            String checkSQL = "SELECT COUNT(*) FROM information_schema.tables WHERE table_name='spam_detection'";
            try (PreparedStatement ps = conn.prepareStatement(checkSQL);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    System.out.println("able 'spam_detection' is ready already exists .");
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static String normalizeText(String text) {
        return text
            .replaceAll("\\r\\n?", "\n")
            .replaceAll("\\s+", " ")    
            .trim()
            .toLowerCase(Locale.ROOT);
    }

    public static String findVerdict(String messageContent) {
        String sql = "SELECT verdict FROM spam_detection WHERE LOWER(REGEXP_REPLACE(content, '\\s+', ' ', 'g')) = LOWER(?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, normalizeText(messageContent));
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("verdict");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void insertMessage(String content, String verdict) {
        String sql = "INSERT INTO spam_detection (content, verdict) VALUES (?, ?) ON CONFLICT (content) DO NOTHING";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, content.trim());
            ps.setString(2, verdict);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                System.out.println("Saved: " + content.substring(0, Math.min(50, content.length())) + "... → " + verdict);
            } else {
                System.out.println(" Duplicate detected, skipping insert");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        try {
            System.out.println(" Connecting to DB...");
            Connection conn = getConnection();
            System.out.println(" Connected successfully!");

            // Auto-create table if not exists
            initDatabase();

           /*  System.out.print("Enter a message to test verdict: ");
            String msg = sc.nextLine();
            String testVerdict = findVerdict(msg);
            System.out.println("Verdict: " + testVerdict);

            //System.out.print("Enter a message to insert (content, verdict): ");
            //String content = sc.nextLine();
            //String verdict = sc.nextLine();
            insertMessage(content, verdict);*/
            System.out.println("DEBUG: DB_URL=" + DB_URL);
System.out.println("DEBUG: USER=" + USER);
System.out.println("DEBUG: PASSWORD=" + PASSWORD);


            conn.close();
        } catch (SQLException e) {
            System.out.println("DB connection failed!");
            e.printStackTrace();
        }
    }
}
