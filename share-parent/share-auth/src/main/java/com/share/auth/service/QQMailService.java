package com.share.auth.service;

import java.util.Properties;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * QQ 邮箱 SMTP 邮件发送服务。
 *
 * <p>直接使用 Java 语言自带的 SMTP 协议与 Jakarta Mail，通过 SSL (465端口)
 * 直连腾讯 QQ 邮箱服务器（smtp.qq.com）投递验证码邮件。</p>
 */
@Service
public class QQMailService {

    private static final Logger log = LoggerFactory.getLogger(QQMailService.class);

    @Value("${qq.mail.host:smtp.qq.com}")
    private String host;

    @Value("${qq.mail.port:465}")
    private int port;

    @Value("${qq.mail.username:${QQ_MAIL_USERNAME:}}")
    private String username;

    @Value("${qq.mail.password:${QQ_MAIL_PASSWORD:}}")
    private String password;

    @Value("${qq.mail.sender:${QQ_MAIL_SENDER:}}")
    private String sender;

    /**
     * 发送 QQ 邮箱注册验证码。
     *
     * @param toEmail 收件人 QQ 邮箱地址
     * @param code    6 位验证码
     * @return 是否通过真实 SMTP 成功送达
     */
    public boolean sendVerificationCode(String toEmail, String code) {
        String mailUser = (username != null && !username.isBlank()) ? username.trim() : "";
        String mailPass = (password != null && !password.isBlank()) ? password.trim() : "";
        String mailSender = (sender != null && !sender.isBlank()) ? sender.trim() : mailUser;

        if (mailUser.isEmpty() || mailPass.isEmpty()) {
            log.warn("【QQ邮箱SMTP】未配置发件人或16位授权码（qq.mail.username / qq.mail.password），进入开发调试兜底模式。收件人: {}, 验证码: {}", toEmail, code);
            return false;
        }

        try {
            JavaMailSenderImpl mailSenderImpl = new JavaMailSenderImpl();
            mailSenderImpl.setHost(host != null && !host.isBlank() ? host.trim() : "smtp.qq.com");
            mailSenderImpl.setPort(port > 0 ? port : 465);
            mailSenderImpl.setUsername(mailUser);
            mailSenderImpl.setPassword(mailPass);
            mailSenderImpl.setDefaultEncoding("UTF-8");

            Properties props = mailSenderImpl.getJavaMailProperties();
            props.put("mail.transport.protocol", "smtp");
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.ssl.required", "true");
            props.put("mail.smtp.socketFactory.port", String.valueOf(mailSenderImpl.getPort()));
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.socketFactory.fallback", "false");
            props.put("mail.smtp.timeout", "8000");
            props.put("mail.smtp.connectiontimeout", "8000");
            props.put("mail.smtp.writetimeout", "8000");

            MimeMessage message = mailSenderImpl.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(mailSender, "智问学伴教育平台");
            helper.setTo(toEmail.trim());
            helper.setSubject("【智问学伴】账号注册验证码");

            String htmlContent = buildEmailTemplate(code);
            helper.setText(htmlContent, true);

            mailSenderImpl.send(message);
            log.info("【QQ邮箱SMTP】验证码邮件已成功投递至: {}", toEmail);
            return true;
        } catch (Exception ex) {
            log.error("【QQ邮箱SMTP】发送邮件至 {} 失败（username: {}）: {}", toEmail, mailUser, ex.getMessage());
            // 若包含前缀别名且认证失败，自动尝试去除前缀纯数字账号重试
            if (mailUser.matches("^a\\d+@qq\\.com$")) {
                String numericUser = mailUser.substring(1);
                try {
                    log.info("【QQ邮箱SMTP】尝试使用纯数字账号重试认证: {}", numericUser);
                    JavaMailSenderImpl retrySender = new JavaMailSenderImpl();
                    retrySender.setHost(host != null && !host.isBlank() ? host.trim() : "smtp.qq.com");
                    retrySender.setPort(port > 0 ? port : 465);
                    retrySender.setUsername(numericUser);
                    retrySender.setPassword(mailPass);
                    retrySender.setDefaultEncoding("UTF-8");

                    Properties props = retrySender.getJavaMailProperties();
                    props.put("mail.transport.protocol", "smtp");
                    props.put("mail.smtp.auth", "true");
                    props.put("mail.smtp.ssl.enable", "true");
                    props.put("mail.smtp.ssl.required", "true");
                    props.put("mail.smtp.socketFactory.port", String.valueOf(retrySender.getPort()));
                    props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
                    props.put("mail.smtp.socketFactory.fallback", "false");
                    props.put("mail.smtp.timeout", "8000");
                    props.put("mail.smtp.connectiontimeout", "8000");

                    MimeMessage retryMsg = retrySender.createMimeMessage();
                    MimeMessageHelper helper = new MimeMessageHelper(retryMsg, true, "UTF-8");
                    helper.setFrom(mailSender, "智问学伴教育平台");
                    helper.setTo(toEmail.trim());
                    helper.setSubject("【智问学伴】账号注册验证码");
                    helper.setText(buildEmailTemplate(code), true);

                    retrySender.send(retryMsg);
                    log.info("【QQ邮箱SMTP】重试发送成功，验证码邮件已送达: {}", toEmail);
                    return true;
                } catch (Exception retryEx) {
                    log.error("【QQ邮箱SMTP】重试发送失败: {}", retryEx.getMessage());
                }
            }
            return false;
        }
    }

