package org.lorislab.lorisgate.quarkus.deployment.devservices;

import static io.quarkus.devservices.common.ConfigureUtil.configureSharedServiceLabel;
import static io.quarkus.devservices.common.ContainerLocator.locateContainerWithLabels;
import static io.quarkus.runtime.LaunchMode.DEVELOPMENT;
import static org.lorislab.lorisgate.quarkus.deployment.LorisgateProcessor.FEATURE_NAME;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Supplier;

import org.lorislab.lorisgate.quarkus.deployment.LorisgateBuildTimeConfig;
import org.lorislab.lorisgate.quarkus.runtime.LorisgateServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.deployment.IsDevServicesSupportedByLaunchMode;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.builditem.*;
import io.quarkus.deployment.console.ConsoleInstalledBuildItem;
import io.quarkus.deployment.console.StartupLogCompressor;
import io.quarkus.deployment.dev.devservices.DevServicesConfig;
import io.quarkus.deployment.logging.LoggingSetupBuildItem;
import io.quarkus.devservices.common.ConfigureUtil;
import io.quarkus.devservices.common.ContainerLocator;
import io.quarkus.devservices.common.ContainerShutdownCloseable;
import io.quarkus.runtime.LaunchMode;

@BuildSteps(onlyIf = { IsDevServicesSupportedByLaunchMode.class, DevServicesConfig.Enabled.class })
public class DevServiceLorisgateProcessor {

    private static final Logger log = LoggerFactory.getLogger(DevServiceLorisgateProcessor.class);

    private static final String DEFAULT_LORISLAB_CONTAINER_IMAGE = "ghcr.io/lorislab/lorisgate";
    private static final DockerImageName LORISGATE_IMAGE_NAME = DockerImageName.parse(DEFAULT_LORISLAB_CONTAINER_IMAGE)
            .withTag("main");

    public static final int LORISGATE_EXPOSED_PORT = 8080;
    private static final String DEV_SERVICE_LABEL = "lorislab-dev-service-lorisgate";

    private static final ContainerLocator lorisgateContainerLocator = locateContainerWithLabels(LORISGATE_EXPOSED_PORT,
            DEV_SERVICE_LABEL);

    private static volatile DevServicesResultBuildItem.RunningDevService devServices;
    private static volatile LorisgateDevServicesConfig capturedDevServicesConfiguration;
    private static volatile boolean first = true;

    @BuildStep
    public DevServicesResultBuildItem startContainers(LaunchModeBuildItem launchMode,
            DockerStatusBuildItem dockerStatusBuildItem,
            DevServicesComposeProjectBuildItem composeProjectBuildItem,
            LoggingSetupBuildItem loggingSetupBuildItem,
            Optional<ConsoleInstalledBuildItem> consoleInstalledBuildItem,
            List<DevServicesSharedNetworkBuildItem> sharedNetwork,
            CuratedApplicationShutdownBuildItem closeBuildItem,
            LorisgateBuildTimeConfig lorisgateBuildTimeConfig,
            DevServicesConfig devServicesConfig) {

        LorisgateDevServicesConfig currentDevServicesConfiguration = lorisgateBuildTimeConfig.devService();

        if (devServices != null) {
            boolean restartRequired = !currentDevServicesConfiguration.equals(capturedDevServicesConfiguration);
            if (!restartRequired) {
                return devServices.toBuildItem();
            }
            try {
                devServices.close();
            } catch (Throwable e) {
                log.error("Failed to stop Lorisgate container", e);
            }
            devServices = null;
            capturedDevServicesConfiguration = null;
        }

        capturedDevServicesConfiguration = currentDevServicesConfiguration;

        StartupLogCompressor compressor = new StartupLogCompressor(
                (launchMode.isTest() ? "(test) " : "") + "Lorisgate Dev Services Starting:", consoleInstalledBuildItem,
                loggingSetupBuildItem);

        try {

            boolean useSharedNetwork = DevServicesSharedNetworkBuildItem.isSharedNetworkRequired(devServicesConfig,
                    sharedNetwork);

            devServices = startContainer(dockerStatusBuildItem, composeProjectBuildItem,
                    launchMode.getLaunchMode(),
                    currentDevServicesConfiguration,
                    useSharedNetwork, devServicesConfig.timeout());
            if (devServices == null) {
                compressor.closeAndDumpCaptured();
            } else {
                compressor.close();
            }

        } catch (Throwable t) {
            compressor.closeAndDumpCaptured();
            throw new RuntimeException(t);
        }

        if (devServices == null) {
            return null;
        }

        if (first) {
            first = false;
            Runnable closeTask = () -> {
                if (devServices != null) {
                    try {
                        devServices.close();
                    } catch (Throwable t) {
                        log.error("Failed to stop MockServer", t);
                    }
                }
                first = true;
                devServices = null;
                capturedDevServicesConfiguration = null;
            };
            closeBuildItem.addCloseTask(closeTask, true);
        }
        if (devServices.isOwner()) {
            log.info("The lorisgate server is ready to accept connections on http://{}:{}",
                    devServices.getConfig().get(LorisgateServerConfig.CLIENT_HOST),
                    devServices.getConfig().get(LorisgateServerConfig.CLIENT_PORT));
        }
        return devServices.toBuildItem();
    }

