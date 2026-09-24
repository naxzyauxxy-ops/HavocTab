package dev.havoc.havoctab.shared.event.impl;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import dev.havoc.havoctab.api.event.plugin.TabLoadEvent;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TabLoadEventImpl implements TabLoadEvent {

    @Getter private static final TabLoadEvent instance = new TabLoadEventImpl();
}
