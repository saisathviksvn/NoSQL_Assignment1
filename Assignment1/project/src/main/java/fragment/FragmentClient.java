package fragment;
import java.sql.*;
import java.util.*;
import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
public class FragmentClient {

    private Map<Integer, Connection> connectionPool;
    private Router router;
    private int numFragments;

    public FragmentClient(int numFragments) {
        this.numFragments = numFragments;
        this.router = new Router(numFragments);
        this.connectionPool = new HashMap<>();
    }

    /**
     * TODO: Initialize JDBC connections to all N Fragments.
     */
    public void setupConnections() {
        for (int i = 0; i < numFragments; i++) {
            try {
                String dbName = "fragment" + i;
                String url = "jdbc:postgresql://localhost:5432/" + dbName;

                Connection conn = DriverManager.getConnection(
                        url,
                        "simufrag",
                        "simufrag123"
                );

                connectionPool.put(i, conn);

                System.out.println("Connected to " + dbName);

            } catch (SQLException e) {
                System.err.println("FAILED to connect to fragment" + i);
                e.printStackTrace();

                throw new RuntimeException("Cannot start system without all fragments");
            }
        }
    }


    /**
     * TODO: Route the student to the correct shard and execute the INSERT.
     */
    public void insertStudent(String studentId, String name, int age, String email) {
        try {
            // Your code here:
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * TODO: Route the grade to the correct shard and execute the INSERT.
     */
    public void insertGrade(String studentId, String courseId, int score) {
        try {
            // Your code here
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void updateGrade(String studentId, String courseId, int newScore) {
        try {
		    // Your code here:
            int fragmentId = router.getFragmentId(studentId);
            Connection conn = connectionPool.get(fragmentId);
            String sql = "UPDATE Grade SET score = ? WHERE student_id = ? AND course_id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, newScore);
            ps.setString(2, studentId);
            ps.setString(3, courseId);
            ps.executeUpdate();
            ps.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void deleteStudentFromCourse(String studentId, String courseId) {
        try {
            // Your code here

            int fragmentId = router.getFragmentId(studentId);
            Connection conn = connectionPool.get(fragmentId);
            String sql = "DELETE FROM Grade WHERE student_id = ? AND course_id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, studentId);
            ps.setString(2, courseId);
            ps.executeUpdate();
            ps.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * TODO: Fetch the student's name and email.
     */
	public String getStudentProfile(String studentId) {
        try {
            // Your code here

            int fragmentId = router.getFragmentId(studentId);
            Connection conn = connectionPool.get(fragmentId);
            String sql = "SELECT name, email FROM Student WHERE student_id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, studentId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String name = rs.getString("name");
                String email = rs.getString("email");
                return name + "," + email;
            }
            ps.close();
            return null;
            
        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR";
        }
    }

    /**
     * TODO: Calculate the average score per department.
     */
    public String getAvgScoreByDept() {
        try {
            // Your code here
            int fragmentId = new Random().nextInt(numFragments);
            Connection conn = connectionPool.get(fragmentId);
             String sql = 
                "SELECT c.department, AVG(g.score) AS avg_score " +
                "FROM Grade g JOIN Course c ON g.course_id = c.course_id " +
                "GROUP BY c.department";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            StringBuilder result = new StringBuilder();
            boolean first = true;
            while (rs.next()) {
                if (!first) result.append(";");
                first = false;

                String dept = rs.getString("department");
                double avg = rs.getDouble("avg_score");

                result.append(dept).append(":").append(String.format("%.1f", avg));
            }

            rs.close();
            stmt.close();

            return result.toString();
            // return null;

        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR";
        }
    }

    /**
     * TODO: Find all the students that have taken most number of courses
     */
    public String getAllStudentsWithMostCourses() {
        try {
            // Your code here
            int fragmentId = new Random().nextInt(numFragments); 
            Connection conn = connectionPool.get(fragmentId);
            String sql = 
                "SELECT student_id " +
                "FROM Grade " +
                "GROUP BY student_id " +
                "HAVING COUNT(course_id) = (" +
                "  SELECT MAX(cnt) FROM (" +
                "    SELECT COUNT(course_id) AS cnt FROM Grade GROUP BY student_id" +
                "  ) sub" +
                ")";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            StringBuilder result = new StringBuilder();
            boolean first = true;
            while (rs.next()) {
                if (!first) result.append(";");
                first = false;
                result.append(rs.getString("student_id"));
            }
            rs.close();
            stmt.close();
            return result.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR";
        }
    }

    public void closeConnections() {
        try {
            for (Connection conn : connectionPool.values()) {
                if (conn != null && !conn.isClosed()) {
                    conn.close();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
