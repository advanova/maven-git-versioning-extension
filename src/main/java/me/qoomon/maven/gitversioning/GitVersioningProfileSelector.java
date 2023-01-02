package me.qoomon.maven.gitversioning;

import org.apache.maven.model.Profile;
import org.apache.maven.model.building.ModelProblemCollector;
import org.apache.maven.model.profile.DefaultProfileSelector;
import org.apache.maven.model.profile.ProfileActivationContext;
import org.apache.maven.model.profile.ProfileSelector;
import org.codehaus.plexus.component.annotations.Component;
import org.slf4j.Logger;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.*;
import static org.slf4j.LoggerFactory.getLogger;

@Named
@Singleton
@Component(role = ProfileSelector.class, hint = "default")
public class GitVersioningProfileSelector extends DefaultProfileSelector {

    final private Logger logger = getLogger(GitVersioningProfileSelector.class);

    @Inject
    private ContextProvider contextProvider;

    @Override
    public List<Profile> getActiveProfiles(Collection<Profile> profiles, ProfileActivationContext context, ModelProblemCollector problems) {
        List<Profile> activeProfiles = super.getActiveProfiles(profiles, context, problems);
        if (context.getProjectDirectory() == null) {
            return activeProfiles;
        }

        final Map<String, Boolean> desiredProfileStateMap;
        try {
            desiredProfileStateMap = contextProvider.getPatchMatch().getPatchDescription().profiles;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        return profiles.stream().filter(p -> {
            if (desiredProfileStateMap.containsKey(p.getId())) {
                logger.info("Explicit configuration for {}: {}", p.getId(), desiredProfileStateMap.get(p.getId()));
                //we have an explicit override in the configuration, use it
                return desiredProfileStateMap.get(p.getId());
            } else {
                logger.info("Implicit configuration for {}: {}", p.getId(), activeProfiles.contains(p));
                // nothing configured, use whatever maven got us
                return activeProfiles.contains(p);
            }
        }).collect(Collectors.toList());
    }
}
