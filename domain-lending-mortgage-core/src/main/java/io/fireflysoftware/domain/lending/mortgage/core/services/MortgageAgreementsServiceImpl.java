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

package io.fireflysoftware.domain.lending.mortgage.core.services;

import io.fireflysoftware.core.lending.mortgages.sdk.model.MortgageAgreementDTO;
import io.fireflysoftware.domain.lending.mortgage.core.commands.CreateMortgageAgreementCommand;
import io.fireflysoftware.domain.lending.mortgage.core.commands.GetMortgageAgreementQuery;
import io.fireflysoftware.domain.lending.mortgage.core.commands.RemoveMortgageAgreementCommand;
import io.fireflysoftware.domain.lending.mortgage.core.commands.UpdateMortgageAgreementCommand;
import io.fireflysoftware.domain.lending.mortgage.core.commands.UpdateMortgageAgreementStatusCommand;
import io.fireflysoftware.domain.lending.mortgage.core.lifecycle.MortgageAgreementLifecycle;
import io.fireflysoftware.domain.lending.mortgage.core.constants.MortgageAgreementSagaConstants;
import org.fireflyframework.cqrs.command.CommandBus;
import org.fireflyframework.cqrs.query.QueryBus;
import org.fireflyframework.orchestration.saga.engine.SagaEngine;
import org.fireflyframework.orchestration.saga.engine.SagaResult;
import org.fireflyframework.orchestration.saga.engine.StepInputs;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Runs the mortgage agreement sagas and reads through the query bus.
 */
@Service
public class MortgageAgreementsServiceImpl implements MortgageAgreementsService {

    private final SagaEngine sagaEngine;
    private final QueryBus queryBus;
    private final CommandBus commandBus;

    public MortgageAgreementsServiceImpl(SagaEngine sagaEngine, QueryBus queryBus, CommandBus commandBus) {
        this.sagaEngine = sagaEngine;
        this.queryBus = queryBus;
        this.commandBus = commandBus;
    }

    @Override
    public Mono<MortgageAgreementDTO> create(CreateMortgageAgreementCommand command) {
        StepInputs inputs = StepInputs.builder()
                .forStepId(io.fireflysoftware.domain.lending.mortgage.core.constants.MortgageAgreementSagaConstants.STEP_REGISTER, command)
                .build();
        return sagaEngine.execute(io.fireflysoftware.domain.lending.mortgage.core.constants.MortgageAgreementSagaConstants.SAGA_REGISTER, inputs)
                .flatMap(result -> outcome(result, io.fireflysoftware.domain.lending.mortgage.core.constants.MortgageAgreementSagaConstants.STEP_REGISTER, MortgageAgreementDTO.class));
    }

    @Override
    public Mono<MortgageAgreementDTO> updateStatus(UpdateMortgageAgreementStatusCommand command) {
        StepInputs inputs = StepInputs.builder()
                .forStepId(io.fireflysoftware.domain.lending.mortgage.core.constants.MortgageAgreementSagaConstants.STEP_RETRIEVE, new GetMortgageAgreementQuery(command.mortgageAgreementId()))
                .forStepId(io.fireflysoftware.domain.lending.mortgage.core.constants.MortgageAgreementSagaConstants.STEP_UPDATE_STATUS, command)
                .build();
        return sagaEngine.execute(io.fireflysoftware.domain.lending.mortgage.core.constants.MortgageAgreementSagaConstants.SAGA_UPDATE_STATUS, inputs)
                .flatMap(result -> outcome(result, io.fireflysoftware.domain.lending.mortgage.core.constants.MortgageAgreementSagaConstants.STEP_UPDATE_STATUS, MortgageAgreementDTO.class));
    }

    @Override
    public Mono<MortgageAgreementDTO> get(UUID mortgageAgreementId) {
        return queryBus.query(new GetMortgageAgreementQuery(mortgageAgreementId));
    }

    @Override
    public Mono<MortgageAgreementDTO> update(UpdateMortgageAgreementCommand command) {
        StepInputs inputs = StepInputs.builder()
                .forStepId(io.fireflysoftware.domain.lending.mortgage.core.constants.MortgageAgreementSagaConstants.STEP_RETRIEVE_FOR_UPDATE, new GetMortgageAgreementQuery(command.mortgageAgreementId()))
                .forStepId(io.fireflysoftware.domain.lending.mortgage.core.constants.MortgageAgreementSagaConstants.STEP_UPDATE_RECORD, command)
                .build();
        return sagaEngine.execute(io.fireflysoftware.domain.lending.mortgage.core.constants.MortgageAgreementSagaConstants.SAGA_UPDATE_RECORD, inputs)
                .flatMap(result -> outcome(result, io.fireflysoftware.domain.lending.mortgage.core.constants.MortgageAgreementSagaConstants.STEP_UPDATE_RECORD, MortgageAgreementDTO.class));
    }

    @Override
    public Mono<MortgageAgreementDTO> close(UUID mortgageAgreementId, String statusReason) {
        return transition(mortgageAgreementId, MortgageAgreementDTO.AgreementStatusEnum.CLOSED, statusReason);
    }

    @Override
    public Mono<MortgageAgreementDTO> cancel(UUID mortgageAgreementId, String statusReason) {
        return transition(mortgageAgreementId, MortgageAgreementDTO.AgreementStatusEnum.CANCELLED, statusReason);
    }

    private Mono<MortgageAgreementDTO> transition(UUID mortgageAgreementId, MortgageAgreementDTO.AgreementStatusEnum target, String statusReason) {
        return get(mortgageAgreementId)
                .switchIfEmpty(Mono.error(new IllegalStateException("Mortgage agreement was not found")))
                .flatMap(current -> {
                    if (!MortgageAgreementLifecycle.canTransition(current.getAgreementStatus(), target)) {
                        String from = current.getAgreementStatus() == null
                                ? "unset"
                                : current.getAgreementStatus().getValue();
                        return Mono.<MortgageAgreementDTO>error(new IllegalStateException(
                                "Mortgage agreement " + mortgageAgreementId + " cannot move from " + from + " to " + target.getValue()));
                    }
                    return updateStatus(new UpdateMortgageAgreementStatusCommand(mortgageAgreementId, target.getValue(), statusReason));
                });
    }

    @Override
    public Mono<Void> remove(UUID mortgageAgreementId) {
        return get(mortgageAgreementId)
                .switchIfEmpty(Mono.error(new IllegalStateException("Mortgage agreement was not found")))
                .flatMap(current -> {
                    if (!MortgageAgreementLifecycle.canRemove(current.getAgreementStatus())) {
                        String statusValue = current.getAgreementStatus() == null
                                ? "unset"
                                : current.getAgreementStatus().getValue();
                        return Mono.error(new IllegalStateException(
                                "Mortgage agreement " + mortgageAgreementId + " is " + statusValue
                                        + " and cannot be deleted; close or cancel it so the agreement is kept"));
                    }
                    return commandBus.send(new RemoveMortgageAgreementCommand(mortgageAgreementId));
                });
    }

    private static <T> Mono<T> outcome(SagaResult result, String stepId, Class<T> type) {
        if (result.isFailed()) {
            Throwable error = result.error().orElseGet(() ->
                    new IllegalStateException("Saga " + result.sagaName() + " failed"));
            return Mono.error(error);
        }
        return Mono.justOrEmpty(result.resultOf(stepId, type))
                .switchIfEmpty(Mono.error(new IllegalStateException("Saga step " + stepId + " produced no result")));
    }
}
