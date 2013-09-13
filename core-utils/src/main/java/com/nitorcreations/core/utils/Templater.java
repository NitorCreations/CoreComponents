package com.nitorcreations.core.utils;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map.Entry;

import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
public class Templater {
    
    public static void main( String[] args ) {
        if (args.length < 1) {
            System.err.println("usage: Templater template");
            System.exit(1);
        }
        File template = new File(args[0]);
        try {
            applyTemplate(template, new PrintWriter(System.out));
        } catch (IOException e) {
            System.err.println("usage: Templater template");
            System.err.println("   " + e.getMessage());
            System.exit(1);
        } 
    }

    public static void applyTemplate(File template, PrintWriter w) throws IOException {
        if (template == null) {
            throw new IllegalArgumentException("Template must not be null");
        }
        if (w == null) {
            throw new IllegalArgumentException("Writer must not be null");
        }
        if (!template.exists()) {
            throw new IOException("File " + template.getAbsolutePath() + " does not exist.");
        }
        if (!template.canRead()) {
            throw new IOException("File " + template.getAbsolutePath() + " is not readable.");
        }
        VelocityEngine ve = new VelocityEngine();
        ve.setProperty("resource.loader", "file");
        ve.setProperty("file.resource.loader.class", "org.apache.velocity.runtime.resource.loader.FileResourceLoader");
        ve.setProperty("file.resource.loader.path", "");
        ve.init();
        Template t = ve.getTemplate(template.getAbsolutePath());
        VelocityContext context = new VelocityContext();

        for (Entry<String, String> next : System.getenv().entrySet()) {
            context.put(next.getKey(), next.getValue());
        }

        for (Entry<Object, Object> next : System.getProperties().entrySet()) {
            context.put((String)next.getKey(), (String)next.getValue());
        }

        t.merge( context, w);
        w.flush();
    }
}
