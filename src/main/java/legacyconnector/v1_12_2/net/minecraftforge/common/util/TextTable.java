package legacyconnector.v1_12_2.net.minecraftforge.common.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/** A plain text table (Forge's command output helper): columns with a header and an alignment, rows of values. */
public class TextTable {
    public static Column column(String header) {
        return new Column(header);
    }

    public static Column column(String header, Alignment alignment) {
        return new Column(header, alignment);
    }

    private final List<Column> columns;
    private final List<Row> rows = new ArrayList<>();

    public TextTable(List<Column> columns) {
        this.columns = columns;
    }

    public String build(String lineEnding) {
        StringBuilder destination = new StringBuilder();
        append(destination, lineEnding);
        return destination.toString();
    }

    public void append(StringBuilder destination, String lineEnding) {
        List<String> headers = columns.stream().map(c -> c.formatHeader(" ")).collect(Collectors.toList());
        printRow(destination, headers, '|', lineEnding);
        printSeparator(destination, lineEnding);
        for (Row row : rows) printRow(destination, row.format(columns, " "), '|', lineEnding);
    }

    private void printSeparator(StringBuilder destination, String lineEnding) {
        destination.append('|');
        for (Column column : columns) {
            destination.append(column.alignment != Alignment.RIGHT ? ':' : ' ');
            destination.append(column.getSeparator('-'));
            destination.append(column.alignment != Alignment.LEFT ? ':' : ' ');
            destination.append('|');
        }
        destination.append(lineEnding);
    }

    private void printRow(StringBuilder destination, List<String> values, char separator, String lineEnding) {
        destination.append(separator);
        for (String value : values) {
            destination.append(' ');
            destination.append(value);
            destination.append(' ');
            destination.append(separator);
        }
        destination.append(lineEnding);
    }

    public void add(Object... values) {
        if (values.length != columns.size()) {
            throw new IllegalArgumentException("Received wrong amount of values for table row, expected " + columns.size() + ", received " + values.length + ".");
        }
        Row row = new Row();
        for (int i = 0; i < values.length; i++) {
            String value = String.valueOf(values[i]);
            row.values.add(value);
            columns.get(i).fit(value);
        }
        rows.add(row);
    }

    public void clear() {
        for (Column column : columns) column.resetWidth();
        rows.clear();
    }

    public List<Column> getColumns() {
        return columns;
    }

    public static class Column {
        private final String header;
        private int width;
        private final Alignment alignment;

        public Column(String header) {
            this(header, Alignment.LEFT);
        }

        public Column(String header, Alignment alignment) {
            this.header = header;
            this.alignment = alignment;
            this.width = header.length();
        }

        public String formatHeader(String padding) {
            return format(header, padding);
        }

        public String format(String value, String padding) {
            int pad = width - value.length();
            if (pad <= 0) return value;
            return switch (alignment) {
                case RIGHT -> padding.repeat(pad) + value;
                case CENTER -> padding.repeat(pad / 2) + value + padding.repeat(pad - pad / 2);
                default -> value + padding.repeat(pad);
            };
        }

        public String getSeparator(char character) {
            char[] chars = new char[width];
            Arrays.fill(chars, character);
            return new String(chars);
        }

        public void fit(String value) {
            if (value.length() > width) width = value.length();
        }

        public void resetWidth() {
            this.width = header.length();
        }

        public int getWidth() {
            return width;
        }
    }

    public static class Row {
        private final ArrayList<String> values = new ArrayList<>();

        public List<String> format(List<Column> columns, String padding) {
            if (columns.size() != values.size()) {
                throw new IllegalArgumentException("Received wrong amount of columns for table row, expected " + columns.size() + ", received " + values.size() + ".");
            }
            List<String> formatted = new ArrayList<>();
            for (int i = 0; i < values.size(); i++) formatted.add(columns.get(i).format(values.get(i), padding));
            return formatted;
        }
    }

    public enum Alignment {
        LEFT, CENTER, RIGHT
    }
}
