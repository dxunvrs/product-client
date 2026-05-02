package io.readers;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

public class FileReader implements Reader {
    private final Scanner scanner;

    public FileReader(String fileName) throws IOException {
        scanner = new Scanner(new File(fileName));
    }

    @Override
    public boolean hasNextLine() {
        return scanner.hasNextLine();
    }

    @Override
    public String nextLine(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine();
    }

    @Override
    public void close() {
        scanner.close();
    }
}
