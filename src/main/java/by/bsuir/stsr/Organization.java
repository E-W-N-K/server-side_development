package by.bsuir.stsr;

import jakarta.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "organization")
@XmlAccessorType(XmlAccessType.FIELD)
public class Organization {
    @XmlAttribute public String name;
    @XmlElementWrapper(name = "departments")
    @XmlElement(name = "department")
    public List<Department> departments = new ArrayList<>();

    public Organization() {} // требуется JAXB
    public Organization(String name, List<Department> departments) {
        this.name = name;
        this.departments = departments;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Department {
        @XmlAttribute public int id;
        @XmlElement public String name;
        @XmlElement public String manager;

        public Department() {}
        public Department(int id, String name, String manager) {
            this.id = id; this.name = name; this.manager = manager;
        }
        @Override public String toString() { return id + ": " + name + " (" + manager + ")"; }
    }
}
