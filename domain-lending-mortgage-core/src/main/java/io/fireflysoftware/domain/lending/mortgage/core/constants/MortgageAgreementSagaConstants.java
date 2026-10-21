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

package io.fireflysoftware.domain.lending.mortgage.core.constants;

/**
 * Names shared by the mortgage agreement sagas, their steps, and the saga engine.
 */
public final class MortgageAgreementSagaConstants {

    public static final String SAGA_REGISTER = "RegisterMortgageAgreementSaga";
    public static final String SAGA_UPDATE_STATUS = "UpdateMortgageAgreementStatusSaga";
    public static final String SAGA_UPDATE_RECORD = "UpdateMortgageAgreementSaga";

    public static final String STEP_REGISTER = "registerMortgageAgreement";
    public static final String STEP_RETRIEVE = "retrieveMortgageAgreement";
    public static final String STEP_UPDATE_STATUS = "updateMortgageAgreementStatus";
    public static final String STEP_RETRIEVE_FOR_UPDATE = "retrieveMortgageAgreementForUpdate";
    public static final String STEP_UPDATE_RECORD = "updateMortgageAgreement";

    public static final String COMPENSATE_REMOVE = "removeMortgageAgreement";
    public static final String COMPENSATE_RESTORE = "restoreMortgageAgreementStatus";
    public static final String COMPENSATE_RESTORE_RECORD = "restoreMortgageAgreement";

    public static final String EVENT_REGISTERED = "mortgage-agreements.registered";
    public static final String EVENT_RETRIEVED = "mortgage-agreements.retrieved";
    public static final String EVENT_STATUS_UPDATED = "mortgage-agreements.status.updated";
    public static final String EVENT_RECORD_UPDATED = "mortgage-agreements.updated";

    public static final String CTX_RECORD = "mortgage-agreementsRecord";
    public static final String CTX_PREVIOUS_STATUS = "previousMortgageAgreementStatus";
    public static final String CTX_PREVIOUS_RECORD = "previousMortgageAgreementRecord";

    private MortgageAgreementSagaConstants() {
    }
}
