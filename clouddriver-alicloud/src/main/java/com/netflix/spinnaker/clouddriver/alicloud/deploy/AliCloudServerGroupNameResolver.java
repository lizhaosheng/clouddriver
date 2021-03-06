package com.netflix.spinnaker.clouddriver.alicloud.deploy;

import com.netflix.frigga.Names;
import com.netflix.spinnaker.clouddriver.alicloud.AliCloudProvider;
import com.netflix.spinnaker.clouddriver.alicloud.model.AliCloudServerGroup;
import com.netflix.spinnaker.clouddriver.helpers.AbstractServerGroupNameResolver;
import com.netflix.spinnaker.clouddriver.model.Cluster;
import com.netflix.spinnaker.clouddriver.model.ClusterProvider;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class AliCloudServerGroupNameResolver extends AbstractServerGroupNameResolver {

  private static final String ALICLOUD_PHASE = "ALICLOUD_DEPLOY";

  private final String accountName;
  private final String region;
  private final Collection<ClusterProvider> clusterProviders;

  public AliCloudServerGroupNameResolver(
      String accountName, String region, Collection<ClusterProvider> clusterProviders) {
    this.accountName = accountName;
    this.region = region;
    this.clusterProviders = clusterProviders;
  }

  @Override
  public String getPhase() {
    return ALICLOUD_PHASE;
  }

  @Override
  public String getRegion() {
    return region;
  }

  @Override
  public List<TakenSlot> getTakenSlots(String clusterName) {
    Cluster cluster = getCluster(clusterProviders, accountName, clusterName);
    if (cluster == null) {
      return Collections.emptyList();
    }
    ArrayList<AliCloudServerGroup> serverGroups =
        (ArrayList<AliCloudServerGroup>)
            cluster.getServerGroups().stream()
                .filter(it -> region.equals(it.getRegion()))
                .collect(Collectors.toList());
    List<TakenSlot> takenSlots = new ArrayList<>();
    serverGroups.stream()
        .forEach(
            it ->
                takenSlots.add(
                    new TakenSlot(
                        it.getName(),
                        Names.parseName(it.getName()).getSequence(),
                        new Date(it.getCreatedTime()))));
    return takenSlots;
  }

  private static Cluster getCluster(
      Collection<ClusterProvider> clusterProviders, String accountName, String clusterName) {
    String app = Names.parseName(clusterName).getApp();
    List<ClusterProvider> providers =
        clusterProviders.stream()
            .filter(it -> AliCloudProvider.ID.equals(it.getCloudProviderId()))
            .collect(Collectors.toList());
    if (providers.size() == 1) {
      ClusterProvider provider = providers.get(0);
      Cluster cluster = provider.getCluster(app, accountName, clusterName);
      return cluster;
    } else {
      return null;
    }
  }
}
