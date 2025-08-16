package javono.lib;

public class JavonoString {

    private final char[] chars;

    // Construct from a literal or another JavonoString
    public JavonoString(String s) {
        this.chars = s.toCharArray();
    }

    // Construct from char array
    public JavonoString(char[] chars) {
        this.chars = new char[chars.length];
        for (int i = 0; i < chars.length; i++) {
            this.chars[i] = chars[i];
        }
    }

    // Length of string
    public int length() {
        return chars.length;
    }

    // Get character at index
    public char charAt(int index) {
        if (index < 0 || index >= chars.length) {
            throw new IndexOutOfBoundsException();
        }
        return chars[index];
    }

    // Concatenate two JavonoStrings
    public JavonoString concat(JavonoString other) {
        char[] newChars = new char[this.chars.length + other.chars.length];
        for (int i = 0; i < this.chars.length; i++) {
            newChars[i] = this.chars[i];
        }
        for (int i = 0; i < other.chars.length; i++) {
            newChars[this.chars.length + i] = other.chars[i];
        }
        return new JavonoString(newChars);
    }

    // Convert to standard String
    public String toString() {
        return new String(chars);
    }

    // Check equality
    public boolean equals(JavonoString other) {
        if (this.chars.length != other.chars.length) return false;
        for (int i = 0; i < this.chars.length; i++) {
            if (this.chars[i] != other.chars[i]) return false;
        }
        return true;
    }
}
