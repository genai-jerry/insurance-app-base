package com.insurance.email.service;

import com.insurance.common.entity.Lead;
import com.insurance.common.entity.Prospectus;
import com.insurance.common.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for generating email content templates
 */
@Service
@Slf4j
public class EmailTemplateService {

    /**
     * Generate email content for prospectus delivery
     */
    public String generateProspectusEmail(Lead lead, User agent, Prospectus prospectus) {
        StringBuilder body = new StringBuilder();

        body.append("Dear ").append(lead.getName()).append(",\n\n");

        body.append("Thank you for your interest in our insurance solutions. ");
        body.append("I'm pleased to provide you with a personalized insurance proposal ");
        body.append("based on our recent discussion.\n\n");

        body.append("The attached prospectus outlines insurance products that have been ");
        body.append("carefully selected to meet your specific needs and requirements. ");
        body.append("Please review the document at your convenience.\n\n");

        body.append("Key highlights of your proposal:\n");
        body.append("- Customized product recommendations\n");
        body.append("- Detailed coverage information\n");
        body.append("- Competitive pricing options\n");
        body.append("- Clear terms and conditions\n\n");

        body.append("I would be happy to schedule a follow-up call to discuss any questions ");
        body.append("you may have about the proposed insurance solutions. Please feel free ");
        body.append("to reach out at your convenience.\n\n");

        body.append("Best regards,\n");
        body.append(agent.getName()).append("\n");
        body.append(agent.getEmail()).append("\n");

        return body.toString();
    }

    /**
     * Generate default subject for prospectus email
     */
    public String generateProspectusSubject(Lead lead) {
        return "Your Personalized Insurance Proposal - " + lead.getName();
    }

    /**
     * Generate general email template
     */
    public String generateGeneralEmail(Lead lead, User agent, String content) {
        StringBuilder body = new StringBuilder();

        body.append("Dear ").append(lead.getName()).append(",\n\n");
        body.append(content).append("\n\n");
        body.append("Best regards,\n");
        body.append(agent.getName()).append("\n");
        body.append(agent.getEmail()).append("\n");

        return body.toString();
    }

    /**
     * Generate follow-up email
     */
    public String generateFollowUpEmail(Lead lead, User agent) {
        StringBuilder body = new StringBuilder();

        body.append("Dear ").append(lead.getName()).append(",\n\n");

        body.append("I hope this email finds you well. I wanted to follow up on the ");
        body.append("insurance proposal I sent you recently.\n\n");

        body.append("Have you had a chance to review the proposed insurance solutions? ");
        body.append("I would be delighted to answer any questions you may have or ");
        body.append("provide additional information to help you make an informed decision.\n\n");

        body.append("Please let me know if you would like to schedule a call to discuss ");
        body.append("the proposal in more detail. I'm here to help ensure you find the ");
        body.append("best insurance coverage for your needs.\n\n");

        body.append("Looking forward to hearing from you.\n\n");

        body.append("Best regards,\n");
        body.append(agent.getName()).append("\n");
        body.append(agent.getEmail()).append("\n");

        return body.toString();
    }

    /**
     * Generate HTML email template
     */
    public String generateHtmlEmail(Lead lead, User agent, String content) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body {
                        font-family: Arial, sans-serif;
                        line-height: 1.6;
                        color: #333;
                    }
                    .container {
                        max-width: 600px;
                        margin: 0 auto;
                        padding: 20px;
                    }
                    .header {
                        background-color: #3498db;
                        color: white;
                        padding: 20px;
                        text-align: center;
                    }
                    .content {
                        padding: 20px;
                        background-color: #f9f9f9;
                    }
                    .footer {
                        background-color: #2c3e50;
                        color: white;
                        padding: 15px;
                        text-align: center;
                        font-size: 12px;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h2>Insurance Proposal</h2>
                    </div>
                    <div class="content">
                        <p>Dear %s,</p>
                        %s
                        <p>Best regards,<br>
                        %s<br>
                        %s</p>
                    </div>
                    <div class="footer">
                        <p>This email contains confidential information. If you received this in error, please delete it.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(lead.getName(), content, agent.getName(), agent.getEmail());
    }
}
