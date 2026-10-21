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
import io.fireflysoftware.domain.lending.mortgage.core.commands.CreateMortgageAgreementCommand;
import io.fireflysoftware.domain.lending.mortgage.core.commands.RemoveMortgageAgreementCommand;
import io.fireflysoftware.domain.lending.mortgage.core.constants.MortgageAgreementSagaConstants;

import org.fireflyframework.cqrs.command.CommandBus;
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
 * Verifies the register saga stores the core result and can compensate by deleting it.
 */
@ExtendWith(MockitoExtension.class)
class RegisterMortgageAgreementSagaTest {

    @Mock
    private CommandBus commandBus;

    private RegisterMortgageAgreementSaga saga;

    @BeforeEach
    void setUp() {
        saga = new RegisterMortgageAgreementSaga(commandBus);
    }

    @Test
    void register_storesTheRecordReturnedByTheCore() {
        UUID id = UUID.randomUUID();
        MortgageAgreementDTO created = new MortgageAgreementDTO(id, null, null, null, null);
        created.setAgreementStatus(MortgageAgreementDTO.AgreementStatusEnum.DRAFT);
        when(commandBus.send(any(CreateMortgageAgreementCommand.class))).thenReturn(Mono.just(created));

        ExecutionContext ctx = ExecutionContext.forSaga(MortgageAgreementSagaConstants.SAGA_REGISTER, "test");
        MortgageAgreementDTO request = new MortgageAgreementDTO();
        request.setAgreementStatus(MortgageAgreementDTO.AgreementStatusEnum.DRAFT);

        StepVerifier.create(saga.registerMortgageAgreement(new CreateMortgageAgreementCommand(request), ctx))
                .expectNext(created)
                .verifyComplete();

        assertThat(ctx.getVariableAs(MortgageAgreementSagaConstants.CTX_RECORD, MortgageAgreementDTO.class).getMortgageAgreementId()).isEqualTo(id);
    }

    @Test
    void remove_sendsDeleteForTheCreatedRecord() {
        UUID id = UUID.randomUUID();
        when(commandBus.send(any(RemoveMortgageAgreementCommand.class))).thenReturn(Mono.empty());

        StepVerifier.create(saga.removeMortgageAgreement(new MortgageAgreementDTO(id, null, null, null, null)))
                .verifyComplete();

        ArgumentCaptor<RemoveMortgageAgreementCommand> captor = ArgumentCaptor.forClass(RemoveMortgageAgreementCommand.class);
        verify(commandBus).send(captor.capture());
        assertThat(captor.getValue().mortgageAgreementId()).isEqualTo(id);
    }

}
