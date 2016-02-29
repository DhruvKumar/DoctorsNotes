package com.hortonworks.example;

import org.junit.Test;
import static org.junit.Assert.*;

public class MainTest {


    @Test
    public void testMain() {
        Main.main(new String[]{"1","./target"});
    }

    @Test
    public void testDoctorNote() throws Exception {
        String note = new Main().getRandomDoctorsNote();
        assertNotNull(note);
        System.out.println(note);


    }
}