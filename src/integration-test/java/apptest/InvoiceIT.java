package apptest;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;

import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.invoicesender.Application;
import se.sundsvall.invoicesender.integration.raindance.RaindanceIntegration;

@WireMockAppTestSuite(files = "classpath:/InvoiceIT", classes = Application.class)
class InvoiceIT extends AbstractAppTest {

    static GenericContainer<?> samba = new GenericContainer<>("dperson/samba");

    @BeforeAll
    static void setUp() {
        samba.setEnv(List.of(
            "TZ=Europe/Stockholm",
            "RECYCLE=False",
            "NMBD=True",
            "WORKGROUP=HOME",
            "WIDELINKS=True"
        ));
        samba.setPortBindings(List.of("1139:139"));
        samba.setExposedPorts(List.of(1139));
        samba.setCommand(
            "-g 'vfs objects = catia fruit'",
            "-g 'log level = 2'",
            "-s 'mnt;/mnt;yes;no;yes'",
            "-u 'user;p4ssw0rd'",
            "-p"
        );

        // Use a custom wait strategy, due to https://github.com/testcontainers/testcontainers-java/issues/4125
        samba.setWaitStrategy(new HostPortWaitStrategy() {

            @Override
            protected Set<Integer> getLivenessCheckPorts() {
                return super.getLivenessCheckPorts().stream()
                    .filter(port -> port > 0)
.peek(port -> System.err.println("FOUND PORT " + port))
                    .collect(Collectors.toSet());
            }
        });

        //try {
            samba.start();
        //} catch (Exception e) {
        //    System.err.println(e.getMessage());
        //}
    }

    @AfterAll
    static void tearDown() {
        samba.stop();
    }

    @Autowired
    private RaindanceIntegration raindanceIntegration;

    @Test
    void test1() throws Exception {
        raindanceIntegration.readBatch(LocalDate.now());
    }
}
