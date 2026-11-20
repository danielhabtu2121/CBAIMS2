package com.bank.cims.service;

import com.bank.cims.dto.NotificationDispatchResult;
import com.bank.cims.model.Notification;
import com.bank.cims.model.SystemParameter;
import com.bank.cims.model.User;
import com.bank.cims.repository.NotificationRepository;
import com.bank.cims.repository.SystemParameterRepository;
import com.bank.cims.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private static final String MASKED_SECRET = "••••••••";

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationTemplateService notificationTemplateService;

    @Autowired
    private OutlookIntegrationService outlookIntegrationService;

    @Autowired
    private SmsIntegrationService smsIntegrationService;

    @Autowired
    private SystemParameterRepository systemParameterRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired @Lazy
    private CimsService cimsService;

    @Transactional
    public Notification sendNotification(
            String channel,
            String templateCode,
            String recipient,
            String recipientName,
            String defaultSubject,
            String defaultBody,
            String entityType,
            String entityId,
            Map<String, Object> context) {

        String effectiveChannel = (channel != null && !channel.isBlank()) ? channel : "Email";
        String effectiveTemplateCode = (templateCode != null && !templateCode.isBlank()) ? templateCode : "GENERAL_ALERT";
        String effectiveRecipient = (recipient != null && !recipient.isBlank()) ? recipient : "alerts@cims-bank.et";
        String effectiveRecipientName = (recipientName != null && !recipientName.isBlank()) ? recipientName : "CIMS Officer";

        Map<String, Object> ctx = context != null ? new HashMap<>(context) : new HashMap<>();
        ctx.putIfAbsent("Customer Name", effectiveRecipientName);
        ctx.putIfAbsent("Maker ID", effectiveRecipientName);
        ctx.putIfAbsent("Entity ID", entityId != null ? entityId : "");

        String subject = defaultSubject != null ? defaultSubject : "CBAIMS Notification";
        String messageBody = defaultBody != null ? defaultBody : "";
        String renderError = null;

        if (notificationTemplateService != null) {
            try {
                Map<String, String> rendered = notificationTemplateService.render(
                        effectiveTemplateCode, effectiveChannel, defaultSubject, defaultBody, entityType, entityId, ctx);
                subject = rendered.getOrDefault("subject", subject);
                messageBody = rendered.getOrDefault("body", messageBody);
            } catch (Exception e) {
                renderError = "Template resolution error: " + e.getMessage();
                log.error("Template rendering failure for {} (Entity {}:{}): {}",
                        effectiveTemplateCode, entityType, entityId, e.getMessage());
            }
        }

        Notification n = new Notification();
        n.setId("notif-" + System.currentTimeMillis() + "-" + new Random().nextInt(1000));
        n.setChannel(effectiveChannel);
        n.setTemplateCode(effectiveTemplateCode);
        n.setRecipient(effectiveRecipient);
        n.setRecipientName(effectiveRecipientName);
        n.setSubject(subject);
        n.setMessageBody(messageBody);
        n.setEntityType(entityType);
        n.setEntityId(entityId);
        n.setReadStatus("UNREAD");
        n.setRetryCount(0);
        n.setSentAt(LocalDateTime.now().toString());

        // Multi-dimensional targeting extraction
        String recipientUserId = (String) ctx.getOrDefault("recipientUserId", ctx.get("targetUserId"));
        String targetRole = (String) ctx.getOrDefault("targetRole", ctx.get("recipientRole"));
        String targetBranch = (String) ctx.getOrDefault("targetBranch", ctx.get("recipientBranch"));
        String targetSegment = (String) ctx.getOrDefault("targetSegment", ctx.get("recipientSegment"));
        boolean bankWide = Boolean.parseBoolean(String.valueOf(ctx.getOrDefault("bankWide", ctx.getOrDefault("isBankWide", false))));

        // Automatic user matching from recipient email/username if recipientUserId is not explicitly provided
        if ((recipientUserId == null || recipientUserId.isBlank()) && effectiveRecipient != null) {
            String candidate = effectiveRecipient.trim();
            User matchedUser = userRepository.findById(candidate)
                    .or(() -> userRepository.findByUsername(candidate))
                    .or(() -> userRepository.findByEmailIgnoreCase(candidate))
                    .orElse(null);
            if (matchedUser == null && candidate.contains("@")) {
                String prefix = candidate.substring(0, candidate.indexOf('@'));
                matchedUser = userRepository.findByUsername(prefix).or(() -> userRepository.findById(prefix)).orElse(null);
            }
            if (matchedUser != null) {
                recipientUserId = matchedUser.getId();
            }
        }

        // Automatic role detection if recipient matches known role mailbox
        if ((targetRole == null || targetRole.isBlank()) && effectiveRecipient != null) {
            String lowerRec = effectiveRecipient.toLowerCase(Locale.ROOT);
            if (lowerRec.contains("brmgr") || lowerRec.contains("branchmanager")) targetRole = "BRMGR";
            else if (lowerRec.contains("cro") || lowerRec.contains("creditrisk")) targetRole = "CRO";
            else if (lowerRec.contains("bro") || lowerRec.contains("relationshipofficer")) targetRole = "BRO";
            else if (lowerRec.contains("srm") || lowerRec.contains("seniorrelationship")) targetRole = "SRM";
            else if (lowerRec.contains("brm")) targetRole = "BRM";
            else if (lowerRec.contains("colldoc")) targetRole = "MGRCOLLDOC";
            else if (lowerRec.contains("compliance")) targetRole = "COMPLIANCE";
        }

        n.setRecipientUserId(recipientUserId);
        n.setTargetRole(targetRole);
        n.setTargetBranch(targetBranch);
        n.setTargetSegment(targetSegment);
        n.setBankWide(bankWide);

        // If template placeholder resolution failed, do not send misleading/unresolved messages
        if (renderError != null) {
            n.setChannelProvider("SMS".equalsIgnoreCase(effectiveChannel)
                    ? smsIntegrationService.getProviderMode()
                    : outlookIntegrationService.getProviderMode());
            n.setStatus("Failed");
            n.setErrorMessage(renderError);
            Notification saved = notificationRepository.save(n);
            log.warn("Saved failed notification [{}] due to template validation error: {}", saved.getId(), renderError);
            return saved;
        }

        NotificationDispatchResult dispatchResult;
        if ("In-App".equalsIgnoreCase(effectiveChannel)) {
            dispatchResult = NotificationDispatchResult.success("INAPP-" + System.currentTimeMillis() + "-" + new Random().nextInt(1000), "INTERNAL_IN_APP", "Sent");
        } else if ("SMS".equalsIgnoreCase(effectiveChannel)) {
            dispatchResult = smsIntegrationService.sendSms(effectiveRecipient, messageBody, entityType, entityId);
        } else {
            // Email / Office 365
            dispatchResult = outlookIntegrationService.sendOutlookEmail(
                    effectiveRecipient, effectiveRecipientName, subject, messageBody, entityType, entityId);
        }

        n.setChannelProvider(dispatchResult.getProvider());
        n.setDeliveryReference(dispatchResult.getDeliveryReference());
        n.setStatus(dispatchResult.getStatus());
        n.setErrorMessage(dispatchResult.getErrorMessage());
        n.setSentAt(dispatchResult.getTimestamp());

        Notification saved = notificationRepository.save(n);
        log.info("Saved notification [{}] - Channel: {} Provider: {} Status: {} Ref: {}",
                saved.getId(), saved.getChannel(), saved.getChannelProvider(), saved.getStatus(), saved.getDeliveryReference());
        return saved;
    }

    public Notification sendNotification(String channel, String templateCode, String recipient, String recipientName, String subject, String messageBody, String entityType, String entityId) {
        return sendNotification(channel, templateCode, recipient, recipientName, subject, messageBody, entityType, entityId, Collections.emptyMap());
    }

    public int getMaxRetries() {
        if (systemParameterRepository == null) return 3;
        return systemParameterRepository.findById("notification_max_retries")
                .map(p -> {
                    try { return Integer.parseInt(p.getParamValue()); } catch (Exception e) { return 3; }
                }).orElse(3);
    }

    @Transactional
    public Notification retryNotification(String notificationId, String operatorId) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found: " + notificationId));

        int maxRetries = getMaxRetries();
        if (n.getRetryCount() >= maxRetries) {
            throw new IllegalStateException("Maximum retry attempts (" + maxRetries + ") reached for notification: " + notificationId);
        }

        n.setRetryCount(n.getRetryCount() + 1);

        NotificationDispatchResult dispatchResult;
        if ("SMS".equalsIgnoreCase(n.getChannel())) {
            dispatchResult = smsIntegrationService.sendSms(n.getRecipient(), n.getMessageBody(), n.getEntityType(), n.getEntityId());
        } else {
            dispatchResult = outlookIntegrationService.sendOutlookEmail(
                    n.getRecipient(), n.getRecipientName(), n.getSubject(), n.getMessageBody(), n.getEntityType(), n.getEntityId());
        }

        n.setChannelProvider(dispatchResult.getProvider());
        n.setDeliveryReference(dispatchResult.getDeliveryReference());
        n.setStatus(dispatchResult.getStatus());
        n.setErrorMessage(dispatchResult.getErrorMessage());
        n.setSentAt(LocalDateTime.now().toString());

        log.info("Retried notification [{}] by operator {} - New status: {}", n.getId(), operatorId, n.getStatus());
        return notificationRepository.save(n);
    }

    public boolean isNotificationAccessibleToUser(Notification n, User u) {
        if (n == null || u == null) return false;
        if (n.isBankWide()) return true;

        // 1. Direct user match by recipientUserId
        if (n.getRecipientUserId() != null && !n.getRecipientUserId().isBlank()) {
            if (n.getRecipientUserId().equalsIgnoreCase(u.getId()) || n.getRecipientUserId().equalsIgnoreCase(u.getUsername())) {
                return true;
            }
        }

        // 2. Direct user match by recipient address / email / username
        if (n.getRecipient() != null && !n.getRecipient().isBlank()) {
            String rec = n.getRecipient().trim();
            if (rec.equalsIgnoreCase(u.getId()) || rec.equalsIgnoreCase(u.getUsername())) {
                return true;
            }
            if (u.getEmail() != null && rec.equalsIgnoreCase(u.getEmail())) {
                return true;
            }
            if (rec.contains("@")) {
                String prefix = rec.substring(0, rec.indexOf('@'));
                if (prefix.equalsIgnoreCase(u.getUsername()) || prefix.equalsIgnoreCase(u.getId())) {
                    return true;
                }
            }
        }

        // 3. Role-based, Branch-based, and Segment-based targeting
        boolean hasTargeting = (n.getTargetRole() != null && !n.getTargetRole().isBlank())
                || (n.getTargetBranch() != null && !n.getTargetBranch().isBlank())
                || (n.getTargetSegment() != null && !n.getTargetSegment().isBlank());

        if (hasTargeting) {
            boolean roleMatch = (n.getTargetRole() == null || n.getTargetRole().isBlank()
                    || n.getTargetRole().equalsIgnoreCase(u.getRole()));
            boolean branchMatch = (n.getTargetBranch() == null || n.getTargetBranch().isBlank()
                    || n.getTargetBranch().equalsIgnoreCase(u.getBranch()));
            boolean segmentMatch = (n.getTargetSegment() == null || n.getTargetSegment().isBlank()
                    || n.getTargetSegment().equalsIgnoreCase(u.getSegment()));

            if (roleMatch && branchMatch && segmentMatch) {
                return true;
            }
        }

        return false;
    }

    @Transactional
    public Notification markAsRead(String notificationId, User user) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found: " + notificationId));
        if (user != null && !isNotificationAccessibleToUser(n, user) && !"SYSADMIN".equalsIgnoreCase(user.getRole())) {
            throw new SecurityException("Access Denied: Notification does not belong to user " + user.getUsername());
        }
        n.setReadStatus("READ");
        n.setReadAt(LocalDateTime.now().toString());
        return notificationRepository.save(n);
    }

    @Transactional
    public Notification markAsRead(String notificationId) {
        return markAsRead(notificationId, null);
    }

    @Transactional
    public int markAllAsReadForUser(User user) {
        if (user == null) return 0;
        List<Notification> list = notificationRepository.findTop500ByOrderBySentAtDesc();
        int updated = 0;
        String now = LocalDateTime.now().toString();
        for (Notification n : list) {
            if (isNotificationAccessibleToUser(n, user) && !"READ".equalsIgnoreCase(n.getReadStatus())) {
                n.setReadStatus("READ");
                n.setReadAt(now);
                notificationRepository.save(n);
                updated++;
            }
        }
        return updated;
    }

    @Transactional
    public int markAllAsRead(String recipient) {
        if (recipient != null && !recipient.isBlank()) {
            User u = userRepository.findById(recipient)
                    .or(() -> userRepository.findByUsername(recipient))
                    .or(() -> userRepository.findByEmailIgnoreCase(recipient))
                    .orElse(null);
            if (u != null) {
                return markAllAsReadForUser(u);
            }
        }
        List<Notification> list = (recipient != null && !recipient.isBlank())
                ? notificationRepository.findByRecipientOrderBySentAtDesc(recipient)
                : notificationRepository.findAll();

        int updated = 0;
        String now = LocalDateTime.now().toString();
        for (Notification n : list) {
            if (!"READ".equalsIgnoreCase(n.getReadStatus())) {
                n.setReadStatus("READ");
                n.setReadAt(now);
                notificationRepository.save(n);
                updated++;
            }
        }
        return updated;
    }

    public long getUnreadCountForUser(User user) {
        if (user == null) return 0;
        return notificationRepository.findTop500ByOrderBySentAtDesc().stream()
                .filter(n -> isNotificationAccessibleToUser(n, user))
                .filter(n -> !"READ".equalsIgnoreCase(n.getReadStatus()))
                .count();
    }

    public long getUnreadCount(String recipient) {
        if (recipient != null && !recipient.isBlank()) {
            User u = userRepository.findById(recipient)
                    .or(() -> userRepository.findByUsername(recipient))
                    .or(() -> userRepository.findByEmailIgnoreCase(recipient))
                    .orElse(null);
            if (u != null) {
                return getUnreadCountForUser(u);
            }
            return notificationRepository.findByRecipientOrderBySentAtDesc(recipient).stream()
                    .filter(n -> !"READ".equalsIgnoreCase(n.getReadStatus()))
                    .count();
        }
        return notificationRepository.countByReadStatus("UNREAD");
    }

    public List<Notification> getNotificationsForUser(User user) {
        return getNotificationsForUser(user, null, null, null, null, 200);
    }

    public List<Notification> getNotificationsForUser(User user, String channel, String status, String recipient, String query, Integer limit) {
        List<Notification> all = notificationRepository.findTop500ByOrderBySentAtDesc();

        return all.stream()
                .filter(n -> user == null || isNotificationAccessibleToUser(n, user))
                .filter(n -> channel == null || "ALL".equalsIgnoreCase(channel) || channel.equalsIgnoreCase(n.getChannel()))
                .filter(n -> status == null || "ALL".equalsIgnoreCase(status) || status.equalsIgnoreCase(n.getStatus()))
                .filter(n -> recipient == null || recipient.isBlank() || (n.getRecipient() != null && n.getRecipient().equalsIgnoreCase(recipient)))
                .filter(n -> {
                    if (query == null || query.isBlank()) return true;
                    String q = query.toLowerCase(Locale.ROOT);
                    return (n.getSubject() != null && n.getSubject().toLowerCase(Locale.ROOT).contains(q)) ||
                            (n.getMessageBody() != null && n.getMessageBody().toLowerCase(Locale.ROOT).contains(q)) ||
                            (n.getRecipient() != null && n.getRecipient().toLowerCase(Locale.ROOT).contains(q)) ||
                            (n.getRecipientName() != null && n.getRecipientName().toLowerCase(Locale.ROOT).contains(q)) ||
                            (n.getEntityId() != null && n.getEntityId().toLowerCase(Locale.ROOT).contains(q));
                })
                .limit(limit != null && limit > 0 ? limit : 200)
                .toList();
    }

    public List<Notification> getNotifications(String channel, String status, String recipient, String query, Integer limit) {
        return getNotificationsForUser(null, channel, status, recipient, query, limit);
    }

    @Transactional
    public void deleteNotification(String notificationId, User user) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found: " + notificationId));
        if (user != null && !isNotificationAccessibleToUser(n, user) && !"SYSADMIN".equalsIgnoreCase(user.getRole())) {
            throw new SecurityException("Access Denied: You do not have permission to delete notification " + notificationId);
        }
        notificationRepository.delete(n);
    }

    @Transactional
    public Map<String, Object> sendAdminTestEmail(String adminUserId, String targetUserId, String subject, String body) {
        User admin = userRepository.findById(adminUserId)
                .or(() -> userRepository.findByUsername(adminUserId))
                .orElseThrow(() -> new IllegalArgumentException("Admin user not found: " + adminUserId));

        if (!"SYSADMIN".equalsIgnoreCase(admin.getRole()) && !"ADMIN".equalsIgnoreCase(admin.getRole())) {
            throw new SecurityException("Access Denied: Only administrators can dispatch administrative test emails.");
        }

        User target = userRepository.findById(targetUserId)
                .or(() -> userRepository.findByUsername(targetUserId))
                .orElseThrow(() -> new IllegalArgumentException("Target recipient user not found: " + targetUserId));

        if (target.getEmail() == null || target.getEmail().isBlank()) {
            throw new IllegalArgumentException("Recipient user " + target.getUsername() + " (" + target.getFullName() + ") does not have a configured email address.");
        }

        String effectiveSubject = (subject != null && !subject.isBlank()) ? subject : "CBAIMS Notification: Test Dispatch to " + target.getFullName();
        String effectiveBody = (body != null && !body.isBlank()) ? body : "Hello " + target.getFullName() + ",\n\nThis is a verified test email dispatched by Administrator " + admin.getFullName() + " (" + admin.getUsername() + ") from the CBAIMS Admin Console.\n\nTimestamp: " + LocalDateTime.now();

        NotificationDispatchResult dispatch = outlookIntegrationService.sendOutlookEmail(
                target.getEmail(), target.getFullName(), effectiveSubject, effectiveBody, "User", target.getId());

        Notification notif = new Notification();
        notif.setId("notif-adm-test-" + System.currentTimeMillis() + "-" + new Random().nextInt(1000));
        notif.setChannel("Email");
        notif.setTemplateCode("ADMIN_TEST_EMAIL");
        notif.setRecipient(target.getEmail());
        notif.setRecipientName(target.getFullName());
        notif.setRecipientUserId(target.getId());
        notif.setSubject(effectiveSubject);
        notif.setMessageBody(effectiveBody);
        notif.setEntityType("User");
        notif.setEntityId(target.getId());
        notif.setStatus(dispatch.getStatus());
        notif.setErrorMessage(dispatch.getErrorMessage());
        notif.setChannelProvider(dispatch.getProvider());
        notif.setDeliveryReference(dispatch.getDeliveryReference());
        notif.setSentAt(dispatch.getTimestamp());
        notif.setReadStatus("UNREAD");
        notificationRepository.save(notif);

        if (cimsService != null) {
            cimsService.logAudit(
                    admin.getUsername(),
                    admin.getRole(),
                    "ADMIN_SEND_TEST_EMAIL",
                    "User",
                    target.getId(),
                    "Status",
                    "",
                    dispatch.getStatus(),
                    "Admin sent test email to " + target.getEmail() + " via " + dispatch.getProvider() + " (Ref: " + dispatch.getDeliveryReference() + ")"
            );
        }

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("success", "Sent".equalsIgnoreCase(dispatch.getStatus()) || "Delivered".equalsIgnoreCase(dispatch.getStatus()));
        resp.put("provider", dispatch.getProvider());
        resp.put("isSimulator", "SIMULATOR".equalsIgnoreCase(dispatch.getProvider()));
        resp.put("status", dispatch.getStatus());
        resp.put("deliveryReference", dispatch.getDeliveryReference());
        resp.put("recipient", target.getEmail());
        resp.put("recipientName", target.getFullName());
        resp.put("recipientUserId", target.getId());
        resp.put("subject", effectiveSubject);
        resp.put("errorMessage", dispatch.getErrorMessage());
        resp.put("timestamp", dispatch.getTimestamp());
        return resp;
    }

    public Map<String, Object> getNotificationStats() {
        List<Notification> all = notificationRepository.findAll();
        long total = all.size();
        long sent = all.stream().filter(n -> "Sent".equalsIgnoreCase(n.getStatus())).count();
        long failed = all.stream().filter(n -> "Failed".equalsIgnoreCase(n.getStatus())).count();
        long email = all.stream().filter(n -> "Email".equalsIgnoreCase(n.getChannel()) || "Outlook".equalsIgnoreCase(n.getChannel())).count();
        long sms = all.stream().filter(n -> "SMS".equalsIgnoreCase(n.getChannel())).count();
        long unread = all.stream().filter(n -> !"READ".equalsIgnoreCase(n.getReadStatus())).count();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", total);
        stats.put("sent", sent);
        stats.put("failed", failed);
        stats.put("pending", total - (sent + failed));
        stats.put("emailCount", email);
        stats.put("smsCount", sms);
        stats.put("unreadCount", unread);
        stats.put("outlookStatus", outlookIntegrationService.isEnabled() ? "READY" : "DISABLED");
        stats.put("smsStatus", smsIntegrationService.isEnabled() ? "READY" : "DISABLED");
        return stats;
    }

    public Map<String, Object> getIntegrationsStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        Map<String, Object> outlookStatus = new LinkedHashMap<>(outlookIntegrationService.testConnection());
        Map<String, Object> smsStatus = new LinkedHashMap<>(smsIntegrationService.testConnection());

        // Standardized normalized fields for consumers
        outlookStatus.putIfAbsent("provider", outlookStatus.get("providerMode"));
        outlookStatus.putIfAbsent("host", outlookStatus.get("smtpHost"));
        outlookStatus.putIfAbsent("port", outlookStatus.get("smtpPort"));
        outlookStatus.putIfAbsent("sender", outlookStatus.get("senderMailbox"));
        outlookStatus.put("connected", "ONLINE".equalsIgnoreCase(String.valueOf(outlookStatus.get("status"))));

        smsStatus.putIfAbsent("provider", smsStatus.get("providerMode"));
        smsStatus.put("connected", "ONLINE".equalsIgnoreCase(String.valueOf(smsStatus.get("status"))));

        status.put("outlook", outlookStatus);
        status.put("email", outlookStatus);
        status.put("sms", smsStatus);
        status.put("timestamp", LocalDateTime.now().toString());
        return status;
    }

    public Map<String, String> getIntegrationConfig() {
        Map<String, String> config = new LinkedHashMap<>();
        List<String> keys = List.of(
                "notification_outlook_enabled",
                "notification_outlook_provider",
                "notification_outlook_sender_email",
                "notification_outlook_smtp_host",
                "notification_outlook_smtp_port",
                "notification_outlook_smtp_starttls",
                "notification_outlook_smtp_username",
                "notification_outlook_smtp_password",
                "notification_outlook_connection_timeout_ms",
                "notification_outlook_read_timeout_ms",
                "notification_outlook_write_timeout_ms",
                "notification_sms_enabled",
                "notification_sms_provider",
                "notification_sms_token_url",
                "notification_sms_api_url",
                "notification_sms_scope",
                "notification_sms_send_for",
                "notification_sms_client_id",
                "notification_sms_client_secret",
                "notification_sms_challenge_algo",
                "notification_sms_phone_format",
                "notification_sms_timeout_ms",
                "notification_sms_max_concat_parts"
        );

        for (String k : keys) {
            systemParameterRepository.findById(k).ifPresent(p -> {
                String val = p.getParamValue();
                if (isSecretKey(k, p.getParamType())) {
                    val = (val != null && !val.isBlank()) ? MASKED_SECRET : "";
                }
                config.put(k, val);
            });
        }

        // Indicate if environment variables are currently supplying credentials
        config.put("env_smtp_credentials_detected", String.valueOf(
                System.getenv("CBAIMS_SMTP_PASSWORD") != null || System.getenv("CBAIMS_EMAIL_PASSWORD") != null));
        config.put("env_sms_credentials_detected", String.valueOf(
                System.getenv("CBAIMS_SMS_CLIENT_SECRET") != null));

        return config;
    }

    @Transactional
    public Map<String, String> saveIntegrationConfig(Map<String, String> updates, String updatedBy) {
        if (updates != null) {
            for (Map.Entry<String, String> entry : updates.entrySet()) {
                String key = entry.getKey();
                String val = entry.getValue();
                if (key != null && key.startsWith("notification_") && val != null) {
                    SystemParameter param = systemParameterRepository.findById(key)
                            .orElse(new SystemParameter(key, val, "Notification Integration Parameter",
                                    isSecretKey(key, null) ? "password" : "string"));

                    // If incoming value is masked secret (••••••••) or empty on a secret, preserve existing stored secret
                    if (isSecretKey(key, param.getParamType())) {
                        if (val.contains("•••") || val.contains("***") || val.isBlank()) {
                            continue; // Preserve stored secret
                        }
                    }

                    param.setParamValue(val);
                    systemParameterRepository.save(param);
                }
            }
        }
        return getIntegrationConfig();
    }

    private boolean isSecretKey(String key, String paramType) {
        if ("password".equalsIgnoreCase(paramType)) return true;
        if (key == null) return false;
        String lower = key.toLowerCase(Locale.ROOT);
        return lower.contains("password") || lower.contains("secret") || lower.contains("api_key");
    }
}
