import core.ConsoleApp;

public class Main {
    public static void main(String[] args) {
        if (args.length == 0) {
            ConsoleApp consoleApp = new ConsoleApp("localhost", 1234);
            consoleApp.start();
        } else if (args.length == 2) {
            ConsoleApp consoleApp = new ConsoleApp(args[0], Integer.parseInt(args[1]));
            consoleApp.start();
        } else {
            System.out.println("Введено неверное кол-во аргументов");
        }
    }
}
