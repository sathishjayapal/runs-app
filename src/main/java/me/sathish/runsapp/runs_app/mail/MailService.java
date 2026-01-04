package me.sathish.runsapp.runs_app.mail;


public interface MailService {

    void sendMail(String mailTo, String subject, String html);

}
