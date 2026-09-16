package com.dypiu.nba.emmu;

import com.dypiu.nba.emmu.dto.EmmuIntent;
import com.dypiu.nba.emmu.router.EmmuIntentRouter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmmuIntentRouterTest {

    private EmmuIntentRouter router;

    @BeforeEach
    void setUp() {
        router = new EmmuIntentRouter();
    }

    @Test
    @DisplayName("Should detect mutation attempts and route to MUTATION_ATTEMPT")
    void testMutationDetection() {
        EmmuIntentRouter.RoutedIntent r1 = router.route("Please approve this ATR report right now");
        assertThat(r1.getPrimaryIntent()).isEqualTo(EmmuIntent.MUTATION_ATTEMPT);

        EmmuIntentRouter.RoutedIntent r2 = router.route("Delete course CS101 from the curriculum");
        assertThat(r2.getPrimaryIntent()).isEqualTo(EmmuIntent.MUTATION_ATTEMPT);

        EmmuIntentRouter.RoutedIntent r3 = router.route("Submit the batch attainment for approval");
        assertThat(r3.getPrimaryIntent()).isEqualTo(EmmuIntent.MUTATION_ATTEMPT);

        EmmuIntentRouter.RoutedIntent r4 = router.route("Update the target threshold for PO3 to 2.5");
        assertThat(r4.getPrimaryIntent()).isEqualTo(EmmuIntent.MUTATION_ATTEMPT);
    }

    @Test
    @DisplayName("Should route greetings and general chat to GENERIC_CHAT")
    void testGenericChatRouting() {
        EmmuIntentRouter.RoutedIntent r1 = router.route("Hello! Who are you?");
        assertThat(r1.getPrimaryIntent()).isEqualTo(EmmuIntent.GENERIC_CHAT);

        EmmuIntentRouter.RoutedIntent r2 = router.route("Good morning, what can you do?");
        assertThat(r2.getPrimaryIntent()).isEqualTo(EmmuIntent.GENERIC_CHAT);

        EmmuIntentRouter.RoutedIntent r3 = router.route("Hi Emmu");
        assertThat(r3.getPrimaryIntent()).isEqualTo(EmmuIntent.GENERIC_CHAT);
    }

    @Test
    @DisplayName("Should route outcome attainment queries and extract outcome codes")
    void testOutcomeAttainmentRouting() {
        EmmuIntentRouter.RoutedIntent r1 = router.route("What is the attainment level of PO3 in the 2020 batch?");
        assertThat(r1.getPrimaryIntent()).isEqualTo(EmmuIntent.OUTCOME_ATTAINMENT);
        assertThat(r1.getOutcomeCode()).isEqualTo("PO3");

        EmmuIntentRouter.RoutedIntent r2 = router.route("Explain root cause for PSO2 attainment");
        assertThat(r2.getPrimaryIntent()).isEqualTo(EmmuIntent.ROOT_CAUSE);
        assertThat(r2.getOutcomeCode()).isEqualTo("PSO2");

        EmmuIntentRouter.RoutedIntent r3 = router.route("Show me CO2 attainment for Data Structures");
        assertThat(r3.getPrimaryIntent()).isEqualTo(EmmuIntent.OUTCOME_ATTAINMENT);
        assertThat(r3.getOutcomeCode()).isEqualTo("CO2");
    }

    @Test
    @DisplayName("Should route course ranking and ATR queries")
    void testRankingAndAtrRouting() {
        EmmuIntentRouter.RoutedIntent r1 = router.route("Show the lowest performing courses in CSE");
        assertThat(r1.getPrimaryIntent()).isEqualTo(EmmuIntent.COURSE_RANKING);

        EmmuIntentRouter.RoutedIntent r2 = router.route("List pending ATR actions for the recent semester");
        assertThat(r2.getPrimaryIntent()).isEqualTo(EmmuIntent.ATR_ACTION);

        EmmuIntentRouter.RoutedIntent r3 = router.route("Compare historical trend across last 3 batches");
        assertThat(r3.getPrimaryIntent()).isEqualTo(EmmuIntent.HISTORICAL_TREND);
    }
}
