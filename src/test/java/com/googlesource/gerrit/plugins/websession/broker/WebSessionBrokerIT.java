// Copyright (C) 2021 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.googlesource.gerrit.plugins.websession.broker;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static com.google.gerrit.httpd.CacheBasedWebSession.ACCOUNT_COOKIE;
import static com.google.gerrit.httpd.WebSessionManager.CACHE_NAME;

import com.gerritforge.gerrit.eventbroker.BrokerApi;
import com.gerritforge.gerrit.eventbroker.EventMessage;
import com.gerritforge.gerrit.eventbroker.TopicSubscriber;
import com.google.common.cache.Cache;
import com.google.gerrit.acceptance.GerritConfig;
import com.google.gerrit.acceptance.LightweightPluginDaemonTest;
import com.google.gerrit.acceptance.TestPlugin;
import com.google.gerrit.extensions.registration.DynamicItem;
import com.google.gerrit.extensions.registration.DynamicMap;
import com.google.gerrit.extensions.registration.PluginName;
import com.google.gerrit.httpd.WebSessionManager;
import com.google.gerrit.server.git.WorkQueue;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Module;
import java.io.IOException;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import javax.servlet.http.HttpServletResponse;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.Before;
import org.junit.Test;

@TestPlugin(
    name = "websession-broker",
    httpModule = "com.googlesource.gerrit.plugins.websession.broker.WebSessionBrokerIT$TestModule")
public class WebSessionBrokerIT extends LightweightPluginDaemonTest {

  @Inject private DynamicMap<Cache<?, ?>> cacheMap;
  private CloseableHttpClient httpClient;
  private BasicCookieStore httpCookieStore;

  @Before
  public void createHttpClient() {
    httpCookieStore = new BasicCookieStore();
    httpClient =
        HttpClientBuilder.create()
            .disableRedirectHandling()
            .setDefaultCookieStore(httpCookieStore)
            .build();
  }

  @Test
  @GerritConfig(name = "auth.type", value = "DEVELOPMENT_BECOME_ANY_ACCOUNT")
  public void shouldNotUsePersistedWebSessionsCache() throws IOException {
    @SuppressWarnings("unchecked")
    Cache<String, WebSessionManager.Val> persisted_web_sessions =
        (Cache<String, WebSessionManager.Val>) cacheMap.get(PluginName.GERRIT, CACHE_NAME);

    Cookie cookie = loginAndGetCookie(admin.id().get());

    assertWithMessage(
            String.format(
                "Account cookie was NOT expected to be stored in the persistent '%s' cache",
                CACHE_NAME))
        .that(persisted_web_sessions.getIfPresent(cookie.getValue()))
        .isNull();
  }

  private Cookie loginAndGetCookie(Integer accountId) throws IOException {
    HttpGet httpGet = new HttpGet(canonicalWebUrl.get() + "login?account_id=" + accountId);
    HttpResponse loginResponse = httpClient.execute(httpGet);
    assertThat(loginResponse.getStatusLine().getStatusCode())
        .isEqualTo(HttpServletResponse.SC_MOVED_TEMPORARILY);
    Optional<Cookie> gerritAccount =
        httpCookieStore.getCookies().stream()
            .filter(k -> k.getName().equals(ACCOUNT_COOKIE))
            .findFirst();

    assertThat(gerritAccount.isPresent()).isTrue();
    return gerritAccount.get();
  }

  private static class NoopBrokerApi implements BrokerApi {

    @Override
    public boolean send(String s, EventMessage eventMessage) {
      return true;
    }

    @Override
    public void receiveAsync(String s, Consumer<EventMessage> consumer) {}

    @Override
    public Set<TopicSubscriber> topicSubscribers() {
      return Collections.emptySet();
    }

    @Override
    public void disconnect() {}

    @Override
    public void replayAllEvents(String s) {}
  }

  public static class TestModule extends AbstractModule {

    private Module baseModule;

    @Inject
    public TestModule(WorkQueue workQueue, BrokerBasedWebSessionConfiguration configuration) {
      baseModule = new BrokerBasedWebSession.Module(workQueue, configuration);
    }

    @Override
    protected void configure() {
      DynamicItem.itemOf(binder(), BrokerApi.class);
      DynamicItem.bind(binder(), BrokerApi.class).to(NoopBrokerApi.class);
      install(baseModule);
    }
  }
}
