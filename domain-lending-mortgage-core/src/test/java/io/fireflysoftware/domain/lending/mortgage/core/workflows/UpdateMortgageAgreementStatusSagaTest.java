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
import io.fireflysoftware.domain.lending.mortgage.core.commands.UpdateMortgageAgreementStatusCommand;
import io.fireflysoftware.domain.lending.mortgage.core.constants.MortgageAgreementSagaConstants;

import org.fireflyframework.cqrs.command.CommandBus;
import org.fireflyframework.cqrs.query.QueryBus;
import org.fireflyframework.orchestration.core.context.ExecutionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies a status change is written through the core and can be restored.
 */
@ExtendWith(MockitoExtension.class)
class UpdateMortgageAgreementStatusSagaTest {

    @Mock
    private CommandBus commandBus;

    @Mock
    private QueryBus queryBus;

    private UpdateMortgageAgreementStatusSaga saga;

    @BeforeEach
    void setUp() {
        saga = new UpdateMortgageAgreementStatusSaga(commandBus, queryBus);
    }

    @Test
    void updateStatus_sendsTheRequestedStatusToTheCore() {
        UUID id = UUID.randomUUID();
        MortgageAgreementDTO current = new MortgageAgreementDTO(id, null, null, null, null);
        current.setAgreementStatus(MortgageAgreementDTO.AgreementStatusEnum.DRAFT);
        when(commandBus.send(any(ApplyMortgageAgreementStatusCommand.class))).thenReturn(Mono.just(current));

        ExecutionContext ctx = ExecutionContext.forSaga(MortgageAgreementSagaConstants.SAGA_UPDATE_STATUS, "test");
        ctx.putVariable(MortgageAgreementSagaConstants.CTX_RECORD, current);

        StepVerifier.create(saga.updateMortgageAgreementStatus(new UpdateMortgageAgreementStatusCommand(id, "ACTIVE", "Caller request"), ctx))
                .expectNext(current)
                .verifyComplete();

        ArgumentCaptor<ApplyMortgageAgreementStatusCommand> captor = ArgumentCaptor.forClass(ApplyMortgageAgreementStatusCommand.class);
        verify(commandBus).send(captor.capture());
        assertThat(captor.getValue().mortgageAgreementId()).isEqualTo(id);
        assertThat(captor.getValue().record().getAgreementStatus()).isEqualTo(MortgageAgreementDTO.AgreementStatusEnum.ACTIVE);
        assertThat(ctx.getVariableAs(MortgageAgreementSagaConstants.CTX_PREVIOUS_STATUS, String.class)).isEqualTo("DRAFT");
    }

    @Test
    void restore_writesThePreviousStatusBack() {
        UUID id = UUID.randomUUID();
        MortgageAgreementDTO updated = new MortgageAgreementDTO(id, null, null, null, null);
        updated.setAgreementStatus(MortgageAgreementDTO.AgreementStatusEnum.ACTIVE);
        when(commandBus.send(any(ApplyMortgageAgreementStatusCommand.class))).thenReturn(Mono.just(updated));

        ExecutionContext ctx = ExecutionContext.forSaga(MortgageAgreementSagaConstants.SAGA_UPDATE_STATUS, "test");
        ctx.putVariable(MortgageAgreementSagaConstants.CTX_PREVIOUS_STATUS, "DRAFT");

        StepVerifier.create(saga.restoreMortgageAgreementStatus(updated, ctx))
                .verifyComplete();

        ArgumentCaptor<ApplyMortgageAgreementStatusCommand> captor = ArgumentCaptor.forClass(ApplyMortgageAgreementStatusCommand.class);
        verify(commandBus).send(captor.capture());
        assertThat(captor.getValue().record().getAgreementStatus()).isEqualTo(MortgageAgreementDTO.AgreementStatusEnum.DRAFT);
    }

}
