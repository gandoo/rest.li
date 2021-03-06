/*
   Copyright (c) 2012 LinkedIn Corp.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package com.linkedin.d2.discovery.stores.mock;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;
import com.linkedin.d2.discovery.event.PropertyEventBus;
import com.linkedin.d2.discovery.event.PropertyEventPublisher;
import com.linkedin.d2.discovery.stores.PropertyStore;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MockStore<T> implements PropertyEventPublisher<T>, PropertyStore<T>
{
  private volatile PropertyEventBus<T>  _eventBus;
  private volatile boolean     _shutdown;

  /**
   * Concurrency considerations: updates to _properties (all of which are published), or
   * reads from _properties which supply a value to be published (as in startPublishing)
   * must be atomic with the corresponding publish operation. Else events might be
   * published to the bus in an order which does not reflect the order of updates.
   *
   * All access to _properties or _publishing must be synchronized with _lock.
   */
  private final Object         _lock = new Object();
  private final Map<String, T> _properties;
  private final Set<String>    _publishing;

  public MockStore()
  {
    _properties = new HashMap<String, T>();
    _publishing = new HashSet<String>();
    _shutdown = false;
  }

  @Override
  public void setBus(PropertyEventBus<T> bus)
  {
    _eventBus = bus;
  }

  @Override
  public T get(String listenTo)
  {
    synchronized (_lock)
    {
      return _properties.get(listenTo);
    }
  }

  @Override
  public void put(String listenTo, T discoveryProperties)
  {
    synchronized (_lock)
    {
      _properties.put(listenTo, discoveryProperties);
      if (_eventBus != null && _publishing.contains(listenTo))
      {
        _eventBus.publishAdd(listenTo, discoveryProperties);
      }
    }
  }

  @Override
  public void remove(String listenTo)
  {
    synchronized (_lock)
    {
      _properties.remove(listenTo);
      if (_eventBus != null && _publishing.contains(listenTo))
      {
        _eventBus.publishRemove(listenTo);
      }
    }

  }

  @Override
  public void startPublishing(String prop)
  {
    synchronized (_lock)
    {
      if (_publishing.add(prop))
      {
        _eventBus.publishInitialize(prop, _properties.get(prop));
      }
    }
  }

  @Override
  public void stopPublishing(String prop)
  {
    synchronized (_lock)
    {
      _publishing.remove(prop);
    }

  }

  @Override
  public void start(Callback<None> callback)
  {
    callback.onSuccess(None.none());
  }

  @Override
  public void shutdown(Callback<None> shutdown)
  {
    _shutdown = true;
    shutdown.onSuccess(None.none());
  }

  public boolean isShutdown()
  {
    return _shutdown;
  }

  @Override
  public String toString()
  {
    return "MockStore [_properties=" + _properties + "]";
  }
}
