package by.bsuir.stsr;

import java.time.LocalTime;
import java.util.List;

public record Lesson(String subject, LocalTime duration, List<String> topics) {}
