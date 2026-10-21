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
import io.fireflysoftware.domain.lending.mortgage.core.commands.UpdateMortgageAgreementCommand;
import org.fireflyframework.cqrs.command.CommandBus;
import org.fireflyframework.cqrs.query.QueryBus;
import org.fireflyframework.orchestration.core.context.ExecutionContext;
import org.fireflyframework.orchestration.saga.annotation.Saga;
import org.fireflyframework.orchestration.saga.annotation.SagaStep;
import org.fireflyframework.orchestration.saga.annotation.StepEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static io.fireflysoftware.domain.lending.mortgage.core.constants.MortgageAgreementSagaConstants.COMPENSATE_RESTORE_RECORD;
import static io.fireflysoftware.domain.lending.mortgage.core.constants.MortgageAgreementSagaConstants.CTX_PREVIOUS_RECORD;
import static io.fireflysoftware.domain.lending.mortgage.core.constants.MortgageAgreementSagaConstants.CTX_RECORD;
import static io.fireflysoftware.domain.lending.mortgage.core.constants.MortgageAgreementSagaConstants.EVENT_RECORD_UPDATED;
import static io.fireflysoftware.domain.lending.mortgage.core.constants.MortgageAgreementSagaConstants.EVENT_RETRIEVED;
import static io.fireflysoftware.domain.lending.mortgage.core.constants.MortgageAgreementSagaConstants.SAGA_UPDATE_RECORD;
import static io.fireflysoftware.domain.lending.mortgage.core.constants.MortgageAgreementSagaConstants.STEP_RETRIEVE_FOR_UPDATE;
import static io.fireflysoftware.domain.lending.mortgage.core.constants.MortgageAgreementSagaConstants.STEP_UPDATE_RECORD;

/**
 * Updates a mortgage agreement on {@code core-lending-mortgage} and compensates by writing the previous record back.
 * Status stays on the status saga.
 */
@Saga(name = SAGA_UPDATE_RECORD)
@Service
public class UpdateMortgageAgreementSaga {

    private final CommandBus commandBus;
    private final QueryBus queryBus;

    @Autowired
    public UpdateMortgageAgreementSaga(CommandBus commandBus, QueryBus queryBus) {
        this.commandBus = commandBus;
        this.queryBus = queryBus;
    }

    @SagaStep(id = STEP_RETRIEVE_FOR_UPDATE)
    @StepEvent(type = EVENT_RETRIEVED)
    public Mono<MortgageAgreementDTO> retrieveMortgageAgreementForUpdate(GetMortgageAgreementQuery query, ExecutionContext ctx) {
        return queryBus.query(query)
                .doOnNext(record -> ctx.putVariable(CTX_RECORD, record));
    }

    @SagaStep(id = STEP_UPDATE_RECORD, dependsOn = STEP_RETRIEVE_FOR_UPDATE, compensate = COMPENSATE_RESTORE_RECORD)
    @StepEvent(type = EVENT_RECORD_UPDATED)
    public Mono<MortgageAgreementDTO> updateMortgageAgreement(UpdateMortgageAgreementCommand command, ExecutionContext ctx) {
        MortgageAgreementDTO current = ctx.getVariableAs(CTX_RECORD, MortgageAgreementDTO.class);
        if (current == null || current.getMortgageAgreementId() == null) {
            return Mono.error(new IllegalStateException("Mortgage agreement was not found"));
        }
        MortgageAgreementDTO incoming = command.record();
        if (incoming == null) {
            return Mono.error(new IllegalArgumentException("Mortgage agreement is required"));
        }
        if (current.getAgreementStatus() != null) {
            incoming.setAgreementStatus(current.getAgreementStatus());
        }
        ctx.putVariable(CTX_PREVIOUS_RECORD, current);
        return commandBus.send(new ApplyMortgageAgreementStatusCommand(current.getMortgageAgreementId(), incoming))
                .doOnNext(updated -> ctx.putVariable(CTX_RECORD, updated));
    }

    public Mono<Void> restoreMortgageAgreement(MortgageAgreementDTO updated, ExecutionContext ctx) {
        MortgageAgreementDTO previous = ctx.getVariableAs(CTX_PREVIOUS_RECORD, MortgageAgreementDTO.class);
        if (previous == null || previous.getMortgageAgreementId() == null) {
            return Mono.empty();
        }
        return commandBus.send(new ApplyMortgageAgreementStatusCommand(previous.getMortgageAgreementId(), previous)).then();
    }
}
