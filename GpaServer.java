import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Government Polytechnic Adityapur - Professional Web Server
 * Built with Java's built-in HTTP server - no external dependencies needed.
 * 
 * Features:
 * - Serves static files (HTML, CSS, JS, images)
 * - REST API for Faculty, Notices, Departments, Contact
 * - JSON response formatting
 * - CORS support
 * - Request logging
 * 
 * Run: javac GpaServer.java && java GpaServer
 * Access: http://localhost:8080
 */
public class GpaServer {

    private static final int PORT = 8080;
    private static final String PUBLIC_DIR = "../public";
    private static final List<Map<String, String>> contactMessages = Collections.synchronizedList(new ArrayList<>());

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        // API Endpoints
        server.createContext("/api/faculty", GpaServer::handleFaculty);
        server.createContext("/api/notices", GpaServer::handleNotices);
        server.createContext("/api/departments", GpaServer::handleDepartments);
        server.createContext("/api/contact", GpaServer::handleContact);
        server.createContext("/api/testimonials", GpaServer::handleTestimonials);
        server.createContext("/api/stats", GpaServer::handleStats);

        // Static file server (must be last - catches all other routes)
        server.createContext("/", GpaServer::handleStaticFiles);

        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();

        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║   Government Polytechnic Adityapur - Web Server         ║");
        System.out.println("║   ─────────────────────────────────────────────────      ║");
        System.out.println("║   Server running on: http://localhost:" + PORT + "              ║");
        System.out.println("║   Press Ctrl+C to stop                                  ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
    }

    // ════════════════════════════════════════
    //  STATIC FILE SERVER
    // ════════════════════════════════════════

    private static void handleStaticFiles(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if (path.equals("/")) path = "/index.html";
        
        // Map clean URLs to pages
        if (!path.contains(".") && !path.startsWith("/api")) {
            path = "/pages" + path + ".html";
        }

        File file = new File(PUBLIC_DIR + path).getCanonicalFile();
        
        if (!file.exists()) {
            // Fallback to index.html for SPA-like behavior
            file = new File(PUBLIC_DIR + "/index.html");
        }

        if (file.exists() && file.isFile()) {
            String contentType = getContentType(file.getName());
            byte[] bytes = Files.readAllBytes(file.toPath());
            
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.getResponseHeaders().set("Cache-Control", "public, max-age=3600");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
        } else {
            String notFound = "<html><body><h1>404 - Page Not Found</h1></body></html>";
            exchange.sendResponseHeaders(404, notFound.length());
            exchange.getResponseBody().write(notFound.getBytes());
        }
        exchange.getResponseBody().close();
        log(exchange, path);
    }

    // ════════════════════════════════════════
    //  API: FACULTY
    // ════════════════════════════════════════

    private static void handleFaculty(HttpExchange exchange) throws IOException {
        setCorsHeaders(exchange);
        if (handlePreflight(exchange)) return;

        String query = exchange.getRequestURI().getQuery();
        String deptFilter = null;
        if (query != null && query.startsWith("dept=")) {
            deptFilter = query.substring(5);
        }

        List<Map<String, Object>> faculty = getFacultyData();
        
        if (deptFilter != null) {
            String filter = deptFilter;
            faculty = faculty.stream()
                .filter(f -> f.get("deptCode").toString().equalsIgnoreCase(filter))
                .collect(Collectors.toList());
        }

        sendJson(exchange, toJson(faculty));
    }

