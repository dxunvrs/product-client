package utility;

import exceptions.EndOfInputException;
import exceptions.ScriptExecutionException;
import exceptions.TypeNotFoundException;
import io.InputManager;
import models.UnitOfMeasure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class Form {
    private static final Logger logger = LoggerFactory.getLogger(Form.class);

    private final InputManager inputManager;
    private final boolean scriptMode;

    public Form(InputManager inputManager) {
        this.inputManager = inputManager;
        this.scriptMode = inputManager.isScriptMode();
    }

    protected <T> T ask(Class<T> type, String name, Validator<T> validator) {
        T result;
        while (true) {
            try {
                logger.info("У пользователя запрашивается {} типа {}", name, type.getSimpleName());
                result = map(type, inputManager.readData("Введите " + name + ": ", getSuggestions(type)).trim());
                if (validator.validate(result)) {
                    if (scriptMode) System.out.println(result);
                    break;
                }
                logger.debug("Значение не прошло валидацию");

            } catch (EndOfInputException e) {
                if (scriptMode) throw new ScriptExecutionException("Получен конец ввода, ожидались данные типа " + type.getSimpleName());
                throw new EndOfInputException("Конец ввода");

            } catch (NumberFormatException e) {
                handleError("Ожидались данные типа " + type.getSimpleName(), e);
            } catch (IllegalArgumentException e) {
                handleError("Такой единицы измерения не существует", e);
            } catch (DateTimeParseException e) {
                handleError("Ожидалась дата в формате yyyy-MM-dd", e);
            } catch (TypeNotFoundException e) {
                handleError("Тип не поддерживается", e);
            }
        }
        return result;
    }

    private <T> T map(Class<T> type, String value) throws TypeNotFoundException {
        if (value == null || value.isEmpty()) {
            return null;
        }

        return switch (type.getSimpleName()) {
            case "Integer", "int" -> type.cast(Integer.parseInt(value));
            case "Long", "long" -> type.cast(Long.parseLong(value));
            case "String" -> type.cast(value);
            case "LocalDate" -> type.cast(LocalDate.parse(value));
            case "UnitOfMeasure" -> type.cast(UnitOfMeasure.valueOf(value.trim().toUpperCase()));
            default -> throw new TypeNotFoundException("Тип еще не поддерживается");
        };
    }

    private <T> Set<String> getSuggestions(Class<T> type) {
        return switch (type.getSimpleName()) {
            case "UnitOfMeasure" -> Arrays.stream(UnitOfMeasure.values()).map(Enum::name).collect(Collectors.toSet());
            case "LocalDate" -> Set.of(LocalDate.now().toString());
            default -> null;
        };
    }

    private void handleError(String message, Exception e) {
        if (scriptMode) {
            throw new ScriptExecutionException(message);
        }
        logger.warn(message, e);
        System.out.println(message);
    }
}
