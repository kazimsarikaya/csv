package com.sanaldiyar.projects.csvparser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Hello world!
 *
 */
public class CSVParser<T> {

    private class CSVReaderIterator implements Iterator<T> {

        private Scanner scanner;
        private Field[] declaredFields;
        private Map<String, Integer> fieldMap;

        public boolean hasNext() {
            return scanner.hasNextLine();
        }

        public T next() {
            try {
                if (!hasNext()) {
                    return null;
                }
                String line = scanner.nextLine();
                T item = buildClassFromLine(declaredFields, fieldMap, parseLine(line));
                return item;
            } catch (InstantiationException ex) {
                Logger.getLogger(CSVParser.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(CSVParser.class.getName()).log(Level.SEVERE, null, ex);
            }
            return null;
        }
    }

    public class CSVReader implements Iterable<T>, AutoCloseable {

        private Scanner scanner;
        private boolean started = false;
        private CSVReaderIterator itr;

        public Iterator<T> iterator() {
            if (!started) {
                throw new UnsupportedOperationException("Cannot get iterator without begin command");
            }
            return itr;
        }

        public void begin() {
            itr = new CSVReaderIterator();
            itr.scanner = scanner;
            itr.declaredFields = clazz.getDeclaredFields();
            String header;
            do {
                header = scanner.nextLine();
            } while (header.length() == 0);
            itr.fieldMap = buildFieldMap(itr.declaredFields, parseLine(header));
            started = true;
        }

        public void close() throws Exception {
            scanner.close();
        }
    }

    public class CSVWriter implements AutoCloseable {

        private PrintWriter pw;
        private boolean started;
        private Field[] declaredFields;

        public void begin() {
            declaredFields = clazz.getDeclaredFields();
            for (int i = 0; i < declaredFields.length - 1; i++) {
                pw.print("\"" + declaredFields[i].getName() + "\"" + delimeter);
            }
            pw.println("\"" + declaredFields[declaredFields.length - 1].getName() + "\"");
            pw.flush();
        }

