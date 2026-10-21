/*
 * Copyright 2026 Firefly Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.fireflysoftware.domain.lending.mortgage.core.workflows;

import io.fireflysoftware.core.lending.mortgages.sdk.model.MortgageAgreementDTO;
import io.fireflysoftware.domain.lending.mortgage.core.commands.ApplyMortgageAgreementStatusCommand;
import io.fireflysoftware.domain.lending.mortgage.core.commands.GetMortgageAgreementQuery;
import io.fireflysoftware.domain.lending.mortgage.core.commands.UpdateMortgageAgreementStatusCommand;

import org.fireflyframework.cqrs.command.CommandBus;
import org.fireflyframework.cqrs.query.QueryBus;
import org.fireflyframework.orchestration.core.context.ExecutionContext;
import org.fireflyframework.orchestration.saga.annotation.Saga;
import org.fireflyframework.orchestration.saga.annotation.SagaStep;
import org.fireflyframework.orchestration.saga.annotation.StepEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static io.fireflysoftware.domain.lending.mortgage.core.constants.MortgageAgreementSagaConstants.COMPENSATE_RESTORE;
import static io.fireflysoftware.domain.lending.mortgage.core.constants.MortgageAgreementSagaConstants.CTX_PREVIOUS_STATUS;
import static io.fireflysoftware.domain.lending.mortgage.core.constants.MortgageAgreementSagaConstants.CTX_RECORD;
import static io.fireflysoftware.domain.lending.mortgage.core.constants.MortgageAgreementSagaConstants.EVENT_RETRIEVED;
import static io.fireflysoftware.domain.lending.mortgage.core.constants.MortgageAgreementSagaConstants.EVENT_STATUS_UPDATED;
import static io.fireflysoftware.domain.lending.mortgage.core.constants.MortgageAgreementSagaConstants.SAGA_UPDATE_STATUS;
import static io.fireflysoftware.domain.lending.mortgage.core.constants.MortgageAgreementSagaConstants.STEP_RETRIEVE;
import static io.fireflysoftware.domain.lending.mortgage.core.constants.MortgageAgreementSagaConstants.STEP_UPDATE_STATUS;




/**
 * Changes a mortgage agreement status by reading it and writing the new status through the core.
 */
@Saga(name = SAGA_UPDATE_STATUS)
@Service
public class UpdateMortgageAgreementStatusSaga {

    private final CommandBus commandBus;
    private final QueryBus queryBus;

    @Autowired
    public UpdateMortgageAgreementStatusSaga(CommandBus commandBus, QueryBus queryBus) {
        this.commandBus = commandBus;
        this.queryBus = queryBus;
    }

    @SagaStep(id = STEP_RETRIEVE)
    @StepEvent(type = EVENT_RETRIEVED)
    public Mono<MortgageAgreementDTO> retrieveMortgageAgreement(GetMortgageAgreementQuery query, ExecutionContext ctx) {
        return queryBus.query(query)
                .doOnNext(record -> ctx.putVariable(CTX_RECORD, record));
    }

    @SagaStep(id = STEP_UPDATE_STATUS, dependsOn = STEP_RETRIEVE, compensate = COMPENSATE_RESTORE)
    @StepEvent(type = EVENT_STATUS_UPDATED)
    public Mono<MortgageAgreementDTO> updateMortgageAgreementStatus(UpdateMortgageAgreementStatusCommand command, ExecutionContext ctx) {
        MortgageAgreementDTO current = ctx.getVariableAs(CTX_RECORD, MortgageAgreementDTO.class);
        if (current == null || current.getMortgageAgreementId() == null) {
            return Mono.error(new IllegalStateException("Mortgage agreement was not found"));
        }
        if (current.getAgreementStatus() == null) {
            return Mono.error(new IllegalStateException("Mortgage agreement has no current status"));
        }
        if (command.status() == null || command.status().isBlank()) {
            return Mono.error(new IllegalArgumentException("Status is required"));
        }
        ctx.putVariable(CTX_PREVIOUS_STATUS, current.getAgreementStatus().getValue());
        current.setAgreementStatus(MortgageAgreementDTO.AgreementStatusEnum.fromValue(command.status()));
        return commandBus.send(new ApplyMortgageAgreementStatusCommand(current.getMortgageAgreementId(), current))
                .doOnNext(updated -> ctx.putVariable(CTX_RECORD, updated));
    }

    public Mono<Void> restoreMortgageAgreementStatus(MortgageAgreementDTO updated, ExecutionContext ctx) {
        if (updated == null || updated.getMortgageAgreementId() == null) {
            return Mono.empty();
        }
        String previous = ctx.getVariableAs(CTX_PREVIOUS_STATUS, String.class);
        if (previous == null || previous.isBlank()) {
            return Mono.empty();
        }
        updated.setAgreementStatus(MortgageAgreementDTO.AgreementStatusEnum.fromValue(previous));
        return commandBus.send(new ApplyMortgageAgreementStatusCommand(updated.getMortgageAgreementId(), updated)).then();
    }

}
