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

package io.fireflysoftware.domain.lending.mortgage.core.handlers;

import io.fireflysoftware.core.lending.mortgages.sdk.api.MortgageAgreementApi;
import io.fireflysoftware.domain.lending.mortgage.core.commands.RemoveMortgageAgreementCommand;
import lombok.extern.slf4j.Slf4j;
import org.fireflyframework.cqrs.annotations.CommandHandlerComponent;
import org.fireflyframework.cqrs.command.CommandHandler;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Deletes a mortgage agreement through the core during saga compensation.
 */
@Slf4j
@CommandHandlerComponent
public class RemoveMortgageAgreementHandler extends CommandHandler<RemoveMortgageAgreementCommand, Void> {

    private final MortgageAgreementApi api;

    public RemoveMortgageAgreementHandler(MortgageAgreementApi api) {
        this.api = api;
    }

    @Override
    protected Mono<Void> doHandle(RemoveMortgageAgreementCommand command) {
        log.debug("Removing mortgage agreement: mortgageAgreementId={}", command.mortgageAgreementId());
        return api.delete(command.mortgageAgreementId(), UUID.randomUUID().toString()).then();
    }
}
