package net.thucydides.core.reports

import net.thucydides.core.model.TestResult
import spock.lang.Specification
import static net.thucydides.core.util.TestResources.directoryInClasspathCalled

class WhenObtainingResultSummariesFromTestOutcomes extends Specification {
    def currentLocale = Locale.getDefault()

    def setup() {
        Locale.setDefault(Locale.US)
    }

    def cleanup() {
        Locale.setDefault(currentLocale)
    }
    def loader = new TestOutcomeLoader()

    def "should count the number of successful tests in a set"() {
        when:
            def testOutcomes = TestOutcomeLoader.testOutcomesIn(directoryInClasspathCalled("/tagged-test-outcomes"));
        then:
            testOutcomes.total == 3
    }

    def "should determine the correct overall result for a set of tests"() {
        when:
            TestOutcomes testOutcomes = TestOutcomeLoader.testOutcomesIn(directoryInClasspathCalled(directory));
        then:
            testOutcomes.result == result
        where:
            directory                                  | result
            "/test-outcomes/all-successful"            | TestResult.SUCCESS
            "/test-outcomes/containing-failure"        | TestResult.FAILURE
            "/test-outcomes/containing-nostep-errors"  | TestResult.FAILURE
            "/test-outcomes/containing-errors"         | TestResult.ERROR
            "/test-outcomes/containing-pending"        | TestResult.PENDING
            "/test-outcomes/containing-skipped"        | TestResult.SUCCESS
    }

    def "should find the total number of tests with a given result in a test outcome set"() {
        when:
            def testOutcomes = TestOutcomeLoader.testOutcomesIn(directoryInClasspathCalled(directory));
        then:
            testOutcomes.totalTests.withResult("success") == successCount &&
            testOutcomes.totalTests.withResult("failure") == failureCount &&
            testOutcomes.totalTests.withResult("error") == errorCount &&
            testOutcomes.totalTests.withResult("pending") == pendingCount &&
            testOutcomes.totalTests.withResult("skipped") == skipCount
            testOutcomes.totalTests.withIndeterminateResult() == indeterminateCount
        where:
            directory                                  | successCount | failureCount | errorCount   | pendingCount | skipCount  | indeterminateCount
            "/test-outcomes/all-successful"            | 3            | 0            | 0            | 0            | 0          | 0
            "/test-outcomes/containing-failure"        | 1            | 1            | 0            | 1            | 0          | 1
            "/test-outcomes/containing-nostep-errors"  | 2            | 3            | 1            | 2            | 0          | 2
            "/test-outcomes/containing-errors"         | 1            | 0            | 2            | 0            | 0          | 0
            "/test-outcomes/containing-pending"        | 2            | 0            | 0            | 1            | 0          | 1
            "/test-outcomes/containing-skipped"        | 3            | 0            | 0            | 0            | 1          | 1
    }

    def "should find the total number of tests with a given result and a given execution type (manual/automated)"() {
        when:
            def testOutcomes = TestOutcomeLoader.testOutcomesIn(directoryInClasspathCalled("/test-outcomes/all-successful"));
        then:
            testOutcomes.count("automated").withResult("success") == 2
        and:
            testOutcomes.count("manual").withResult("success") == 1
    }

    def "should count the number steps in a set of test outcomes"() {
        when:
           def testOutcomes = TestOutcomeLoader.testOutcomesIn(directoryInClasspathCalled("/tagged-test-outcomes"));
        then:
            testOutcomes.stepCount == 17
    }

    def "should calculate the percentage of passing steps"() {
        when:
            def testOutcomes = TestOutcomeLoader.testOutcomesIn(directoryInClasspathCalled(directory));
        then:
            testOutcomes.percentSteps.withResult("success") == percentagePassing &&
            testOutcomes.percentSteps.withResult("failure")== percentageFailing &&
            testOutcomes.percentSteps.withResult("pending") == percentagePending
            testOutcomes.percentSteps.withIndeterminateResult() == percentagePending
        where:
            directory                                  | percentagePassing | percentageFailing  | percentagePending
            "/test-outcomes/all-successful"            | 1.0               | 0.0                | 0.0
            "/test-outcomes/containing-failure"        | 0.32              | 0.24               | 0.44
            "/test-outcomes/containing-pending"        | 0.6               | 0.0                | 0.4
            "/test-outcomes/all-pending"               | 0.0               | 0.0                | 1.0
    }

    def "should calculate the percentage of passing tests"() {
        when:
            def testOutcomes = TestOutcomeLoader.testOutcomesIn(directoryInClasspathCalled(directory));
        then:
            testOutcomes.proportion.withResult("success") == percentagePassing &&
            testOutcomes.proportion.withResult("failure")== percentageFailing &&
            testOutcomes.proportion.withResult("pending") == percentagePending
            testOutcomes.proportion.withIndeterminateResult() == percentagePending
        where:
        directory                                  | percentagePassing | percentageFailing  | percentagePending
        "/test-outcomes/all-successful"            | 1.0               | 0.0                | 0.0
        "/test-outcomes/containing-failure"        | 0.3333333333333333| 0.3333333333333333 | 0.3333333333333333
        "/test-outcomes/containing-pending"        | 0.6666666666666666| 0.0                | 0.3333333333333333
        "/test-outcomes/all-pending"               | 0.0               | 0.0                | 1.0
    }

    def "should calculate the percentage of manual and automated passing steps"() {
        when:
            def testOutcomes = TestOutcomeLoader.testOutcomesIn(directoryInClasspathCalled(directory));
        then:
            testOutcomes.proportionalStepsOf("manual").withResult("success") == percentageManual &&
            testOutcomes.proportionalStepsOf("automated").withResult("success")== percentageAutomated
        where:
            directory                                  | percentageManual  | percentageAutomated
            "/test-outcomes/all-successful"            | 0.3333333333333333| 0.6666666666666666
    }

