package com.signomix.scheduler.app.logic;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.util.ArrayList;

import org.jboss.logging.Logger;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import jakarta.inject.Inject;

/**
 * This is usable only to run command inside docker container.
 */
public class SystemCommandJob extends Job implements org.quartz.Job {

    @Inject
    Logger logger;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        Long id = (Long) context.getMergedJobDataMap().get("id");
        //ArrayList<String> commands = new ArrayList<>();
        String command = (String) context.getMergedJobDataMap().get("command");
        if (command == null || command.isEmpty()) {
            logger.error("No command defined for system command job " + id);
            return;
        }
        //commands.add(command);
        ArrayList<String> cmdArray = new ArrayList<>();
        cmdArray.add(command);
        String option;
        int i = 0;
        do {
            option = (String) context.getMergedJobDataMap().get("option" + i);
            if (option != null && !option.isEmpty()) {
                cmdArray.add(option);
            }
            i++;
        } while (option != null && !option.isEmpty());
        String[] options = new String[cmdArray.size()];
        for (int j = 0; j < cmdArray.size(); j++) {
            options[j] = cmdArray.get(j);
        }

        Runtime rt = Runtime.getRuntime();
        Process proc=null;
        try {
            proc = rt.exec(options);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        BufferedReader stdError = new BufferedReader(new InputStreamReader(proc.getErrorStream()));

        // Read the output from the command
        System.out.println("Here is the standard output of the command:\n");
        String s = null;
        try {
            while ((s = stdInput.readLine()) != null) {
                System.out.println(s);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Read any errors from the attempted command
        System.out.println("Here is the standard error of the command (if any):\n");
        try {
            while ((s = stdError.readLine()) != null) {
                System.out.println(s);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
