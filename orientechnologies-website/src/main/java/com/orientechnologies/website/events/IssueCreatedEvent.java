package com.orientechnologies.website.events;

import com.orientechnologies.website.repository.IssueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring4.SpringTemplateEngine;

import reactor.event.Event;

import com.orientechnologies.website.configuration.AppConfig;
import com.orientechnologies.website.model.schema.dto.Issue;
import com.orientechnologies.website.model.schema.dto.OUser;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Enrico Risa on 30/12/14.
 */
@Component
public class IssueCreatedEvent extends EventInternal<Issue> {

  @Autowired
  @Lazy
  protected JavaMailSenderImpl sender;

  @Autowired
  protected AppConfig          config;

  @Autowired
  private IssueRepository      issueRepository;

  @Autowired
  private SpringTemplateEngine templateEngine;

  public static String         EVENT = "issue_created";

  @Override
  public String event() {
    return EVENT;
  }

  @Override
  public void accept(Event<Issue> issueEvent) {

    Issue issue = issueEvent.getData();
    Context context = new Context();
    fillContextVariable(context, issue);
    String htmlContent = templateEngine.process("newIssue.html", context);
    SimpleMailMessage mailMessage = new SimpleMailMessage();
    OUser owner = issue.getScope().getOwner();
    OUser user = issue.getUser();

    List<String> dests = new ArrayList<String>();
    List<OUser> members = issue.getScope().getMembers();
    if (owner != null) {
      boolean found = false;
      for (OUser member : members) {
        if (member.getName().equals(owner.getName())) {
          found = true;
        }
      }
      if (!found && !user.getName().equals(owner.getName())) {
        members.add(owner);
      }
    }

    for (OUser member : members) {
      if (Boolean.TRUE.equals(member.getNotification())) {
        if (member.getEmail() != null && !member.getEmail().isEmpty())
          dests.add(member.getEmail());
        else if (member.getWorkingEmail() != null && !member.getWorkingEmail().isEmpty())
          dests.add(member.getWorkingEmail());

      }
    }
    if (dests.size() > 0) {
      mailMessage.setTo(dests.toArray(new String[dests.size()]));
      mailMessage.setFrom("prjhub@orientechnologies.com");
      mailMessage.setSubject(issue.getTitle());
      mailMessage.setText(htmlContent);
      sender.send(mailMessage);
    }
  }

  private void fillContextVariable(Context context, Issue issue) {
    context.setVariable("link", config.endpoint + "/#issues/" + issue.getIid());
    String body = null;
    if (issue.getBody() != null) {
      body = issue.getBody();
    } else {
      body = "@" + issue.getUser().getName() + " opened issue #" + issue.getIid();
    }
    context.setVariable("body", body);
  }
}
