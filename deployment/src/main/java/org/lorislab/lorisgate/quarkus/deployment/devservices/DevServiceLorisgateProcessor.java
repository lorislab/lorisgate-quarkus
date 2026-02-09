package org.lorislab.lorisgate.quarkus.deployment.devservices;

import static io.quarkus.devservices.common.ConfigureUtil.configureSharedServiceLabel;
import static io.quarkus.devservices.common.ContainerLocator.locateContainerWithLabels;

import java.util.List;
import java.util.Map;

import org.lorislab.lorisgate.quarkus.deployment.LorisgateBuildTimeConfig;
import org.lorislab.lorisgate.quarkus.deployment.LorisgateProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.deployment.IsDevServicesSupportedByLaunchMode;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.builditem.*;
import io.quarkus.deployment.dev.devservices.DevServicesConfig;
import io.quarkus.devservices.common.ComposeLocator;
import io.quarkus.devservices.common.ConfigureUtil;
import io.quarkus.devservices.common.ContainerLocator;
import io.quarkus.runtime.LaunchMode;

@BuildSteps(onlyIf = { IsDevServicesSupportedByLaunchMode.class, DevServicesConfig.Enabled.class })
public class DevServiceLorisgateProcessor {

    private static final Logger log = LoggerFactory.getLogger(DevServiceLorisgateProcessor.class);

    private static final String DEFAULT_DOCKER_IMAGE = "ghcr.io/lorislab/lorisgate:main";
    public static final String PROP_LORISGATE_CLIENT_URL = "lorisgate.client.url";
    public static final int DEFAULT_LORISGATE_PORT = 8080;
    private static final String DEV_SERVICE_LABEL = "lorislab-dev-service-lorisgate";

    private static final ContainerLocator lorisgateContainerLocator = locateContainerWithLabels(DEFAULT_LORISGATE_PORT,
            DEV_SERVICE_LABEL);

    @BuildStep
    public DevServicesResultBuildItem startRedisContainers(LaunchModeBuildItem launchMode,
            DockerStatusBuildItem dockerStatusBuildItem,
            DevServicesComposeProjectBuildItem compose,
            List<DevServicesSharedNetworkBuildItem> sharedNetwork,
            LorisgateBuildTimeConfig lorisgateBuildTimeConfig,
            DevServicesConfig devServicesConfig) {

        var config = lorisgateBuildTimeConfig.devService();
        if (devServiceDisabled(dockerStatusBuildItem, config)) {
            return null;
        }

        boolean useSharedNetwork = DevServicesSharedNetworkBuildItem.isSharedNetworkRequired(devServicesConfig, sharedNetwork);

        return lorisgateContainerLocator.locateContainer(config.serviceName(), config.shared(), launchMode.getLaunchMode())
                .or(() -> ComposeLocator.locateContainer(compose,
                        List.of("lorisgate"),
                        DEFAULT_LORISGATE_PORT, launchMode.getLaunchMode(), useSharedNetwork))
                .map(containerAddress -> {
                    return DevServicesResultBuildItem.discovered()
                            .name(LorisgateProcessor.FEATURE_NAME)
                            .containerId(containerAddress.getId())
                            .config(Map.of(PROP_LORISGATE_CLIENT_URL, containerAddress.getUrl()))
                            .build();
                }).orElseGet(() -> DevServicesResultBuildItem.owned()
                        .feature(LorisgateProcessor.FEATURE_NAME)
                        .serviceName(config.serviceName())
                        .serviceConfig(config)
                        .startable(() -> createContainer(compose, config, useSharedNetwork, launchMode))
                        //                        .postStartHook(s -> ...)
                        .configProvider(Map.of(PROP_LORISGATE_CLIENT_URL, Startable::getConnectionInfo))
                        .build());
    }

    private Startable createContainer(DevServicesComposeProjectBuildItem composeProjectBuildItem,
            LorisgateDevServicesConfig config, boolean useSharedNetwork,
            LaunchModeBuildItem launchMode) {
        var container = new LorisgateContainer(DockerImageName.parse(config.imageName().orElse(DEFAULT_DOCKER_IMAGE)),
                config.port().orElse(0),
                composeProjectBuildItem.getDefaultNetworkId(),
                useSharedNetwork)
                .withEnv(config.containerEnv())
                .withSharedServiceLabel(launchMode.getLaunchMode(), config.serviceName());
        // enabled or disable container logs
        if (config.log()) {
            container.withLogConsumer(ContainerLogger.create(config.serviceName()));
        }
        return container;
    }

    private boolean devServiceDisabled(DockerStatusBuildItem dockerStatusBuildItem, LorisgateDevServicesConfig config) {
        if (!config.enabled().orElse(true)) {
            // explicitly disabled
            log.debug("Not starting dev services for Lorisgate, as it has been disabled in the config.");
            return true;
        }

        if (!dockerStatusBuildItem.isContainerRuntimeAvailable()) {
            log.warn(
                    "Docker isn't working, please configure the OIDC server.");
            return true;
        }
        return false;
    }

    private static class LorisgateContainer extends GenericContainer<LorisgateContainer> implements Startable {

        private final boolean useSharedNetwork;
        private final int fixedExposedPort;

        private String hostName = null;

        public LorisgateContainer(DockerImageName image, int fixedExposedPort, String serviceName, boolean useSharedNetwork) {
            super(image);
            log.debug("P6 docker image {}", image);
            this.fixedExposedPort = fixedExposedPort;
            this.useSharedNetwork = useSharedNetwork;

            this.withExposedPorts(DEFAULT_LORISGATE_PORT);

            if (serviceName != null) {
                this.withLabel(DEV_SERVICE_LABEL, serviceName);
            }

            // wait for start
            this.waitingFor(Wait.forHttp("/q/health"));
        }

        public String getExternalAddress(final int port) {
            return String.format("http://%s:%d", this.getHost(), this.getMappedPort(port));
        }

        @Override
        protected void configure() {
            super.configure();

            if (useSharedNetwork) {
                hostName = ConfigureUtil.configureSharedNetwork(this, "lorisgate");
                return;
            } else {
                withNetwork(Network.SHARED);
            }

            if (fixedExposedPort > 0) {
                addFixedExposedPort(fixedExposedPort, DEFAULT_LORISGATE_PORT);
            } else {
                addExposedPort(DEFAULT_LORISGATE_PORT);
            }
        }

        public int getPort() {
            if (useSharedNetwork) {
                return DEFAULT_LORISGATE_PORT;
            }
            if (fixedExposedPort > 0) {
                return fixedExposedPort;
            }
            return super.getFirstMappedPort();
        }

        public String getLorisgateHost() {
            return useSharedNetwork ? hostName : super.getHost();
        }

        public String getUrl() {
            return String.format("http://%s:%d", this.getLorisgateHost(), this.getPort());
        }

        public LorisgateContainer withSharedServiceLabel(LaunchMode launchMode, String serviceName) {
            return configureSharedServiceLabel(this, launchMode, DEV_SERVICE_LABEL, serviceName);
        }

        @Override
        public String getConnectionInfo() {
            return getUrl();
        }

        @Override
        public void close() {
            super.close();
        }
    }
}