    private String buildEmailTemplate(String code) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="UTF-8">
              <title>智问学伴 注册验证码</title>
            </head>
            <body style="margin: 0; padding: 30px; background-color: #F8FAFC; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;">
              <div style="max-width: 540px; margin: 0 auto; background: #FFFFFF; border-radius: 12px; border: 1px solid #E2E8F0; padding: 36px 32px; box-shadow: 0 4px 16px rgba(15, 23, 42, 0.05);">
                <div style="display: flex; align-items: center; margin-bottom: 24px;">
                  <div style="width: 40px; height: 40px; background-color: #2563EB; border-radius: 8px; color: #FFFFFF; font-size: 20px; font-weight: bold; line-height: 40px; text-align: center; margin-right: 12px;">智</div>
                  <div>
                    <h2 style="margin: 0; font-size: 20px; color: #0F172A;">智问学伴 · 在线教育平台</h2>
                    <p style="margin: 2px 0 0 0; font-size: 13px; color: #64748B;">智能驱动，高效成长</p>
                  </div>
                </div>
                <hr style="border: none; border-top: 1px solid #E2E8F0; margin: 20px 0;" />
                <p style="font-size: 15px; color: #334155; line-height: 1.6;">尊敬的学员，您好：</p>
                <p style="font-size: 14px; color: #475569; line-height: 1.6;">您正在申请注册智问学伴账号，本次操作的邮箱验证码为：</p>
                <div style="margin: 28px 0; text-align: center;">
                  <div style="display: inline-block; padding: 14px 36px; background-color: #EFF6FF; border: 1px dashed #2563EB; border-radius: 8px; font-size: 32px; font-weight: 700; letter-spacing: 8px; color: #2563EB;">
                    %s
                  </div>
                </div>
                <p style="font-size: 13px; color: #64748B; line-height: 1.6;">
                  • 验证码有效期为 <strong>5 分钟</strong>，请尽快完成注册。<br>
                  • 如果这不是您的操作，请忽略此邮件，您的账号安全不会受到影响。<br>
                  • 请勿将验证码泄露给任何人。
                </p>
                <hr style="border: none; border-top: 1px solid #E2E8F0; margin: 28px 0 16px 0;" />
                <p style="font-size: 12px; color: #94A3B8; text-align: center; margin: 0;">此邮件为系统自动发出，请勿直接回复。</p>
              </div>
            </body>
            </html>
            """.formatted(code);
    }
}
