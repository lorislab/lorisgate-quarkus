package org.lorislab.lorisgate.quarkus.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

public class LorisgateProcessor {

    public static final String FEATURE_NAME = "lorisgate";

    @BuildStep
    public FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE_NAME);
    }

}
