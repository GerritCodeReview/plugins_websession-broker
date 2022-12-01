Broker WebSession Plugin Configuration
======================

@PLUGIN@ parameters can be configured using Gerrit config file: $site_dir/etc/gerrit.config.

Sample config
---------------------

```
[plugin "websession-broker"]
        webSessionTopic = gerrit_web_session
```

Configuration parameters
---------------------

`plugin.websession-broker.webSessionTopic`
:   Name of the topic to use for publishing web session events.
    Default: gerrit\_web\_session

`plugin.websession-broker.numberOfThreads`
:   Number of threads used to send web session events via events-broker.
    Default: 1

`plugin.websession-broker.cleanupInterval`
:   Frequency of the expired web session cleanup operation.
    Value should use common time unit suffixes to express their setting:
    * h, hr, hour, hours
    * d, day, days
    * w, week, weeks (`1 week` is treated as `7 days`)
    * mon, month, months (`1 month` is treated as `30 days`)
    * y, year, years (`1 year` is treated as `365 days`)
    If a time unit suffix is not specified, `hours` is assumed.
    Time intervals smaller than one hour are not supported.
    Default: 24 hours

`plugin.websession-broker.replayAllSessions`
:   Replay all web sessions after Gerrit start. Replaying all web sessions
    after restart is useful when `web_sessions` cache is an in-memory cache.
    For persisted caches for example when cache.*.diskLimit is greater than
    zero or cache backed implementation is persisted(for example cache-chroniclemap)
    this can be set to false because we need to consume just the new web
    sessions.
    Default: true
