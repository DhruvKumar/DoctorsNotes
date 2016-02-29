package com.hortonworks.example;

import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.parser.CanonicalModelClassFactory;
import ca.uhn.hl7v2.parser.Parser;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Creates very simple MDM^T02 HL7 messages with fake data for every field
 */
public class Main {
    private Random r;

    public Main() {
        r = new Random();

        try {
            URI fs = Main.class.getClassLoader().getResource("names.txt").toURI();
            if (fs.getScheme().contains("jar")) {
                Map<String, String> env = new HashMap<String, String>() {{
                    put("create", "true");
                }};

                FileSystems.newFileSystem(fs, env);
            }
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {

        int count = 1;
        if (args.length > 0) {
            count = Integer.parseInt(args[0]);
        }
        String destDir = "/tmp/hl7-messages/";
        if (args.length > 1) {
            destDir = args[1];
        }
        new Main().generateMessages(count, destDir);
    }

    private void generateMessages(int count, String destDir) {
        for (int i = 1; i <= count; i++) {
            String name = getRandomName();
            String m = "MSH|^~\\&|||||" + formatDate(new Date(), "YYYYMMddHHmmss.SSSZ") + "||MDM^T02|" + r.nextInt(100) + "|P|2.3\r" +
                    "EVN|T02|" + formatDate(new Date(), "YYYYMMddHHmm") + "\r" +
                    "PID|1||" + generateRandomSSN() + "||" + name + "||" + formatDate(randomBirthday(), "YYYYMMdd") + "|F\r" +
                    "PV1|1|O|" + getRandomDepartment() + "^" + r.nextInt(100) + "^" + r.nextInt(100) + "\r" +
                    "TXA|1|CN|TX|" + formatDate(getRecentDate(), "YYYYMMddHHmmss") + "||||||||DOC-ID-1000" + i + "|||||AU||AV\r" +
                    "OBX|" + i + "|TX|100" + i + "^Reason For Visit: |" + i + "|" + getRandomDoctorsNote() + "||||||F";
            HapiContext context = new DefaultHapiContext();
            CanonicalModelClassFactory mcf = new CanonicalModelClassFactory("2.3");
            context.setModelClassFactory(mcf);

          /*
           * A Parser is used to convert between string representations of messages and instances of
           * HAPI's "Message" object. In this case, we are using a "GenericParser", which is able to
           * handle both XML and ER7 (pipe & hat) encodings.
           */
            Parser p = context.getGenericParser();

            try {
                p.parse(m);
            } catch (HL7Exception e) {
                e.printStackTrace();
                continue;
            }

            Path fn = Paths.get(destDir, name.replace("^", "_") + ".txt");
            try (BufferedWriter writer = Files.newBufferedWriter(fn)) {
                writer.write(m);
            } catch (IOException e) {
                e.printStackTrace();
                continue;
            }

            System.out.println("wrote message " + i + " to " + fn.toString());
        }

    }

    /*
    reads from symptoms.txt and combines with random sentence beginnings and sentence endings
     */
    String getRandomDoctorsNote() {
        String[] symptoms = {"abnormal_gait"};
        String[] pre = {
                "The patient complained of ",
                "Reason for visit was ",
                "Evaluated patient for ",
                "Visited me because of ",
                "Started treatment for "};
        String[] post = {
                ".  Performed tests. ",
                ". ",
                ". Prescribed medication. ",
                ". Drew blood. ",
                ". Discharged. ",
                ". Admitted to hospital. ",
                ". Recommended adjustments. ",
                ". Discussed changes. "};

        try {
            URI filePath = getClass().getClassLoader().getResource("symptoms.txt").toURI();
            symptoms = Files.lines(Paths.get(filePath)).toArray(String[]::new);
        } catch (URISyntaxException | IOException e) {
            e.printStackTrace();
        }
        return pre[r.nextInt(pre.length)] + symptoms[r.nextInt(symptoms.length)].toLowerCase() + post[r.nextInt(post.length)];
    }

    /* return a date within the past 60 months */
    private Date getRecentDate() {
        Calendar cal = new GregorianCalendar();
        cal.set(Calendar.MONTH, cal.get(Calendar.MONTH) - r.nextInt(60));
        cal.set(Calendar.DAY_OF_MONTH, cal.get(Calendar.DAY_OF_MONTH) - r.nextInt(32));
        return cal.getTime();
    }

    private String getRandomDepartment() {
        String[] lines = {"ER"};
        try {
            URI filePath = getClass().getClassLoader().getResource("departments.txt").toURI();
            lines = Files.lines(Paths.get(filePath)).toArray(String[]::new);
        } catch (URISyntaxException | IOException e) {
            e.printStackTrace();
        }
        return lines[r.nextInt(lines.length)];
    }

    private String generateRandomSSN() {
        return String.format("%03d%02d%04d", r.nextInt(1000), r.nextInt(100), r.nextInt(10000));
    }

    /*
    generate a random birthday for someone that is 18-99 years old
     */
    private Date randomBirthday() {
        Calendar cal = new GregorianCalendar();
        cal.set(Calendar.YEAR, cal.get(Calendar.YEAR) - 18 - r.nextInt(80));
        cal.set(Calendar.MONTH, r.nextInt(12));
        cal.set(Calendar.DAY_OF_MONTH, r.nextInt(32));
        return cal.getTime();
    }

    private String getRandomName() {
        String[] lines = {"gibson^hannah"};
        try {
            URI filePath = getClass().getClassLoader().getResource("names.txt").toURI();
            lines = Files.lines(Paths.get(filePath)).toArray(String[]::new);
        } catch (URISyntaxException | IOException e) {
            e.printStackTrace();
        }
        return lines[r.nextInt(lines.length)] + "^";
    }

    private String formatDate(Date date, String format) {
        return new SimpleDateFormat(format).format(date);
    }
}