    private static List<Map<String, Object>> getFacultyData() {
        List<Map<String, Object>> list = new ArrayList<>();

        // Department of Electrical Engineering
        list.add(facultyMap("Mr. Birendra Prasad", "Professor / Lecturer SG", "Electrical Engineering", "ee",
            "B.Sc. Engg. (Electrical)", "16 Years", "0 Years", "Power Systems, Electrical Machines, Wiring"));
        list.add(facultyMap("Mr. Bikram Majhi", "Lecturer", "Electrical Engineering", "ee",
            "B.Sc. Engg. (Electrical)", "16 Years", "0 Years", "Control Systems, Power Electronics"));
        list.add(facultyMap("Dr. Chetna Sumedha", "Lecturer", "Electrical Engineering", "ee",
            "Ph.D.", "3 Years", "0 Years", "Signal Processing, Control Engineering"));
        list.add(facultyMap("Mr. Gulshan Kalundia", "Lecturer", "Electrical Engineering", "ee",
            "B.Tech", "9 Years", "0 Years", "Electrical Circuits, Measurement"));
        list.add(facultyMap("Mr. Akhilesh Kumar", "Lecturer", "Electrical Engineering", "ee",
            "M.Tech.", "5 Years", "1 Years", "Power Electronics, Drives"));

        // Department of Mechanical Engineering
        list.add(facultyMap("Mr. Suresh Tiwary", "Lecturer", "Mechanical Engineering", "mech",
            "Ph.D. (Pursuing)", "16 Years", "4 Years", "Thermodynamics, Heat Engines, Fluid Mechanics"));
        list.add(facultyMap("Mr. Gulshan Kumar", "Lecturer", "Mechanical Engineering", "mech",
            "M.Tech.", "10 Years", "0 Years", "Machine Design, Manufacturing"));
        list.add(facultyMap("Mr. Ravi Shankar", "Lecturer", "Mechanical Engineering", "mech",
            "M.Tech", "6 Years", "0 Years", "Production Engineering, CAD/CAM"));
        list.add(facultyMap("Md. Sakil", "Lecturer", "Mechanical Engineering", "mech",
            "B.E. / B.Tech (Mechanical)", "5 Years", "0 Years", "Workshop Technology, Production Engineering"));

        // Department of Metallurgical Engineering
        list.add(facultyMap("Mr. Rewati Raman Upadhyay", "Lecturer (Selection Grade)", "Metallurgical Engineering", "met",
            "M.Tech", "25 Years", "0 Years", "Iron & Steel Making, Physical Metallurgy"));
        list.add(facultyMap("Mr. Abhishek Kumar", "Lecturer", "Metallurgical Engineering", "met",
            "M.Tech", "4 Years", "7 Years", "Material Science, Heat Treatment, Quality Testing"));

        // Department of Computer Science & Engineering
        list.add(facultyMap("Mr. Kunal Mahto", "Lecturer (NBL)", "Computer Science & Engineering", "cse",
            "Ph.D. (Pursuing)", "9 Years", "1 Years", "Data Structures, Algorithms, Emerging Technologies"));
        list.add(facultyMap("Ms. Momita Rani Giri", "Lecturer (NBL)", "Computer Science & Engineering", "cse",
            "B.Tech (CSE)", "6 Years", "0 Years", "Programming, Databases"));
        list.add(facultyMap("Mr. Niraj Kumar", "Lecturer (NBL)", "Computer Science & Engineering", "cse",
            "B.Tech (CSE)", "4 Years", "2 Years", "Networking, Web Technologies"));
        list.add(facultyMap("Ms. Priyanka", "Lecturer (NBL)", "Computer Science & Engineering", "cse",
            "B.Tech (CSE)", "6 Years", "0 Years", "Data Structures, DBMS"));

        // Department of Humanities (Applied Sciences)
        list.add(facultyMap("Dr. Chetna Sumedha", "Senior Lecturer", "Applied Sciences & Humanities", "ash",
            "Ph.D. (Pursuing)", "34 Years", "0 Years", "Physics, Applied Sciences"));
        list.add(facultyMap("Mr. Manoj Kumar", "Lecturer", "Applied Sciences & Humanities", "ash",
            "M.Sc.", "10 Years", "0 Years", "Mathematics, Applied Mathematics"));
        list.add(facultyMap("Mr. Sanjay Prasad", "Lecturer", "Applied Sciences & Humanities", "ash",
            "M.Sc.", "20 Years", "0 Years", "Chemistry, Environmental Science"));
        list.add(facultyMap("Ms. Neha Kumari", "Lecturer", "Applied Sciences & Humanities", "ash",
            "M.Sc.", "4 Years", "0 Years", "English, Communication Skills"));

        return list;
    }

