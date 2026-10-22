package by.bsuir.stsr;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import org.w3c.dom.*;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.XMLConstants;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

/** Лабораторная №3: SAX (вариант 9), DOM, JAXB и Gson. */
public final class App {
    private static final Path OUT = Path.of("target", "lr3-output");

    public static void main(String[] args) throws Exception {
        Files.createDirectories(OUT);
        saxClients();
        domEmployees();
        jaxbOrganization();
        gsonLessons();
    }

    private static void saxClients() throws Exception {
        LocalDate today = LocalDate.now();
        String xml = "<clients>" +
                "<client id=\"1\"><name>Анна</name><contact>anna@example.org</contact><lastOrder>" + today.minusDays(10) + "</lastOrder><total>125.50</total></client>" +
                "<client id=\"2\"><name>Борис</name><contact>boris@example.org</contact><lastOrder>" + today.minusMonths(2) + "</lastOrder><total>80.00</total></client>" +
                "<client id=\"3\"><name>Вера</name><contact>vera@example.org</contact><lastOrder>" + today.minusDays(2) + "</lastOrder><total>360.00</total></client>" +
                "</clients>";
        Path file = OUT.resolve("clients.xml");
        Files.writeString(file, xml, StandardCharsets.UTF_8);
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        ClientHandler handler = new ClientHandler();
        factory.newSAXParser().parse(file.toFile(), handler);
        System.out.println("SAX: клиенты с заказом за последний месяц (" + today.minusMonths(1) + " — " + today + "):");
        handler.clients.stream().filter(c -> !c.lastOrder.isBefore(today.minusMonths(1)) && !c.lastOrder.isAfter(today))
                .forEach(System.out::println);
    }

    private record Client(String name, String contact, LocalDate lastOrder, double total) {}
    private static final class ClientHandler extends DefaultHandler {
        final List<Client> clients = new ArrayList<>();
        String element = "", name, contact, date, total;
        final StringBuilder text = new StringBuilder();
        @Override public void startElement(String uri, String local, String qName, Attributes attributes) {
            element = qName; text.setLength(0);
        }
        @Override public void characters(char[] ch, int start, int length) { text.append(ch, start, length); }
        @Override public void endElement(String uri, String local, String qName) {
            switch (qName) {
                case "name" -> name = text.toString().trim();
                case "contact" -> contact = text.toString().trim();
                case "lastOrder" -> date = text.toString().trim();
                case "total" -> total = text.toString().trim();
                case "client" -> clients.add(new Client(name, contact, LocalDate.parse(date), Double.parseDouble(total)));
            }
            element = "";
        }
    }

    private static DocumentBuilder secureBuilder() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        return factory.newDocumentBuilder();
    }

    private static void domEmployees() throws Exception {
        Path file = OUT.resolve("employees.xml");
        Document doc = secureBuilder().newDocument();
        Element root = doc.createElement("employees"); doc.appendChild(root);
        // Добавлены поля email и phone; у email есть атрибут verified.
        addEmployee(doc, root, 1, "Евгений Сакун", 20, "мужской", "разработчик", "sakun@example.org", "+375291234567", true);
        addEmployee(doc, root, 2, "Анна Иванова", 24, "женский", "аналитик", "anna@example.org", "+375331234567", false);
        writeXml(doc, file);
        Document read = secureBuilder().parse(file.toFile());
        NodeList employees = read.getElementsByTagName("employee");
        System.out.println("DOM: прочитано сотрудников: " + employees.getLength());
        for (int i=0; i<employees.getLength(); i++) {
            Element e = (Element) employees.item(i);
            System.out.println("  " + e.getAttribute("id") + " " + value(e,"name") + ", " + value(e,"role") + ", " + value(e,"email") + ", " + value(e,"phone"));
        }
        Element found = findEmployee(read, 2);
        System.out.println("DOM: найден id=2: " + value(found, "name"));
        found.getParentNode().removeChild(found);
        writeXml(read, OUT.resolve("employees-after-delete.xml"));
        System.out.println("DOM: после удаления id=2 осталось " + read.getElementsByTagName("employee").getLength() + " записей");
    }

    private static void addEmployee(Document doc, Element root, int id, String name, int age, String gender,
                                    String role, String email, String phone, boolean verified) {
        Element employee = doc.createElement("employee"); employee.setAttribute("id", Integer.toString(id));
        for (String[] field : new String[][]{{"name",name},{"age",Integer.toString(age)},{"gender",gender},
                {"role",role},{"email",email},{"phone",phone}}) {
            Element child = doc.createElement(field[0]); child.setTextContent(field[1]);
            if (field[0].equals("email")) child.setAttribute("verified", Boolean.toString(verified));
            employee.appendChild(child);
        }
        root.appendChild(employee);
    }

    private static Element findEmployee(Document doc, int id) {
        NodeList list = doc.getElementsByTagName("employee");
        for (int i=0; i<list.getLength(); i++) {
            Element e = (Element) list.item(i);
            if (e.getAttribute("id").equals(Integer.toString(id))) return e;
        }
        throw new IllegalArgumentException("Сотрудник не найден: " + id);
    }
    private static String value(Element parent, String tag) {
        return parent.getElementsByTagName(tag).item(0).getTextContent();
    }
    private static void writeXml(Document doc, Path file) throws Exception {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.transform(new DOMSource(doc), new StreamResult(file.toFile()));
    }

    private static void jaxbOrganization() throws Exception {
        Organization source = new Organization("БГУИР", List.of(
                new Organization.Department(1,"Разработка","Иванов"),
                new Organization.Department(2,"Аналитика","Петрова")));
        JAXBContext context = JAXBContext.newInstance(Organization.class);
        Path file = OUT.resolve("organization.xml");
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.marshal(source, file.toFile());
        Organization loaded = (Organization) context.createUnmarshaller().unmarshal(file.toFile());
        System.out.println("JAXB: " + loaded.name + ", отделов: " + loaded.departments.size());
        loaded.departments.forEach(d -> System.out.println("  " + d));
    }

    private static void gsonLessons() throws IOException {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(LocalTime.class, (JsonSerializer<LocalTime>) (v,t,c) -> new JsonPrimitive(v.toString()))
                .registerTypeAdapter(LocalTime.class, (JsonDeserializer<LocalTime>) (v,t,c) -> LocalTime.parse(v.getAsString()))
                .setPrettyPrinting().create();
        List<Lesson> lessons = List.of(
                new Lesson("JDBC", LocalTime.of(1,30), List.of("Connection", "PreparedStatement")),
                new Lesson("Marshalling", LocalTime.of(2,0), List.of("SAX", "DOM", "JAXB", "Gson")));
        Path file = OUT.resolve("lessons.json");
        Files.writeString(file, gson.toJson(lessons), StandardCharsets.UTF_8);
        Type listType = new TypeToken<List<Lesson>>() {}.getType();
        List<Lesson> read = gson.fromJson(Files.readString(file, StandardCharsets.UTF_8), listType);
        System.out.println("Gson: считано занятий: " + read.size());
        read.forEach(System.out::println);
    }
}
