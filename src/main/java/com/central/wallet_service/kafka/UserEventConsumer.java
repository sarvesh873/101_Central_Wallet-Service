package com.central.wallet_service.kafka;

import com.central.authentication.UserEvent;
import com.central.wallet_service.constants.WalletConstants;
import com.central.wallet_service.exception.WalletNotFoundException;
import com.central.wallet_service.model.WalletUserSnapshot;
import com.central.wallet_service.repository.WalletUserSnapshotRepository;
import com.google.protobuf.InvalidProtocolBufferException;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UserEventConsumer {


    private static final String USER_EVENT_TOPIC = "${kafka.topics.user-events}";

    private final KafkaTemplate<String, byte[]> kafkaTemplate;

    private final WalletUserSnapshotRepository userSnapshotRepository;

    @Autowired
    public UserEventConsumer(KafkaTemplate<String, byte[]> kafkaTemplate, WalletUserSnapshotRepository userSnapshotRepository) {
        this.kafkaTemplate = kafkaTemplate;
        this.userSnapshotRepository = userSnapshotRepository;
    }

    @KafkaListener(topics = USER_EVENT_TOPIC, groupId = "wallet-service")
    @Transactional
    public void handleUserUpdateEvent(byte[] event) {
        long startTime = System.currentTimeMillis();
        String userCode = "";

        try {
            log.info("Received event from Kafka topic: {}", USER_EVENT_TOPIC);
            UserEvent userEvent = UserEvent.parseFrom(event);
            userCode = userEvent.getUserCode();

            WalletUserSnapshot userSnapshot = userSnapshotRepository.findByUserCode(userCode)
                    .orElseThrow(() -> new WalletNotFoundException(
                            String.format(WalletConstants.WALLET_NOT_FOUND, userEvent.getUserCode())));

            userSnapshot.setUsername(userEvent.getUsername());
            userSnapshot.setEmail(userEvent.getEmail());
            userSnapshot.setPhoneNumber(userEvent.getPhoneNumber());

            userSnapshotRepository.save(userSnapshot);

        } catch (InvalidProtocolBufferException e) {
            log.error("Failed to parse transaction event after {} ms. Error: {}",
                    (System.currentTimeMillis() - startTime), e.getMessage(), e);
            throw new RuntimeException("Failed to process transaction event", e);
        } finally {
            log.info("Completed processing for transaction: {} - Total time taken: {} ms",
                    userCode, (System.currentTimeMillis() - startTime));
        }

    }


}
