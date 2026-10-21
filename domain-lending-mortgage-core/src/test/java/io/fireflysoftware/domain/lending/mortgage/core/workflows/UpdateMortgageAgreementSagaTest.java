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
import io.fireflysoftware.domain.lending.mortgage.core.commands.UpdateMortgageAgreementCommand;
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
 * Verifies a field update keeps the current status and can be restored.
 */
@ExtendWith(MockitoExtension.class)
class UpdateMortgageAgreementSagaTest {

    @Mock
    private CommandBus commandBus;

    @Mock
    private QueryBus queryBus;

    private UpdateMortgageAgreementSaga saga;

    @BeforeEach
    void setUp() {
        saga = new UpdateMortgageAgreementSaga(commandBus, queryBus);
    }

    @Test
    void update_keepsTheCurrentStatus() {
        UUID id = UUID.randomUUID();
        MortgageAgreementDTO current = new MortgageAgreementDTO(id, null, null, null, null);
        current.setAgreementStatus(MortgageAgreementDTO.AgreementStatusEnum.DRAFT);
        MortgageAgreementDTO incoming = new MortgageAgreementDTO();
        incoming.setAgreementStatus(MortgageAgreementDTO.AgreementStatusEnum.ACTIVE);
        when(commandBus.send(any(ApplyMortgageAgreementStatusCommand.class))).thenAnswer(invocation ->
                Mono.just(invocation.getArgument(0, ApplyMortgageAgreementStatusCommand.class).record()));

        ExecutionContext ctx = ExecutionContext.forSaga(MortgageAgreementSagaConstants.SAGA_UPDATE_RECORD, "test");
        ctx.putVariable(MortgageAgreementSagaConstants.CTX_RECORD, current);

        StepVerifier.create(saga.updateMortgageAgreement(new UpdateMortgageAgreementCommand(id, incoming), ctx))
                .assertNext(saved -> assertThat(saved.getAgreementStatus()).isEqualTo(MortgageAgreementDTO.AgreementStatusEnum.DRAFT))
                .verifyComplete();

        ArgumentCaptor<ApplyMortgageAgreementStatusCommand> captor = ArgumentCaptor.forClass(ApplyMortgageAgreementStatusCommand.class);
        verify(commandBus).send(captor.capture());
        assertThat(captor.getValue().mortgageAgreementId()).isEqualTo(id);
        assertThat(captor.getValue().record().getAgreementStatus()).isEqualTo(MortgageAgreementDTO.AgreementStatusEnum.DRAFT);
        assertThat(ctx.getVariableAs(MortgageAgreementSagaConstants.CTX_PREVIOUS_RECORD, MortgageAgreementDTO.class)).isSameAs(current);
    }

    @Test
    void restore_writesThePreviousRecordBack() {
        UUID id = UUID.randomUUID();
        MortgageAgreementDTO previous = new MortgageAgreementDTO(id, null, null, null, null);
        previous.setAgreementStatus(MortgageAgreementDTO.AgreementStatusEnum.DRAFT);
        MortgageAgreementDTO updated = new MortgageAgreementDTO(id, null, null, null, null);
        updated.setAgreementStatus(MortgageAgreementDTO.AgreementStatusEnum.ACTIVE);
        when(commandBus.send(any(ApplyMortgageAgreementStatusCommand.class))).thenReturn(Mono.just(previous));

        ExecutionContext ctx = ExecutionContext.forSaga(MortgageAgreementSagaConstants.SAGA_UPDATE_RECORD, "test");
        ctx.putVariable(MortgageAgreementSagaConstants.CTX_PREVIOUS_RECORD, previous);

        StepVerifier.create(saga.restoreMortgageAgreement(updated, ctx))
                .verifyComplete();

        ArgumentCaptor<ApplyMortgageAgreementStatusCommand> captor = ArgumentCaptor.forClass(ApplyMortgageAgreementStatusCommand.class);
        verify(commandBus).send(captor.capture());
        assertThat(captor.getValue().record()).isSameAs(previous);
    }
}
