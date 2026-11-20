package com.bank.cims.service;

import com.bank.cims.model.ReminderSchedule;
import com.bank.cims.repository.ReminderScheduleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.util.*;

@Service
@Transactional
public class ReminderConfigurationService {
    @Autowired private ReminderScheduleRepository repository;
    @Autowired @Lazy private CimsService cimsService;

    @PostConstruct
    public void ensureStandardSchedule() {
        // Standard bank policy reminder schedule:
        // 1) 30 working days before expiry
        ensure("rem-30w", 30, "Working Day", "Before Expiry", "One-time", "Both", "[\"Email\",\"SMS\"]", "[\"CRO\",\"BRO\",\"SRM\",\"BRM\",\"BRMGR\"]");
        // 2) 25 working days before expiry
        ensure("rem-25w", 25, "Working Day", "Before Expiry", "One-time", "Both", "[\"Email\",\"SMS\"]", "[\"CRO\",\"BRO\",\"SRM\",\"BRM\"]");
        // 3) 20 working days before expiry
        ensure("rem-20w", 20, "Working Day", "Before Expiry", "One-time", "Both", "[\"Email\",\"SMS\"]", "[\"CRO\",\"BRO\",\"SRM\",\"BRM\"]");
        // 4) 10 calendar days before expiry
        ensure("rem-10c", 10, "Calendar Day", "Before Expiry", "One-time", "Both", "[\"Email\",\"SMS\"]", "[\"CRO\",\"BRO\",\"SRM\",\"BRM\",\"BRMGR\"]");
        // 5) 5 calendar days after expiry
        ensure("rem-5c-post", 5, "Calendar Day", "After Expiry", "One-time", "Both", "[\"Email\",\"SMS\"]", "[\"CRO\",\"BRO\",\"SRM\",\"BRM\",\"BRMGR\"]");
        // 6) Overdue > 10 days: repeat every 15 days
        ensure("rem-15c-repeat", 10, "Calendar Day", "After Expiry", "Repeat Every 15 Days", "Both", "[\"Email\",\"SMS\"]", "[\"CRO\",\"BRO\",\"SRM\",\"BRM\",\"BRMGR\",\"DISTDIR\"]");
    }

    private void ensure(String id, int offset, String dayType, String triggerBasis, String recurrence, String audience, String channels, String roles) {
        if (repository.findById(id).isEmpty()) {
            ReminderSchedule r = new ReminderSchedule();
            r.setId(id);
            r.setOffsetDays(offset);
            r.setDaysBeforeExpiry(offset);
            r.setDayType(dayType);
            r.setTriggerBasis(triggerBasis);
            r.setRecurrence(recurrence);
            r.setAudience(audience);
            r.setContinueUntil("Renewed");
            r.setChannelsJson(channels);
            r.setRecipientRolesJson(roles);
            r.setActive(true);
            repository.save(r);
        }
    }

    public List<ReminderSchedule> getAll() {
        return repository.findAllByOrderByDaysBeforeExpiryDesc();
    }

    public ReminderSchedule save(ReminderSchedule incoming, String userId) {
        if (incoming == null) {
            throw new IllegalArgumentException("Reminder schedule payload is required.");
        }
        int offset = incoming.getOffsetDays() > 0 ? incoming.getOffsetDays() : incoming.getDaysBeforeExpiry();
        if (offset <= 0 || offset > 3650) {
            throw new IllegalArgumentException("Reminder offset days must be between 1 and 3650.");
        }
        incoming.setOffsetDays(offset);
        incoming.setDaysBeforeExpiry(offset);

        validateChannels(incoming.getChannelsJson());
        validateRoles(incoming.getRecipientRolesJson());

        if (incoming.getDayType() == null || incoming.getDayType().isBlank()) {
            incoming.setDayType("Working Day");
        }
        if (incoming.getTriggerBasis() == null || incoming.getTriggerBasis().isBlank()) {
            incoming.setTriggerBasis("Before Expiry");
        }
        if (incoming.getRecurrence() == null || incoming.getRecurrence().isBlank()) {
            incoming.setRecurrence("One-time");
        }
        if (incoming.getAudience() == null || incoming.getAudience().isBlank()) {
            incoming.setAudience("Both");
        }

        String id = incoming.getId();
        Optional<ReminderSchedule> existing = (id == null || id.isBlank()) ? Optional.empty() : repository.findById(id);

        // Check for duplicate with identical trigger basis, offset days, and day type
        Optional<ReminderSchedule> duplicate = repository.findAll().stream()
                .filter(r -> r.getOffsetDays() == offset &&
                        Objects.equals(r.getTriggerBasis(), incoming.getTriggerBasis()) &&
                        Objects.equals(r.getDayType(), incoming.getDayType()) &&
                        Objects.equals(r.getRecurrence(), incoming.getRecurrence()))
                .findFirst();

        if (duplicate.isPresent() && (id == null || !duplicate.get().getId().equals(id))) {
            throw new IllegalArgumentException("A reminder schedule with the same trigger basis ("
                    + incoming.getTriggerBasis() + "), offset (" + offset + " " + incoming.getDayType()
                    + ") and recurrence already exists.");
        }

        if (id == null || id.isBlank()) {
            incoming.setId("rem-" + (incoming.getTriggerBasis().toLowerCase().contains("after") ? "post-" : "")
                    + offset + ("Working Day".equalsIgnoreCase(incoming.getDayType()) ? "w" : "c")
                    + (incoming.getRecurrence().toLowerCase().contains("repeat") ? "-rep" : ""));
        }

        String old = existing.map(this::state).orElse("<not configured>");
        ReminderSchedule saved = repository.save(incoming);
        cimsService.logAudit(
                userId == null ? "SYSADMIN" : userId,
                "SYSADMIN",
                "SAVE_REMINDER_SCHEDULE",
                "ReminderSchedule",
                saved.getId(),
                "Configuration",
                old,
                state(saved),
                "Reminder schedule updated; runtime scheduler will use it immediately."
        );
        return saved;
    }

    private void validateChannels(String json) {
        List<String> a = parse(json);
        if (a.isEmpty()) throw new IllegalArgumentException("At least one notification channel is required.");
        for (String x : a) {
            if (!x.equalsIgnoreCase("Email") && !x.equalsIgnoreCase("SMS")) {
                throw new IllegalArgumentException("Unsupported reminder channel: " + x);
            }
        }
    }

    private void validateRoles(String json) {
        if (parse(json).isEmpty()) throw new IllegalArgumentException("At least one recipient role is required.");
    }

    private List<String> parse(String json) {
        if (json == null) return List.of();
        String n = json.trim();
        if (n.startsWith("[") && n.endsWith("]")) n = n.substring(1, n.length() - 1);
        if (n.isBlank()) return List.of();
        List<String> r = new ArrayList<>();
        for (String s : n.split(",")) {
            String v = s.trim().replaceAll("^\"|\"$", "");
            if (!v.isBlank()) r.add(v);
        }
        return r;
    }

    private String state(ReminderSchedule s) {
        return "offset=" + s.getOffsetDays() + ", basis=" + s.getTriggerBasis() + ", dayType=" + s.getDayType()
                + ", recurrence=" + s.getRecurrence() + ", channels=" + s.getChannelsJson() + ", active=" + s.isActive();
    }
}
