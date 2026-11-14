package pitiupi.quiz.model;

public final class QuizQuestion {
    private final String text;
    private final String[] options;
    private final int correct;

    public QuizQuestion(String text, String[] options, int correct) {
        if (text == null || text.isBlank()) throw new IllegalArgumentException("O enunciado é obrigatório.");
        if (options == null || options.length != 4) throw new IllegalArgumentException("A pergunta precisa ter quatro alternativas.");
        for (String option : options) if (option == null || option.isBlank()) throw new IllegalArgumentException("Preencha as quatro alternativas.");
        if (correct < 0 || correct > 3) throw new IllegalArgumentException("Selecione a resposta correta.");
        this.text = text.trim();
        this.options = options.clone();
        this.correct = correct;
    }
    public String getText() { return text; }
    public String[] getOptions() { return options.clone(); }
    public int getCorrect() { return correct; }
    @Override public String toString() { return text; }
}
