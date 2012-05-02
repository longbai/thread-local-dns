package com.theotherian.dns;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutionException;

import org.apache.log4j.Logger;
import org.xbill.DNS.spi.DNSJavaNameService;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * Name service that uses both thread local and global representations of DNS lookups.<br>
 * <p>
 * Order of resolution is as follows:
 * <ul>
 *   <li>First, see if the thread local cache has an ip for a host</li>
 *   <li>Next, see if there was an ip for that host in the hosts file</li>
 *   <li>Otherwise, look up from a global cache</li>
 * </ul>
 * Entries do not expire from cache, which is the default behavior in the JVM.
 * </p>
 * @author isimpson
 *
 */
public class ThreadLocalNameService extends DNSJavaNameService {

  private static Logger LOGGER = Logger.getLogger(ThreadLocalNameService.class);

  static {
    LOGGER.info("Thread Local DNS Name Service loaded");
  }

  private InheritableThreadLocal<LoadingCache<String, InetAddress[]>> threadLocalDnsCache = new InheritableThreadLocal<LoadingCache<String,InetAddress[]>>() {
    @Override
    protected LoadingCache<String, InetAddress[]> initialValue() {
      return CacheBuilder.newBuilder().build(new CacheLoader<String, InetAddress[]>() {

        @Override
        public InetAddress[] load(String key) throws Exception {
          LOGGER.debug("Looking up " + key);
          if (OverrideNameServiceManager.hasIpForHost(key)) {
            String ipAddress = OverrideNameServiceManager.getIpForHost(key);
            LOGGER.debug("Found thread local override for " + key + " of " + ipAddress);
            return convertToInetAddress(ipAddress);
          }
          else if (HostsFileResolver.hasOverride(key)) {
            String ipAddress = HostsFileResolver.getOverride(key);
            LOGGER.debug("Found hosts entry for " + key + " of " + ipAddress);
            return convertToInetAddress(ipAddress);
          }
          else {
            LOGGER.debug("No override found for " + key + ", looking up from global cache");
            return dnsCache.get(key);
          }
        }

        private InetAddress[] convertToInetAddress(String ipAddress) throws UnknownHostException {
          byte[] ipAsBytes = TextToNumeric.convert(ipAddress);
          InetAddress[] address = new InetAddress[1];
          address[0] = InetAddress.getByAddress(ipAsBytes);
          return address;
        }

      });
    }
  };

  private LoadingCache<String, InetAddress[]> dnsCache = CacheBuilder.newBuilder()
      .build(new CacheLoader<String, InetAddress[]>() {

        @Override
        public InetAddress[] load(String key) throws Exception {
          return normalLookup(key);
        }

      });


  @VisibleForTesting
  static boolean isLocal(InetAddress ip) {
    return ip.isLinkLocalAddress() || ip.isLoopbackAddress() || ip.isSiteLocalAddress();
  }

  public ThreadLocalNameService() {
    super();
  }

  private InetAddress[] normalLookup(String hostname) throws UnknownHostException {
    return super.lookupAllHostAddr(hostname);
  }


  @Override
  public InetAddress[] lookupAllHostAddr(String hostname) throws UnknownHostException {
    try {
      if (hostname == null || hostname.isEmpty()) {
        throw new UnsupportedOperationException("Invalid lookup of null or blank hostname");
      }
      // lower case the hostname since DNS is case insensitive
      return threadLocalDnsCache.get().get(hostname.toLowerCase());
    }
    catch (ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

}
