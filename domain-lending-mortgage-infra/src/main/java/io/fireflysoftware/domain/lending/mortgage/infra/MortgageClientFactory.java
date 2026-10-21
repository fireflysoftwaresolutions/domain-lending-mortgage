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

package io.fireflysoftware.domain.lending.mortgage.infra;

import io.fireflysoftware.core.lending.mortgages.sdk.api.MortgageAgreementApi;
import io.fireflysoftware.core.lending.mortgages.sdk.invoker.ApiClient;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * Exposes the {@code core-lending-mortgage} SDK clients used by the saga handlers.
 */
@Component
public class MortgageClientFactory {

    private final ApiClient apiClient;

    public MortgageClientFactory(MortgageCoreProperties properties) {
        this.apiClient = new ApiClient();
        this.apiClient.setBasePath(properties.getBasePath());
    }

    /**
     * Creates the primary core client.
     *
     * @return the API client
     */
    @Bean
    public MortgageAgreementApi mortgageAgreementApi() {
        return new MortgageAgreementApi(apiClient);
    }

}
