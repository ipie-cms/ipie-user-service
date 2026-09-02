package in.gov.ipie.service.user.consent;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import in.gov.ipie.service.user.domain.NotificationChannel;
import in.gov.ipie.common.utils.id.IdGenerator;

/**
 * Writes the consent ledger that sits beside {@code users.notification_channels}.
 *
 * <p>The column says what the platform should do today; these rows say what the person agreed to,
 * when, and against which version of the notice. DPDP s.6(10) puts the burden of proving consent on
 * the Data Fiduciary, and a column overwritten on every edit destroys that proof each time it is
 * changed.
 *
 * <p><b>The diff is the whole job.</b> A channel newly present is a grant; one newly absent is a
 * withdrawal; one present both before and after is untouched - re-recording it would invent a fresh
 * decision the person never made, and make the history unreadable. Withdrawal never deletes: the row
 * stays so that the period during which messages legitimately went out stays accounted for.
 */
@Component
public class ConsentRecorder {

    /** The notice these consents are given against - {@code V34} seeds version 1. */
    public static final String NOTIFICATION_CHANNELS_NOTICE = "NOTIFICATION_CHANNELS";

    private static final Logger LOG = LoggerFactory.getLogger(ConsentRecorder.class);

    private final ConsentRepository consentRepository;
    private final ConsentNoticeRepository noticeRepository;

    public ConsentRecorder(ConsentRepository consentRepository, ConsentNoticeRepository noticeRepository) {
        this.consentRepository = consentRepository;
        this.noticeRepository = noticeRepository;
    }

    /**
     * Records the first decision a person makes, at registration.
     *
     * @param channels what they chose; each becomes its own row, because s.7 lets them withdraw one
     *     without the others
     */
    public void recordInitialConsent(UUID userId, Set<NotificationChannel> channels) {
        applyChange(userId, Set.of(), channels, ConsentSource.REGISTRATION);
    }

    /**
     * Records a later change, as the difference between two sets.
     *
     * @param previous what was consented to before this call
     * @param current what is consented to after it
     */
    public void recordChange(UUID userId, Set<NotificationChannel> previous, Set<NotificationChannel> current) {
        applyChange(userId, previous, current, ConsentSource.PROFILE_UPDATE);
    }

    private void applyChange(
            UUID userId, Set<NotificationChannel> previous, Set<NotificationChannel> current, ConsentSource source) {
        ConsentNoticeJpaEntity notice = noticeRepository.findCurrent(NOTIFICATION_CHANNELS_NOTICE).orElse(null);
        if (notice == null) {
            // Refusing to write a consent row that points at no notice: it would look like evidence
            // and prove nothing, which is worse than the gap it papers over. V34 seeds version 1, so
            // this means the migration did not run - loud, and not silently tolerated.
            throw new IllegalStateException(
                    "No consent notice found for code " + NOTIFICATION_CHANNELS_NOTICE
                            + " - consent cannot be recorded against a notice that does not exist (see V34)");
        }

        Instant now = Instant.now();
        Map<String, UserConsentJpaEntity> active = consentRepository.findByUserIdAndWithdrawnAtIsNull(userId).stream()
                .collect(Collectors.toMap(UserConsentJpaEntity::getItem, Function.identity(), (first, second) -> first));

        for (NotificationChannel channel : current) {
            // Already consenting to it: leave the existing row alone. Its granted_at is when the
            // person actually decided, and overwriting that would lose the only fact it carries.
            if (!active.containsKey(channel.name())) {
                consentRepository.save(new UserConsentJpaEntity(
                        IdGenerator.newUuid(), userId, notice.getId(), channel.name(), now, source.name()));
            }
        }

        List<UserConsentJpaEntity> withdrawn = active.values().stream()
                .filter(consent -> !current.contains(NotificationChannel.valueOf(consent.getItem())))
                .toList();
        for (UserConsentJpaEntity consent : withdrawn) {
            consent.withdraw(now);
            consentRepository.save(consent);
        }

        // The consent ledger is compliance evidence, so its writes are worth a line each. No PII in
        // it - a user id and a channel name, which is what the audit trail already carries.
        LOG.info("Consent updated for user {}: granted={} withdrawn={} notice={} v{} source={}",
                userId,
                current.stream().map(Enum::name).filter(item -> !active.containsKey(item)).toList(),
                withdrawn.stream().map(UserConsentJpaEntity::getItem).toList(),
                notice.getCode(), notice.getVersion(), source);
    }

    /** Where a decision was made. Distinguishes an initial choice from a later change. */
    public enum ConsentSource {
        REGISTRATION,
        PROFILE_UPDATE
    }
}
