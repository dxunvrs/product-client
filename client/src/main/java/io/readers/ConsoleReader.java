package io.readers;

import org.jline.builtins.Completers;
import org.jline.reader.*;
import org.jline.reader.impl.completer.AggregateCompleter;
import org.jline.reader.impl.completer.ArgumentCompleter;
import org.jline.reader.impl.completer.NullCompleter;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.util.Set;
import java.util.function.Supplier;

public class ConsoleReader implements Reader {
    private final LineReader reader;
    private final Terminal terminal;
    private Set<String> suggestions; // список подсказок
    private boolean isCommandMode = false;
    private boolean isHidden = false;

    public ConsoleReader(Supplier<Set<String>> commandsSupplier) throws IOException {
        terminal = TerminalBuilder.builder().system(true).build();
        reader = LineReaderBuilder.builder().terminal(terminal).completer(createCompleter(commandsSupplier)).build();
        reader.setOpt(LineReader.Option.CASE_INSENSITIVE);
    }

    private Completer createCompleter(Supplier<Set<String>> commandsSupplier) {
        // автодополнение команд
        Completer simpleCommandsCompleter = (lineReader, parsedLine, list) -> {
            Set<String> currentCommands = commandsSupplier.get();
            for (String commandName: currentCommands) {
                list.add(new Candidate(commandName));
            }
        };

        // автодополнение для команды скрипта: execute_script [FILE_NAME]
        ArgumentCompleter scriptCompleter = new ArgumentCompleter( 
                new StringsCompleter("execute_script"),
                new Completers.FileNameCompleter()
        );

        // автодополнение для команды help: help [COMMAND_NAME]
        ArgumentCompleter helpCompleter = new ArgumentCompleter(
                new StringsCompleter("help"),
                simpleCommandsCompleter,
                new NullCompleter()
        );

        // для избежания повтора автодополнений
        ArgumentCompleter strictCommandsCompleter = new ArgumentCompleter(simpleCommandsCompleter, new NullCompleter());

        // Completer Proxy
        return ((lineReader, parsedLine, list) -> {
            if (suggestions != null && !suggestions.isEmpty()) {
                suggestions.forEach(s -> list.add(new Candidate(s)));
            }
            if (!isCommandMode) {
                return;
            }
            new AggregateCompleter(scriptCompleter, helpCompleter, strictCommandsCompleter).complete(
                    lineReader, parsedLine, list
            );
        });
    }

    @Override
    public void setCommandMode(boolean mode) {
        this.isCommandMode = mode;
    }

    @Override
    public void setSuggestions(Set<String> suggestions) {
        this.suggestions = suggestions;
    }

    @Override
    public void setIsHidden(boolean isHidden) { this.isHidden = isHidden; }

    @Override
    public boolean hasNextLine() {
        return true;
    }

    @Override
    public String nextLine(String prompt) {
        try {
            if (isHidden) {
                return reader.readLine(prompt, '\0');
            }
            return reader.readLine(prompt);
        } catch (EndOfFileException | UserInterruptException e) {
            return null;
        }

    }

    @Override
    public void close() {
        try {
            terminal.close();
        } catch (IOException e) {
            System.out.println("Не удалось закрыть терминал");
        }
    }
}
