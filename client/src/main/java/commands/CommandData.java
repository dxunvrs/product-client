package commands;

import models.Product;

import java.util.ArrayList;
import java.util.List;

public class CommandData {
    private final List<String> stringArgs = new ArrayList<>();
    private final List<Integer> intArgs = new ArrayList<>();
    private final List<Product> objectArgs = new ArrayList<>();

    public void addStringArg(String value) {
        stringArgs.add(value);
    }

    public void addIntArg(int value) {
        intArgs.add(value);
    }

    public void addObjectArg(Product value) {
        objectArgs.add(value);
    }

    public List<String> getStringArgs() {
        return stringArgs;
    }

    public List<Integer> getIntArgs() {
        return intArgs;
    }

    public List<Product> getObjectArgs() {
        return objectArgs;
    }
}