    def "should provide a formatted version of the passing coverage"() {
        when:
            def testOutcomes = TestOutcomeLoader.testOutcomesIn(directoryInClasspathCalled("/test-outcomes/containing-failure"));
        then:
            testOutcomes.formatted.percentTests("any").withResult("success") == "33.3%"
        and:
            testOutcomes.formatted.percentTests().withResult("success") == "33.3%"
    }

    def "should provide a formatted version of the passing step coverage"() {
        when:
            def testOutcomes = TestOutcomeLoader.testOutcomesIn(directoryInClasspathCalled("/test-outcomes/containing-failure"));
        then:
            testOutcomes.formatted.percentSteps("any").withResult("success") == "32%"
        and:
            testOutcomes.formatted.percentSteps().withResult("success") == "32%"
    }

    def "should provide a formatted version of the failing coverage metrics"() {
        when:
            def testOutcomes = TestOutcomeLoader.testOutcomesIn(directoryInClasspathCalled("/test-outcomes/containing-failure"));
        then:
            testOutcomes.formatted.percentTests.withResult("failure") == "33.3%"
        and:
            testOutcomes.formatted.percentSteps.withResult("failure") == "24%"

    }

    def "should provide a formatted version of the pending coverage metrics"() {
        when:
            def testOutcomes = TestOutcomeLoader.testOutcomesIn(directoryInClasspathCalled("/test-outcomes/containing-failure"));
        then:
            testOutcomes.formatted.percentSteps.withResult("pending") == "44%"
        and:
            testOutcomes.formatted.percentTests.withResult("pending") == "33.3%"
        and:
            testOutcomes.formatted.percentSteps.withIndeterminateResult() == "44%"
        and:
            testOutcomes.formatted.percentTests.withIndeterminateResult() == "33.3%"
    }

    def "should return 0% passing coverage if there are no steps"() {
        when:
            def testOutcomes = TestOutcomeLoader.testOutcomesIn(directoryInClasspathCalled("/test-outcomes/with-no-steps"));
        then:
            testOutcomes.formatted.percentSteps.withResult("success") == "0%"
        and:
            testOutcomes.formatted.percentTests.withResult("success") == "0%"
    }

    def "should return 0% failing coverage if there are no steps"() {
        when:
            def testOutcomes = TestOutcomeLoader.testOutcomesIn(directoryInClasspathCalled("/test-outcomes/with-no-steps"));
        then:
            testOutcomes.formatted.percentTests.withResult("failure") == "0%"
    }

    def "should return 100% pending coverage if there are no steps"() {
        when:
            def testOutcomes = TestOutcomeLoader.testOutcomesIn(directoryInClasspathCalled("/test-outcomes/with-no-steps"));
        then:
            testOutcomes.formatted.percentTests.withResult("pending") == "100%"
    }


    def "should count lines in data-driven tests as individual tests"() {
        when:
            def testOutcomes = TestOutcomeLoader.testOutcomesIn(directoryInClasspathCalled("/test-outcomes/datadriven"));
        then:
            testOutcomes.total == 14
            testOutcomes.totalTestScenarios == 2
    }


    def "should count results in lines of data-driven tests as individual tests"() {
        when:
            def testOutcomes = TestOutcomeLoader.testOutcomesIn(directoryInClasspathCalled("/test-outcomes/datadriven"));
        then:
            testOutcomes.totalTests.withResult("success") == 12
            testOutcomes.totalTests.withResult("failure") == 2
    }

    def "should count percentage results in lines of data-driven tests as individual tests"() {
        when:
            def testOutcomes = TestOutcomeLoader.testOutcomesIn(directoryInClasspathCalled("/test-outcomes/datadriven"));
        then:
            testOutcomes.formatted.percentSteps.withResult("success") == "85.7%"
            testOutcomes.formatted.percentSteps.withResult("failure") == "14.3%"
    }


    def "should count results correctly in mixed data-driven and normal tests"() {
        when:
            def testOutcomes = TestOutcomeLoader.testOutcomesIn(directoryInClasspathCalled("/test-outcomes/somedatadriven"));
        then:
            testOutcomes.totalTests.withResult("success") == 1
            testOutcomes.totalTests.withResult("failure") == 2
            testOutcomes.totalTests.withResult("pending") == 3
            testOutcomes.totalTests.withResult("error") == 1
            testOutcomes.total  == 7
            testOutcomes.totalTestScenarios  == 4
    }

    def "should count percentage results correctly in mixed data-driven and normal tests"() {
        when:
            def testOutcomes = TestOutcomeLoader.testOutcomesIn(directoryInClasspathCalled("/test-outcomes/somedatadriven"));
        then:
            testOutcomes.hasDataDrivenTests()
            testOutcomes.totalDataRows == 5
            testOutcomes.proportion.withResult("success") == 0.14285714285714285
            testOutcomes.proportion.withResult("failure") == 0.2857142857142857
            testOutcomes.proportion.withResult("error") == 0.14285714285714285
            testOutcomes.proportion.withResult("pending") == 0.42857142857142855
    }

    def "should count percentage results correctly with no results"() {
        when:
            def testOutcomes = TestOutcomeLoader.testOutcomesIn(directoryInClasspathCalled("/test-outcomes"));
        then:
            testOutcomes.proportion.withResult("success") == 0.0
            testOutcomes.proportion.withResult("failure") == 0.0
            testOutcomes.proportion.withResult("error") == 0.0
            testOutcomes.proportion.withResult("pending") == 0.0
    }
}