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
import io.fireflysoftware.domain.lending.mortgage.core.commands.UpdateMortgageAgreementCommand;
import io.fireflysoftware.domain.lending.mortgage.core.commands.UpdateMortgageAgreementStatusCommand;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Application service for mortgage agreement orchestration.
 */
public interface MortgageAgreementsService {

    /**
     * Creates a mortgage agreement through the register saga.
     *
     * @param command the create command
     * @return the record returned by the core
     */
    Mono<MortgageAgreementDTO> create(CreateMortgageAgreementCommand command);

    /**
     * Moves a mortgage agreement to a new status through the core.
     *
     * @param command the status change
     * @return the updated record
     */
    Mono<MortgageAgreementDTO> updateStatus(UpdateMortgageAgreementStatusCommand command);

    /**
     * Reads one mortgage agreement.
     *
     * @param mortgageAgreementId the core identifier
     * @return the record, or empty when the core has none
     */
    Mono<MortgageAgreementDTO> get(UUID mortgageAgreementId);

    /**
     * Updates fields on a mortgage agreement and leaves its status unchanged.
     *
     * @param command the update
     * @return the record returned by the core
     */
    Mono<MortgageAgreementDTO> update(UpdateMortgageAgreementCommand command);

    /**
     * Close mortgage agreement when the core allows CLOSED.
     *
     * @param mortgageAgreementId the core identifier
     * @param statusReason optional reason stored by the status saga when the core keeps one
     * @return the updated record
     */
    Mono<MortgageAgreementDTO> close(UUID mortgageAgreementId, String statusReason);

    /**
     * Cancel mortgage agreement when the core allows CANCELLED.
     *
     * @param mortgageAgreementId the core identifier
     * @param statusReason optional reason stored by the status saga when the core keeps one
     * @return the updated record
     */
    Mono<MortgageAgreementDTO> cancel(UUID mortgageAgreementId, String statusReason);

    /**
     * Deletes a mortgage agreement when the core lifecycle allows removal.
     *
     * @param mortgageAgreementId the core identifier
     * @return completion when the core has deleted the record
     */
    Mono<Void> remove(UUID mortgageAgreementId);
}
