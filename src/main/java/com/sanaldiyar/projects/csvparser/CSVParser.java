package com.sanaldiyar.projects.csvparser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Hello world!
 *
 */
public class CSVParser {

    public void writeToFile(Collection data, String filename) throws FileNotFoundException {
        PrintWriter pw = new PrintWriter(new File(filename));
        writeToFile(data, pw);
        pw.close();
    }

    public void writeToFile(Collection data, PrintWriter pw) {
        if (!data.iterator().hasNext()) {
            return;
        }

        try {
            Class clazz = data.iterator().next().getClass();
            Field[] declaredFields = clazz.getDeclaredFields();
            for (int i = 0; i < declaredFields.length - 1; i++) {
                pw.print("\"" + declaredFields[i].getName() + "\",");
            }
            pw.println("\"" + declaredFields[declaredFields.length - 1].getName() + "\"");
            pw.flush();

            boolean access;
            for (Object o : data) {
                for (int i = 0; i < declaredFields.length - 1; i++) {
                    access = declaredFields[i].isAccessible();
                    if (!access) {
                        declaredFields[i].setAccessible(true);
                    }
                    Object value = declaredFields[i].get(o);
                    if (value == null) {
                        pw.print("\"\"");
                    } else {
                        pw.print("\"" + value.toString().replace("\"", "\"\"") + "\",");
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
                pw.flush();
            }
            pw.flush();
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(CSVParser.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(CSVParser.class.getName()).log(Level.SEVERE, null, ex);
        }


    }
}
