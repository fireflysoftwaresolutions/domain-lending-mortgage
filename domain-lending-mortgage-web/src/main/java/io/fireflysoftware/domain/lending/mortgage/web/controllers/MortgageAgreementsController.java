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

package io.fireflysoftware.domain.lending.mortgage.web.controllers;

import io.fireflysoftware.core.lending.mortgages.sdk.model.MortgageAgreementDTO;
import io.fireflysoftware.domain.lending.mortgage.core.commands.CreateMortgageAgreementCommand;
import io.fireflysoftware.domain.lending.mortgage.core.commands.UpdateMortgageAgreementCommand;
import io.fireflysoftware.domain.lending.mortgage.core.commands.UpdateMortgageAgreementStatusCommand;
import io.fireflysoftware.domain.lending.mortgage.core.services.MortgageAgreementsService;
import io.fireflysoftware.domain.lending.mortgage.interfaces.dtos.ReasonRequest;
import io.fireflysoftware.domain.lending.mortgage.interfaces.dtos.StatusChangeRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Domain API for mortgage agreements.
 */
@RestController
@RequestMapping("/api/v1/mortgage-agreements")
@Tag(name = "MortgageAgreements", description = "Domain orchestration for mortgage agreements")
public class MortgageAgreementsController {

    private final MortgageAgreementsService service;

    public MortgageAgreementsController(MortgageAgreementsService service) {
        this.service = service;
    }

    @Operation(summary = "Create mortgage agreement", description = "Runs the register saga against core-lending-mortgage.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MortgageAgreementDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content)
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<MortgageAgreementDTO>> create(@RequestBody MortgageAgreementDTO body) {
        return service.create(new CreateMortgageAgreementCommand(body))
                .map(created -> ResponseEntity.status(HttpStatus.CREATED).body(created));
    }

    @Operation(summary = "Get mortgage agreement", description = "Reads one record from core-lending-mortgage.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MortgageAgreementDTO.class))),
            @ApiResponse(responseCode = "404", description = "Not found", content = @Content)
    })
    @GetMapping(value = "/{mortgageAgreementId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<MortgageAgreementDTO>> get(
            @Parameter(description = "Core identifier", required = true) @PathVariable UUID mortgageAgreementId) {
        return service.get(mortgageAgreementId)
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @Operation(summary = "Update status", description = "Runs the status saga against core-lending-mortgage.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MortgageAgreementDTO.class))),
            @ApiResponse(responseCode = "404", description = "Not found", content = @Content)
    })
    @PutMapping(value = "/{mortgageAgreementId}/status", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<MortgageAgreementDTO>> updateStatus(
            @Parameter(description = "Core identifier", required = true) @PathVariable UUID mortgageAgreementId,
            @RequestBody StatusChangeRequest request) {
        return service.updateStatus(new UpdateMortgageAgreementStatusCommand(mortgageAgreementId, request.getStatus(), request.getStatusReason()))
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @Operation(summary = "Update mortgage agreement", description = "Runs the update saga against core-lending-mortgage. Status is left unchanged.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MortgageAgreementDTO.class))),
            @ApiResponse(responseCode = "404", description = "Not found", content = @Content)
    })
    @PutMapping(value = "/{mortgageAgreementId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<MortgageAgreementDTO>> update(
            @Parameter(description = "Core identifier", required = true) @PathVariable UUID mortgageAgreementId,
            @RequestBody MortgageAgreementDTO body) {
        return service.update(new UpdateMortgageAgreementCommand(mortgageAgreementId, body))
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @Operation(summary = "Close mortgage agreement", description = "Closes a mortgage agreement when the mortgage core allows CLOSED.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MortgageAgreementDTO.class))),
            @ApiResponse(responseCode = "404", description = "Not found", content = @Content)
    })
    @PostMapping(value = "/{mortgageAgreementId}/close", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<MortgageAgreementDTO>> close(
            @Parameter(description = "Core identifier", required = true) @PathVariable UUID mortgageAgreementId,
            @RequestBody ReasonRequest request) {
        return service.close(mortgageAgreementId, request.getStatusReason())
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @Operation(summary = "Cancel mortgage agreement", description = "Cancels a mortgage agreement when the mortgage core allows CANCELLED.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MortgageAgreementDTO.class))),
            @ApiResponse(responseCode = "404", description = "Not found", content = @Content)
    })
    @PostMapping(value = "/{mortgageAgreementId}/cancel", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<MortgageAgreementDTO>> cancel(
            @Parameter(description = "Core identifier", required = true) @PathVariable UUID mortgageAgreementId,
            @RequestBody ReasonRequest request) {
        return service.cancel(mortgageAgreementId, request.getStatusReason())
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @Operation(summary = "Remove mortgage agreement", description = "Deletes the record through core-lending-mortgage when that core allows removal.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Removed"),
            @ApiResponse(responseCode = "404", description = "Not found", content = @Content)
    })
    @DeleteMapping("/{mortgageAgreementId}")
    public Mono<ResponseEntity<Void>> remove(
            @Parameter(description = "Core identifier", required = true) @PathVariable UUID mortgageAgreementId) {
        return service.remove(mortgageAgreementId)
                .thenReturn(ResponseEntity.noContent().build());
    }
}
