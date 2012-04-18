package com.hystericalporpoises.dns;

import static org.junit.Assert.*;
import static com.hystericalporpoises.dns.DnsConfigurationBuilder.*;

import org.junit.Test;

import com.google.common.collect.Lists;

public class DnsConfigurationBuilderTest {

  @Test
  public void build() {
    ThreadLocalDnsConfiguration configuration = newBuilder()
      .map(hosts("www.google.com"), to("127.0.0.1"))
      .map(hosts("www.yahoo.com"), to("127.0.0.2")).build();
    IpToHostsMapping google = new IpToHostsMapping("127.0.0.1", Lists.newArrayList("www.google.com"));
    IpToHostsMapping yahoo = new IpToHostsMapping("127.0.0.2", Lists.newArrayList("www.yahoo.com"));
    assertEquals(Lists.newArrayList(google, yahoo), configuration.getMappings());
  }

}
