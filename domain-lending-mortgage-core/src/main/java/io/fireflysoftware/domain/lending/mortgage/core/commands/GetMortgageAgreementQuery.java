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

package io.fireflysoftware.domain.lending.mortgage.core.commands;

import io.fireflysoftware.core.lending.mortgages.sdk.model.MortgageAgreementDTO;
import org.fireflyframework.cqrs.query.Query;

import java.util.UUID;

/**
 * Reads one mortgage agreement from the core.
 */
public record GetMortgageAgreementQuery(UUID mortgageAgreementId) implements Query<MortgageAgreementDTO> {
}
