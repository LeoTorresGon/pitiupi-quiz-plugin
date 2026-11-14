package pitiupi.quiz.model;

import pitiupi.platform.PitiupiHome;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Properties;

public final class QuizQuestionStore {
    private final Path file;
    public QuizQuestionStore() {
        this(PitiupiHome.get().cacheDirectory().resolve("quiz").resolve("perguntas.properties"));
    }
    QuizQuestionStore(Path file) { this.file = file; }

    public List<QuizQuestion> load() throws IOException {
        List<QuizQuestion> result = new ArrayList<>();
        if (!Files.exists(file)) return result;
        Properties p = new Properties();
        try (var in = Files.newInputStream(file)) { p.load(in); }
        int count = Integer.parseInt(p.getProperty("count", "0"));
        for (int i = 0; i < count; i++) {
            String prefix = "q." + i + ".";
            String text = decode(p.getProperty(prefix + "text", ""));
            String[] options = new String[4];
            for (int j = 0; j < 4; j++) options[j] = decode(p.getProperty(prefix + "option." + j, ""));
            int correct = Integer.parseInt(p.getProperty(prefix + "correct", "0"));
            result.add(new QuizQuestion(text, options, correct));
        }
        return result;
    }

    public void save(List<QuizQuestion> questions) throws IOException {
        Files.createDirectories(file.getParent());
        Properties p = new Properties();
        p.setProperty("count", Integer.toString(questions.size()));
        for (int i = 0; i < questions.size(); i++) {
            QuizQuestion q = questions.get(i);
            String prefix = "q." + i + ".";
            p.setProperty(prefix + "text", encode(q.getText()));
            String[] options = q.getOptions();
            for (int j = 0; j < 4; j++) p.setProperty(prefix + "option." + j, encode(options[j]));
            p.setProperty(prefix + "correct", Integer.toString(q.getCorrect()));
        }
        Path temp = Files.createTempFile(file.getParent(), "perguntas-", ".tmp");
        try {
            try (var out = Files.newOutputStream(temp)) { p.store(out, "Perguntas do Quiz P2P - Pitiupi"); }
            try { Files.move(temp, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE); }
            catch (java.nio.file.AtomicMoveNotSupportedException ex) { Files.move(temp, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temp); }
    }
    private static String encode(String s) { return Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8)); }
    private static String decode(String s) { return new String(Base64.getDecoder().decode(s), StandardCharsets.UTF_8); }
}