    private DevServicesResultBuildItem.RunningDevService startContainer(DockerStatusBuildItem dockerStatusBuildItem,
            DevServicesComposeProjectBuildItem composeProjectBuildItem,
            LaunchMode launchMode,
            LorisgateDevServicesConfig devServicesConfig, boolean useSharedNetwork, Optional<Duration> timeout) {

        if (!devServicesConfig.enabled().orElse(true)) {
            // explicitly disabled
            log.debug("Not starting devservices for rest client as it has been disabled in the config");
            return null;
        }

        if (!dockerStatusBuildItem.isContainerRuntimeAvailable()) {
            log.warn("Please configure or get a working Lorisgate instance");
            return null;
        }

        DockerImageName tmp = LORISGATE_IMAGE_NAME;
        if (devServicesConfig.imageName().isPresent()) {
            tmp = DockerImageName.parse(devServicesConfig.imageName().get())
                    .asCompatibleSubstituteFor(DEFAULT_LORISLAB_CONTAINER_IMAGE);
        }
        DockerImageName dockerImageName = tmp;

        Supplier<DevServicesResultBuildItem.RunningDevService> defaultMockServerSupplier = () -> {
            LorisgateContainer container = new LorisgateContainer(dockerImageName,
                    devServicesConfig.port(),
                    launchMode == DEVELOPMENT ? devServicesConfig.serviceName() : null, useSharedNetwork);
            timeout.ifPresent(container::withStartupTimeout);

            // enabled or disable container logs
            if (devServicesConfig.log()) {
                container.withLogConsumer(ContainerLogger.create(devServicesConfig.serviceName()));
            }

            if (devServicesConfig.containerEnv() != null && !devServicesConfig.containerEnv().isEmpty()) {
                container.withEnv(devServicesConfig.containerEnv());
            }

            // mount directory with mocks
            if (devServicesConfig.volumeMounts() != null && !devServicesConfig.volumeMounts().isEmpty()) {

                for (var mount : devServicesConfig.volumeMounts().entrySet()) {
                    String configDir = mount.getKey();
                    String path = mount.getValue();
                    if (devServicesConfig.configClassPath()) {
                        container.withClasspathResourceMapping(configDir, "/" + path, BindMode.READ_ONLY);
                        log.info("Lorislab configuration class-path directory '{}' mount to '/{}' container directory.",
                                configDir, path);
                    } else {
                        if (Files.isDirectory(Path.of(configDir))) {
                            container.withFileSystemBind(configDir, "/" + path, BindMode.READ_ONLY);
                            log.info("Lorislab configuration local directory '{}' mount to '/{}' container directory.",
                                    configDir, path);
                        } else {
                            log.warn("Lorislab configuration local directory '{}' is not directory.", configDir);
                        }
                    }
                }
            }

            // enable test-container reuse
            if (devServicesConfig.reuse()) {
                container.withReuse(true);
            }

            // start test-container
            container.start();

            return new DevServicesResultBuildItem.RunningDevService(FEATURE_NAME, container.getContainerId(),
                    new ContainerShutdownCloseable(container, FEATURE_NAME),
                    Map.of(LorisgateServerConfig.HOST, container.getDevHost(),
                            LorisgateServerConfig.PORT, "" + container.getDevPort(),
                            LorisgateServerConfig.ENDPOINT, container.getDevEndpoint(),
                            LorisgateServerConfig.CLIENT_HOST, container.getHost(),
                            LorisgateServerConfig.CLIENT_PORT, "" + container.getServerPort()));
        };

        return lorisgateContainerLocator
                .locateContainer(devServicesConfig.serviceName(), devServicesConfig.shared(), launchMode)
                .map(containerAddress -> {

                    return new DevServicesResultBuildItem.RunningDevService(FEATURE_NAME, containerAddress.getId(),
                            null,
                            Map.of(LorisgateServerConfig.HOST, containerAddress.getHost(),
                                    LorisgateServerConfig.PORT, "" + containerAddress.getPort(),
                                    LorisgateServerConfig.CLIENT_HOST, containerAddress.getHost(),
                                    LorisgateServerConfig.CLIENT_PORT, "" + containerAddress.getPort(),
                                    LorisgateServerConfig.ENDPOINT, String.format("http://%s:%d",
                                            containerAddress.getHost(), containerAddress.getPort())));
                })
                .orElseGet(defaultMockServerSupplier);
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
        private final OptionalInt fixedExposedPort;

        private String hostName = null;

        public LorisgateContainer(DockerImageName image, OptionalInt fixedExposedPort, String serviceName,
                boolean useSharedNetwork) {
            super(image);
            log.debug("Lorisgate docker image {}", image);
            this.useSharedNetwork = useSharedNetwork;
            this.fixedExposedPort = fixedExposedPort;

            if (fixedExposedPort.isPresent()) {
                addFixedExposedPort(fixedExposedPort.getAsInt(), LORISGATE_EXPOSED_PORT);
            } else {
                addExposedPort(LORISGATE_EXPOSED_PORT);
            }

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

            if (fixedExposedPort.isPresent()) {
                addFixedExposedPort(fixedExposedPort.getAsInt(), LORISGATE_EXPOSED_PORT);
            } else {
                addExposedPort(LORISGATE_EXPOSED_PORT);
            }
        }

        public int getPort() {
            if (useSharedNetwork) {
                return LORISGATE_EXPOSED_PORT;
            }
            if (fixedExposedPort.isPresent()) {
                return fixedExposedPort.getAsInt();
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

        public String getDevHost() {
            return useSharedNetwork ? hostName : super.getHost();
        }

        public String getDevEndpoint() {
            if (useSharedNetwork) {
                return String.format("http://%s:%d", hostName, LORISGATE_EXPOSED_PORT);
            }
            return String.format("http://%s:%d", getHost(), getMappedPort(LORISGATE_EXPOSED_PORT));
        }

        public int getDevPort() {
            if (useSharedNetwork) {
                return LORISGATE_EXPOSED_PORT;
            }

            if (fixedExposedPort.isPresent()) {
                return fixedExposedPort.getAsInt();
            }
            return getServerPort();
        }

        public Integer getServerPort() {
            return getMappedPort(LORISGATE_EXPOSED_PORT);
        }
    }
}
