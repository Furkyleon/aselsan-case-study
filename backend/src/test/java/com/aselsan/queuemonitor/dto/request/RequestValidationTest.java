package com.aselsan.queuemonitor.dto.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.aselsan.queuemonitor.domain.StopScope;
import com.aselsan.queuemonitor.domain.WorkerType;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class RequestValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    @Test
    void shouldValidateStartSimulationRequest() {
        StartSimulationRequest invalidRequest = new StartSimulationRequest(-1, 1, 0);
        StartSimulationRequest validRequest = new StartSimulationRequest(1, 0, 10);

        assertEquals(2, validator.validate(invalidRequest).size());
        assertTrue(validator.validate(validRequest).isEmpty());
    }

    @Test
    void shouldValidateAddWorkersRequest() {
        AddWorkersRequest invalidRequest = new AddWorkersRequest(null, 0);
        AddWorkersRequest validRequest = new AddWorkersRequest(WorkerType.SENDER, 2);

        assertEquals(2, validator.validate(invalidRequest).size());
        assertTrue(validator.validate(validRequest).isEmpty());
    }

    @Test
    void shouldValidateStopSimulationRequest() {
        StopSimulationRequest invalidRequest = new StopSimulationRequest(null);
        StopSimulationRequest validRequest = new StopSimulationRequest(StopScope.ALL);

        assertEquals(1, validator.validate(invalidRequest).size());
        assertTrue(validator.validate(validRequest).isEmpty());
    }

    @Test
    void shouldValidateWorkerPriorityRequest() {
        UpdateWorkerPriorityRequest tooLow =
                new UpdateWorkerPriorityRequest(WorkerType.SENDER, 0);
        UpdateWorkerPriorityRequest tooHigh =
                new UpdateWorkerPriorityRequest(WorkerType.RECEIVER, 11);
        UpdateWorkerPriorityRequest validRequest =
                new UpdateWorkerPriorityRequest(WorkerType.SENDER, 7);

        assertEquals(1, validator.validate(tooLow).size());
        assertEquals(1, validator.validate(tooHigh).size());
        assertTrue(validator.validate(validRequest).isEmpty());
    }
}
