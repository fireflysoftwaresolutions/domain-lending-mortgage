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
import io.fireflysoftware.core.lending.mortgages.sdk.model.MortgageAgreementDTO;
import io.fireflysoftware.domain.lending.mortgage.core.commands.GetMortgageAgreementQuery;
import lombok.extern.slf4j.Slf4j;
import org.fireflyframework.cqrs.annotations.QueryHandlerComponent;
import org.fireflyframework.cqrs.query.QueryHandler;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Loads one mortgage agreement from the core.
 */
@Slf4j
@QueryHandlerComponent
public class GetMortgageAgreementHandler extends QueryHandler<GetMortgageAgreementQuery, MortgageAgreementDTO> {

    private final MortgageAgreementApi api;

    public GetMortgageAgreementHandler(MortgageAgreementApi api) {
        this.api = api;
    }

    @Override
    protected Mono<MortgageAgreementDTO> doHandle(GetMortgageAgreementQuery query) {
        log.debug("Fetching mortgage agreement: mortgageAgreementId={}", query.mortgageAgreementId());
        return api.getById(query.mortgageAgreementId(), UUID.randomUUID().toString());
    }
}
