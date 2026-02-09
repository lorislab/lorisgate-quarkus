package org.lorislab.lorisgate.quarkus.deployment;

import org.lorislab.lorisgate.quarkus.deployment.devservices.LorisgateDevServicesConfig;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;

@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
@ConfigMapping(prefix = "lorislab.lorisgate")
public interface LorisgateBuildTimeConfig {

    /**
     * Default Dev services configuration.
     */
    @WithName("devservices")
    LorisgateDevServicesConfig devService();
}
