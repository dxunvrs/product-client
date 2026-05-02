package commands;

public abstract class Command {
    private final String name;
    private final String description;

    public Command(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public abstract CommandData execute(String[] tokens);

    public abstract String getSyntax();

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}
