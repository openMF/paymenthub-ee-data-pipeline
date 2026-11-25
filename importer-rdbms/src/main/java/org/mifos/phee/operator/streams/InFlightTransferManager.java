package org.mifos.phee.operator.streams;

import com.jayway.jsonpath.DocumentContext;
import org.mifos.phee.operator.config.TransferTransformerConfig;
import org.mifos.phee.operator.entity.transfer.Transfer;
import org.mifos.phee.operator.entity.transfer.TransferRepository;
import org.mifos.phee.operator.entity.transfer.TransferStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class InFlightTransferManager {

    @Autowired
    TransferTransformerConfig transferTransformerConfig;

    @Autowired
    TransferRepository transferRepository;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public Transfer retrieveOrCreateTransfer(String bpmn, DocumentContext record, String processType) {
        Long processInstanceKey = record.read("$.value.processInstanceKey", Long.class);
        Optional<TransferTransformerConfig.Flow> config = transferTransformerConfig.findFlow(bpmn);

        Optional<Transfer> optionalTransfer = transferRepository.findByWorkflowInstanceKey(processInstanceKey);

        Transfer transfer = optionalTransfer.orElseGet(() -> {
            if (!processType.equals("INCIDENT")) {
                logger.debug("Creating new Transfer for processInstanceKey: {}", processInstanceKey);
                Transfer newTransfer = new Transfer(processInstanceKey);
                newTransfer.setStatus(TransferStatus.IN_PROGRESS);

                config.ifPresent(c -> newTransfer.setDirection(c.getDirection()));

                if (config.isEmpty()) {
                    logger.error("No config found for bpmn: {}", bpmn);
                }

                transferRepository.save(newTransfer);
                return newTransfer;
            } else {
                //Since no transaction found returning dummy transaction
                Transfer dummy = new Transfer(0L);
                dummy.setErrorInformation("404");
                return dummy;
            }
        });

        optionalTransfer.ifPresent(t -> logger.debug("Found existing Transfer for processInstanceKey: {}", processInstanceKey));

        return transfer;
    }
}
