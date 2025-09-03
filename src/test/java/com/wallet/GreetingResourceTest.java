package com.wallet;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class GreetingResourceTest {

    @InjectMocks
    GreetingResource greetingResource;

    @Test
    void testHelloEndpoint() {
        // When
        String result = greetingResource.hello();
        
        // Then
        assertEquals("Hello from Quarkus REST", result);
    }
}