    private static Map<String, Object> facultyMap(String name, String designation, String dept, String deptCode,
            String qualification, String teachingExp, String industrialExp, String specialization) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", name);
        m.put("designation", designation);
        m.put("department", dept);
        m.put("deptCode", deptCode);
        m.put("qualification", qualification);
        m.put("teachingExperience", teachingExp);
        m.put("industrialExperience", industrialExp);
        m.put("specialization", specialization);
        return m;
    }

    // ════════════════════════════════════════
    //  API: NOTICES
    // ════════════════════════════════════════

    private static void handleNotices(HttpExchange exchange) throws IOException {
        setCorsHeaders(exchange);
        if (handlePreflight(exchange)) return;
        sendJson(exchange, toJson(getNoticesData()));
    }

    private static List<Map<String, Object>> getNoticesData() {
        List<Map<String, Object>> list = new ArrayList<>();
        list.add(noticeMap("4th Semester Attendance Mandatory", "2026-04-01", "academic",
            "All 4th semester students are hereby directed to ensure regular attendance from 01-04-2026, failing which they will not be permitted to appear in internal and board examinations. Mass bunk will not be allowed under any circumstances.",
            "https://gpa.ac.in/cdgps/src/uploads/General%20Postings/Notices/2026/04/950903.pdf"));
        list.add(noticeMap("4th Semester Classes Commence", "2026-03-28", "academic",
            "All students of 3rd Semester are hereby informed that the classes for 4th Semester will commence from 01-04-2026, and attendance is compulsory for all.",
            "https://gpa.ac.in/cdgps/src/uploads/General%20Postings/Notices/2026/03/692166.pdf"));
        list.add(noticeMap("Library Book Tender Notice", "2026-04-05", "tender",
            "Short-Term Tender Notice for Supply of Diploma-Level Technical, Academic, and General Books for the Library of Government Polytechnic, Adityapur.",
            "https://gpa.ac.in/cdgps/src/uploads/General%20Postings/Notices/2026/04/788437.pdf"));
        list.add(noticeMap("JCECEB PECE 2026 — Admissions Open", "2026-03-15", "admission",
            "Applications for Polytechnic Entrance Competitive Examination (PECE) 2026 are now open. Eligible candidates (10th pass with min 35% marks) must apply online at jceceb.jharkhand.gov.in.",
            ""));
        list.add(noticeMap("JUT Board Examination Schedule", "2026-03-20", "exam",
            "The JUT Board Examinations for 2nd and 4th semester students will be conducted as per the schedule released by Jharkhand University of Technology, Ranchi.",
            ""));
        list.add(noticeMap("Anti-Ragging Committee Reconstituted", "2026-03-10", "general",
            "The Anti-Ragging Committee and Squad for the academic session 2025-26 has been reconstituted. Any ragging incident must be reported immediately.",
            ""));
        list.add(noticeMap("Campus Placement Drive — Tata Steel & L&T", "2026-02-25", "placement",
            "Campus placement drive by Tata Steel and L&T scheduled for March 2026. Eligible final year students must register with the T&P cell.",
            ""));
        list.add(noticeMap("E-Kalyan Scholarship Application Deadline", "2026-03-01", "scholarship",
            "Students belonging to SC/ST/OBC categories are reminded to apply for E-Kalyan Scholarship before the deadline. Apply at ekalyan.cgg.gov.in.",
            ""));
        return list;
    }

    private static Map<String, Object> noticeMap(String title, String date, String type, String description, String pdfUrl) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("title", title);
        m.put("date", date);
        m.put("type", type);
        m.put("description", description);
        m.put("pdfUrl", pdfUrl);
        return m;
    }

    // ════════════════════════════════════════
    //  API: DEPARTMENTS
    // ════════════════════════════════════════

    private static void handleDepartments(HttpExchange exchange) throws IOException {
        setCorsHeaders(exchange);
        if (handlePreflight(exchange)) return;
        sendJson(exchange, toJson(getDepartmentsData()));
    }

    private static List<Map<String, Object>> getDepartmentsData() {
        List<Map<String, Object>> list = new ArrayList<>();
        list.add(deptMap("Computer Science & Engineering", "cse", 45, "3 Years",
            "Learn programming, data structures, database management, networking, and emerging technologies.",
            new String[]{"C/C++", "Java", "DBMS", "Networking", "Web Tech", "Data Structures", "Operating Systems"}));
        list.add(deptMap("Mechanical Engineering", "mech", 45, "3 Years",
            "Master manufacturing, thermodynamics, machine design, and production engineering.",
            new String[]{"CAD/CAM", "Thermal Engineering", "Manufacturing", "Machine Design", "Workshop", "Fluid Mechanics"}));
        list.add(deptMap("Electrical Engineering", "ee", 45, "3 Years",
            "Study power systems, control electronics, electrical machines, and power electronics.",
            new String[]{"Power Systems", "Control", "Machines", "Electronics", "Wiring", "Measurement"}));
        list.add(deptMap("Metallurgical Engineering", "met", 45, "3 Years",
            "Explore materials science, iron & steel technology, heat treatment, and quality testing.",
            new String[]{"Materials", "Iron & Steel", "Heat Treatment", "Testing", "Metallurgy", "Quality Control"}));
        return list;
    }

    private static Map<String, Object> deptMap(String name, String code, int seats, String duration, String desc, String[] subjects) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", name);
        m.put("code", code);
        m.put("seats", seats);
        m.put("duration", duration);
        m.put("description", desc);
        m.put("subjects", Arrays.asList(subjects));
        return m;
    }

    // ════════════════════════════════════════
    //  API: TESTIMONIALS
    // ════════════════════════════════════════

    private static void handleTestimonials(HttpExchange exchange) throws IOException {
        setCorsHeaders(exchange);
        if (handlePreflight(exchange)) return;

        List<Map<String, String>> list = new ArrayList<>();
        list.add(testimonialMap("Amit Kumar", "CSE Graduate", "Very Supportive Faculty & Good Placement Opportunities. The teachers really care about student growth and career placement."));
        list.add(testimonialMap("Ravi Xaxa", "Mechanical Graduate", "The faculty members are well qualified and very supportive. They help students understand concepts clearly and many students get campus placements."));
        list.add(testimonialMap("Shiwani Jaiswal", "Electrical Graduate", "GPA is one of the best institutes in Jharkhand. Its aim is to achieve excellence in educational field. It provides outstanding infrastructure and facilities."));
        list.add(testimonialMap("Sumit Panda", "Metallurgy Graduate", "This is one of the best polytechnic colleges in Jharkhand for placements. Major companies like Tata Steel, Wipro, and JSPL visit for recruitment."));
        list.add(testimonialMap("Liliy Rugu", "CSE Graduate", "The curriculum is relevant to industry needs and makes students industry-ready. Teachers are helpful and the quality of teaching is good."));
        list.add(testimonialMap("Imroz Rukhsana", "Electrical Graduate", "A life changing experience. The teaching staff provided excellent mentorship and career guidance throughout the program."));
        
        sendJson(exchange, toJson(list));
    }

    private static Map<String, String> testimonialMap(String name, String batch, String text) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("name", name);
        m.put("batch", batch);
        m.put("text", text);
        return m;
    }

    // ════════════════════════════════════════
    //  API: STATS
    // ════════════════════════════════════════

    private static void handleStats(HttpExchange exchange) throws IOException {
        setCorsHeaders(exchange);
        if (handlePreflight(exchange)) return;

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalStudents", 180);
        stats.put("departments", 4);
        stats.put("placementRate", 90);
        stats.put("highestPackage", "7 LPA");
        stats.put("established", 1980);
        stats.put("passoutStudents", 8300);
        stats.put("facultyCount", 19);
        stats.put("hostelCapacity", 100);

        sendJson(exchange, toJson(Map.of("stats", stats)));
    }

    // ════════════════════════════════════════
    //  API: CONTACT FORM
    // ════════════════════════════════════════

    private static void handleContact(HttpExchange exchange) throws IOException {
        setCorsHeaders(exchange);
        if (handlePreflight(exchange)) return;

        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, "{\"error\":\"Method not allowed. Use POST.\"}", 405);
            return;
        }

        String body = new String(exchange.getRequestBody().readAllBytes());
        Map<String, String> formData = parseFormData(body);

        String name = formData.getOrDefault("name", "");
        String email = formData.getOrDefault("email", "");
        String message = formData.getOrDefault("message", "");

        if (name.isEmpty() || email.isEmpty() || message.isEmpty()) {
            sendJson(exchange, "{\"success\":false,\"error\":\"All fields are required.\"}", 400);
            return;
        }

        formData.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        contactMessages.add(formData);

        System.out.println("[CONTACT] New message from: " + name + " <" + email + ">");
        sendJson(exchange, "{\"success\":true,\"message\":\"Thank you! Your message has been received. We will get back to you soon.\"}");
    }

    // ════════════════════════════════════════
    //  UTILITY METHODS
    // ════════════════════════════════════════

    private static void sendJson(HttpExchange exchange, String json) throws IOException {
        sendJson(exchange, json, 200);
    }

    private static void sendJson(HttpExchange exchange, String json, int statusCode) throws IOException {
        byte[] bytes = json.getBytes("UTF-8");
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.getResponseBody().close();
    }

    private static void setCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
    }

    private static boolean handlePreflight(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(200, -1);
            return true;
        }
        return false;
    }

    private static String getContentType(String fileName) {
        String ext = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        return switch (ext) {
            case "html" -> "text/html; charset=UTF-8";
            case "css" -> "text/css; charset=UTF-8";
            case "js" -> "application/javascript; charset=UTF-8";
            case "json" -> "application/json; charset=UTF-8";
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "gif" -> "image/gif";
            case "svg" -> "image/svg+xml";
            case "webp" -> "image/webp";
            case "ico" -> "image/x-icon";
            case "woff2" -> "font/woff2";
            case "woff" -> "font/woff";
            case "ttf" -> "font/ttf";
            case "pdf" -> "application/pdf";
            default -> "application/octet-stream";
        };
    }

    private static Map<String, String> parseFormData(String body) {
        Map<String, String> map = new HashMap<>();
        // Handle JSON body
        if (body.trim().startsWith("{")) {
            body = body.trim();
            body = body.substring(1, body.length() - 1);
            for (String pair : body.split(",")) {
                String[] kv = pair.split(":", 2);
                if (kv.length == 2) {
                    String key = kv[0].trim().replaceAll("\"", "");
                    String val = kv[1].trim().replaceAll("\"", "");
                    map.put(key, val);
                }
            }
        } else {
            // Handle URL-encoded body
            for (String pair : body.split("&")) {
                String[] kv = pair.split("=", 2);
                if (kv.length == 2) {
                    map.put(URLDecoder.decode(kv[0], java.nio.charset.StandardCharsets.UTF_8),
                            URLDecoder.decode(kv[1], java.nio.charset.StandardCharsets.UTF_8));
                }
            }
        }
        return map;
    }

    // Simple JSON serializer (no external library needed)
    @SuppressWarnings("unchecked")
    private static String toJson(Object obj) {
        if (obj == null) return "null";
        if (obj instanceof String) return "\"" + escapeJson((String) obj) + "\"";
        if (obj instanceof Number || obj instanceof Boolean) return obj.toString();
        if (obj instanceof List) {
            List<?> list = (List<?>) obj;
            return "[" + list.stream().map(GpaServer::toJson).collect(Collectors.joining(",")) + "]";
        }
        if (obj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) obj;
            return "{" + map.entrySet().stream()
                .map(e -> "\"" + escapeJson(e.getKey().toString()) + "\":" + toJson(e.getValue()))
                .collect(Collectors.joining(",")) + "}";
        }
        return "\"" + escapeJson(obj.toString()) + "\"";
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    private static void log(HttpExchange exchange, String path) {
        String method = exchange.getRequestMethod();
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.printf("[%s] %s %s%n", time, method, path);
    }
}