        public void append(T item) {
            try {
                if (!started) {
                    throw new UnsupportedOperationException("Cannot append without begin command");
                }
                writeItem(pw, declaredFields, pw);
                pw.flush();
            } catch (IllegalArgumentException ex) {
                Logger.getLogger(CSVParser.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(CSVParser.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        public void close() throws Exception {
            pw.close();
        }

    }

    public enum Delimeter {

        COMMA, TAB, SEMICOLON;

        @Override
        public String toString() {
            String res = "";
            switch (this) {
                case COMMA:
                    res = ",";
                    break;
                case TAB:
                    res = "\t";
                    break;
                case SEMICOLON:
                    res = ";";
                    break;
            }
            return res;
        }
    }
    private Class<T> clazz;
    private Delimeter delimeter;

    public CSVParser(Class<T> clazz) {
        this(clazz, Delimeter.COMMA);
    }

    public CSVParser(Class<T> clazz, Delimeter delimeter) {
        this.clazz = clazz;
        this.delimeter = delimeter;
    }

    public void writeToFile(Collection<T> data, String filename) throws FileNotFoundException {
        PrintWriter pw = null;
        try {
            pw = new PrintWriter(new File(filename), "utf-8");
            writeToFile(data, pw);
            pw.close();
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(CSVParser.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            pw.close();
        }
    }

    public void writeToFile(Collection<T> data, PrintWriter pw) {
        if (!data.iterator().hasNext()) {
            return;
        }

        try {
            Field[] declaredFields = clazz.getDeclaredFields();
            for (int i = 0; i < declaredFields.length - 1; i++) {
                pw.print("\"" + declaredFields[i].getName() + "\"" + this.delimeter);
            }
            pw.println("\"" + declaredFields[declaredFields.length - 1].getName() + "\"");
            pw.flush();

            for (Object o : data) {
                writeItem(pw, declaredFields, o);
                pw.flush();
            }
            pw.flush();
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(CSVParser.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(CSVParser.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public CSVWriter getCSVWriter(String filename) throws FileNotFoundException {
        PrintWriter pw;
        try {
            pw = new PrintWriter(filename, "utf-8");
            return getCSVWriter(pw);
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(CSVParser.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public CSVWriter getCSVWriter(PrintWriter pw) {
        CSVWriter writer = new CSVWriter();
        writer.pw = pw;
        return writer;
    }

    private void writeItem(PrintWriter pw, Field[] declaredFields, Object o) throws IllegalArgumentException, IllegalAccessException {
        boolean access;
        for (int i = 0; i < declaredFields.length - 1; i++) {
            access = declaredFields[i].isAccessible();
            if (!access) {
                declaredFields[i].setAccessible(true);
            }
            Object value = declaredFields[i].get(o);
            if (value == null) {
                pw.print("\"\"" + this.delimeter);
            } else {
                pw.print("\"" + value.toString().replace("\"", "\"\"") + "\"" + this.delimeter);
            }
            if (!access) {
                declaredFields[i].setAccessible(false);
            }
        }
        access = declaredFields[declaredFields.length - 1].isAccessible();
        if (!access) {
            declaredFields[declaredFields.length - 1].setAccessible(true);
        }
        Object value = declaredFields[declaredFields.length - 1].get(o);
        if (value == null) {
            pw.println("\"\"");
        } else {
            pw.println("\"" + value.toString().replace("\"", "\"\"") + "\"");
        }

        if (!access) {
            declaredFields[declaredFields.length - 1].setAccessible(false);
        }
    }

    public Collection<T> readFromFile(String fileName) throws FileNotFoundException {
        Collection<T> ret;
        Scanner scanner = new Scanner(new File(fileName), "utf-8");
        ret = readFromFile(scanner);
        scanner.close();
        return ret;
    }

    public Collection<T> readFromFile(Scanner scanner) {
        try {
            Field[] declaredFields = clazz.getDeclaredFields();
            String header;
            do {
                header = scanner.nextLine();
            } while (header.length() == 0);
            Map<String, Integer> fieldMap = buildFieldMap(declaredFields, parseLine(header));

            String line;
            T item;

            List<T> ret = new LinkedList<T>();

            while (scanner.hasNextLine()) {
                do {
                    line = scanner.nextLine();
                } while (line.length() == 0 && scanner.hasNextLine());
                if (line.length() == 0) {
                    break;
                }
                String[] parsedLine = parseLine(line);
                item = buildClassFromLine(declaredFields, fieldMap, parsedLine);
                ret.add(item);
            }

            return ret;
        } catch (InstantiationException ex) {
            Logger.getLogger(CSVParser.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(CSVParser.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public CSVReader getCSVReader(String fileName) throws FileNotFoundException {
        Scanner scanner = new Scanner(new File(fileName), "utf-8");
        CSVReader reader = getCSVReader(scanner);
        scanner.close();
        return reader;
    }

    public CSVReader getCSVReader(Scanner scanner) {
        CSVReader reader = new CSVReader();
        reader.scanner = scanner;
        return reader;
    }

    private T buildClassFromLine(Field[] declaredFields, Map<String, Integer> fieldMap, String[] parsedLine) throws InstantiationException, IllegalAccessException {
        boolean access;
        T item = clazz.newInstance();

        for (int i = 0; i < declaredFields.length; i++) {
            access = declaredFields[i].isAccessible();
            if (!access) {
                declaredFields[i].setAccessible(true);
            }

            String value = parsedLine[fieldMap.get(declaredFields[i].getName()).intValue()];
            Class<?> type = declaredFields[i].getType();
            if (type.isPrimitive()) {

                if (type.isAssignableFrom(boolean.class)) {
                    declaredFields[i].set(item, Boolean.valueOf(value));
                } else if (type.isAssignableFrom(byte.class)) {
                    declaredFields[i].set(item, Integer.valueOf(value).byteValue());
                } else if (type.isAssignableFrom(char.class)) {
                    declaredFields[i].set(item, value.charAt(0));
                } else if (type.isAssignableFrom(double.class)) {
                    declaredFields[i].set(item, Double.valueOf(value).doubleValue());
                } else if (type.isAssignableFrom(float.class)) {
                    declaredFields[i].set(item, Float.valueOf(value).floatValue());
                } else if (type.isAssignableFrom(int.class)) {
                    declaredFields[i].set(item, Integer.valueOf(value).intValue());
                } else if (type.isAssignableFrom(long.class)) {
                    declaredFields[i].set(item, Long.valueOf(value).longValue());
                } else if (type.isAssignableFrom(short.class)) {
                    declaredFields[i].set(item, Short.valueOf(value).shortValue());
                }

            } else {
                declaredFields[i].set(item, value);
            }

            if (!access) {
                declaredFields[declaredFields.length - 1].setAccessible(false);
            }
        }

        return item;
    }

    private Map<String, Integer> buildFieldMap(Field[] fields, String[] headers) {
        Map<String, Integer> ret = new HashMap<String, Integer>();
        for (int i = 0; i < fields.length; i++) {
            for (int j = 0; j < headers.length; j++) {
                if (fields[i].getName().equals(headers[j])) {
                    ret.put(fields[i].getName(), j);
                }
            }
        }
        return ret;
    }

    private String[] parseLine(String line) {
        List<String> ret = new LinkedList<String>();
        char c;
        StringBuilder sb = null;
        boolean incol = false;
        boolean border = false;

        for (int i = 0; i < line.length(); i++) {
            c = line.charAt(i);

            if (c == '"') {
                if ((i + 1) != line.length()) {
                    if (line.charAt(i + 1) == '"') {
                        sb.append("\"");
                        i++;
                    } else {
                        if (incol) {
                            ret.add(sb.toString());
                        } else {
                            border = true;
                            sb = new StringBuilder();
                        }
                        incol = !incol;
                    }
                }
            } else if (c == this.delimeter.toString().charAt(0)) {
                if (border) {
                    if (incol) {
                        sb.append(this.delimeter.toString());
                    }
                } else {
                    ret.add(sb.toString());
                    sb = new StringBuilder();
                    incol = false;
                }

            } else {
                if (!incol) {
                    incol = true;
                    border = false;
                    sb = new StringBuilder();
                }
                sb.append(c);
            }
        }
        if (incol) {
            ret.add(sb.toString());
        }

        return ret.toArray(new String[ret.size()]);
    }
}
