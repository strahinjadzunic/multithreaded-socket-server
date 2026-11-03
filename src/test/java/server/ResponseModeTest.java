package server;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResponseModeTest {

    @Test
    void oddClientIdsGetUppercase() {
        assertEquals(ResponseMode.UPPERCASE, ResponseMode.forClient(1));
        assertEquals(ResponseMode.UPPERCASE, ResponseMode.forClient(3));
    }

    @Test
    void evenClientIdsGetReversed() {
        assertEquals(ResponseMode.REVERSED, ResponseMode.forClient(2));
        assertEquals(ResponseMode.REVERSED, ResponseMode.forClient(4));
    }

    @Test
    void uppercaseModeConvertsMessageToUppercase() {
        assertEquals("HELLO WORLD", ResponseMode.UPPERCASE.apply("Hello World"));
    }

    @Test
    void uppercaseDoesNotDependOnSystemLanguage() {
        Locale original = Locale.getDefault();
        try {
            // With Turkish rules, "i" would become a dotted capital I instead of "I"
            Locale.setDefault(Locale.forLanguageTag("tr"));
            assertEquals("HI", ResponseMode.UPPERCASE.apply("hi"));
        } finally {
            Locale.setDefault(original);
        }
    }

    @Test
    void reversedModeReversesMessage() {
        assertEquals("dlroW olleH", ResponseMode.REVERSED.apply("Hello World"));
    }
}
