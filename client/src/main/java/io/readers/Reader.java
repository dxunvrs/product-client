package io.readers;

import java.util.Set;

public interface Reader {
    boolean hasNextLine();
    String nextLine(String prompt);
    void close();
    default void setSuggestions(Set<String> suggestions) {}
    default void setCommandMode(boolean mode) {}
    default void setIsHidden(boolean isHidden) {}
}
